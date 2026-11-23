package com.httrack.android;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Default-deny SSRF and unsafe-scheme policy for crawl and login targets.
 *
 * Blocks localhost, loopback, RFC1918 private networks, link-local, IPv6 ULA,
 * CGNAT, multicast/unspecified addresses, and cloud metadata endpoints by
 * default. Explicit LAN archiving opt-in permits local/RFC1918/ULA targets
 * while still blocking cloud metadata endpoints and unsafe schemes.
 */
final class UrlSafetyPolicy {
  enum Verdict {
    ALLOWED,
    BLOCKED_UNSAFE_SCHEME,
    BLOCKED_METADATA,
    BLOCKED_LOCAL_OR_PRIVATE,
    INVALID_URL
  }

  private UrlSafetyPolicy() {
  }

  static Verdict checkUrl(final String rawUrl, final boolean allowLan) {
    if (rawUrl == null) {
      return Verdict.INVALID_URL;
    }
    final String trimmed = rawUrl.trim();
    if (trimmed.isEmpty()) {
      return Verdict.INVALID_URL;
    }

    // Reject explicit dangerous schemes before normalizing bare host inputs.
    final int colon = trimmed.indexOf(':');
    final int slash = trimmed.indexOf('/');
    final String candidate;
    if (colon > 0 && (slash < 0 || colon < slash)) {
      final String scheme = trimmed.substring(0, colon).toLowerCase(Locale.ROOT);
      if (!"http".equals(scheme) && !"https".equals(scheme) && !"ftp".equals(scheme)) {
        return Verdict.BLOCKED_UNSAFE_SCHEME;
      }
      candidate = trimmed;
    } else {
      candidate = "http://" + trimmed;
    }

    final URI uri;
    try {
      uri = URI.create(candidate);
    } catch (final IllegalArgumentException e) {
      return Verdict.INVALID_URL;
    }
    final String host = extractHost(uri);
    if (host == null || host.isEmpty()) {
      return Verdict.INVALID_URL;
    }
    return checkHostName(host, allowLan);
  }

  static Verdict checkUrlWithDns(final String rawUrl, final boolean allowLan) {
    final Verdict syntax = checkUrl(rawUrl, allowLan);
    if (syntax != Verdict.ALLOWED) {
      return syntax;
    }
    final String trimmed = rawUrl.trim();
    final int colon = trimmed.indexOf(':');
    final int slash = trimmed.indexOf('/');
    final String candidate =
        (colon > 0 && (slash < 0 || colon < slash)) ? trimmed : "http://" + trimmed;
    final String host = extractHost(URI.create(candidate));
    if (host == null) {
      return Verdict.INVALID_URL;
    }
    try {
      final InetAddress[] addrs = InetAddress.getAllByName(host);
      for (final InetAddress addr : addrs) {
        final Verdict v = checkHostAddress(addr, allowLan);
        if (v != Verdict.ALLOWED) {
          return v;
        }
      }
    } catch (final UnknownHostException ignored) {
      // Let the engine handle unresolved public hostnames normally.
    }
    return Verdict.ALLOWED;
  }

  static Verdict checkHostName(final String rawHost, final boolean allowLan) {
    if (rawHost == null || rawHost.trim().isEmpty()) {
      return Verdict.INVALID_URL;
    }
    String host = rawHost.trim().toLowerCase(Locale.ROOT);
    if (host.startsWith("[") && host.endsWith("]")) {
      host = host.substring(1, host.length() - 1);
    }
    final int zoneIdx = host.indexOf('%');
    if (zoneIdx >= 0) {
      host = host.substring(0, zoneIdx);
    }
    while (host.endsWith(".")) {
      host = host.substring(0, host.length() - 1);
    }
    if (host.isEmpty()) {
      return Verdict.INVALID_URL;
    }

    // Cloud metadata hostnames are always blocked, even when LAN archiving is enabled.
    if ("metadata.google.internal".equals(host)
        || "metadata".equals(host)
        || "instance-data".equals(host)
        || host.endsWith(".metadata.google.internal")) {
      return Verdict.BLOCKED_METADATA;
    }

    // Check literal IP forms (including integer/hex/octal IPv4 obfuscation).
    final byte[] literalIpv4 = parseIpv4Literal(host);
    if (literalIpv4 != null) {
      return classifyIpv4Bytes(literalIpv4, allowLan);
    }
    if (host.indexOf(':') >= 0) {
      try {
        final InetAddress parsed = InetAddress.getByName(host);
        return checkHostAddress(parsed, allowLan);
      } catch (final UnknownHostException e) {
        return Verdict.INVALID_URL;
      }
    }

    if ("localhost".equals(host)
        || host.endsWith(".localhost")
        || host.endsWith(".local")
        || host.endsWith(".internal")) {
      return allowLan ? Verdict.ALLOWED : Verdict.BLOCKED_LOCAL_OR_PRIVATE;
    }

    return Verdict.ALLOWED;
  }

  static Verdict checkHostAddress(final InetAddress addr, final boolean allowLan) {
    if (addr == null) {
      return Verdict.INVALID_URL;
    }
    if (addr instanceof Inet4Address) {
      return classifyIpv4Bytes(addr.getAddress(), allowLan);
    }
    if (addr instanceof Inet6Address) {
      final byte[] b = addr.getAddress();
      if (b.length != 16) {
        return Verdict.INVALID_URL;
      }
      // IPv4-mapped IPv6 ::ffff:a.b.c.d
      if (isIpv4MappedIpv6(b)) {
        final byte[] v4 = new byte[] { b[12], b[13], b[14], b[15] };
        return classifyIpv4Bytes(v4, allowLan);
      }
      // AWS IPv6 IMDS fd00:ec2::254 — always blocked
      if ((b[0] & 0xff) == 0xfd && (b[1] & 0xff) == 0x00
          && (b[2] & 0xff) == 0x0e && (b[3] & 0xff) == 0xc2
          && b[4] == 0 && b[5] == 0 && b[6] == 0 && b[7] == 0
          && b[8] == 0 && b[9] == 0 && b[10] == 0 && b[11] == 0
          && b[12] == 0 && b[13] == 0 && (b[14] & 0xff) == 0x02 && (b[15] & 0xff) == 0x54) {
        return Verdict.BLOCKED_METADATA;
      }
      final boolean isUnspecified = addr.isAnyLocalAddress();
      final boolean isLoopback = addr.isLoopbackAddress();
      final boolean isLinkLocal = addr.isLinkLocalAddress();
      final boolean isSiteLocal = addr.isSiteLocalAddress();
      final boolean isMulticast = addr.isMulticastAddress();
      final boolean isUla = (b[0] & 0xfe) == 0xfc; // fc00::/7
      if (isLinkLocal || isMulticast || isUnspecified) {
        return allowLan && isLinkLocal ? Verdict.ALLOWED : Verdict.BLOCKED_LOCAL_OR_PRIVATE;
      }
      if (isLoopback || isSiteLocal || isUla) {
        return allowLan ? Verdict.ALLOWED : Verdict.BLOCKED_LOCAL_OR_PRIVATE;
      }
      return Verdict.ALLOWED;
    }
    return Verdict.ALLOWED;
  }

  private static Verdict classifyIpv4Bytes(final byte[] b, final boolean allowLan) {
    final int o0 = b[0] & 0xff;
    final int o1 = b[1] & 0xff;
    final int o2 = b[2] & 0xff;
    final int o3 = b[3] & 0xff;

    // Cloud metadata IPs (AWS/GCP/Azure 169.254.169.254, ECS 169.254.170.2, Alibaba 100.100.100.200)
    if ((o0 == 169 && o1 == 254 && o2 == 169 && o3 == 254)
        || (o0 == 169 && o1 == 254 && o2 == 170 && o3 == 2)
        || (o0 == 100 && o1 == 100 && o2 == 100 && o3 == 200)) {
      return Verdict.BLOCKED_METADATA;
    }
    // 0.0.0.0/8 ("this" network) & 224.0.0.0/4+ (multicast/reserved) — always block
    if (o0 == 0 || o0 >= 224) {
      return Verdict.BLOCKED_LOCAL_OR_PRIVATE;
    }
    // 127.0.0.0/8 loopback, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 169.254.0.0/16, 100.64.0.0/10
    final boolean isPrivateOrLocal = o0 == 127
        || o0 == 10
        || (o0 == 172 && o1 >= 16 && o1 <= 31)
        || (o0 == 192 && o1 == 168)
        || (o0 == 169 && o1 == 254)
        || (o0 == 100 && o1 >= 64 && o1 <= 127);
    if (isPrivateOrLocal) {
      return allowLan ? Verdict.ALLOWED : Verdict.BLOCKED_LOCAL_OR_PRIVATE;
    }
    return Verdict.ALLOWED;
  }

  private static boolean isIpv4MappedIpv6(final byte[] b) {
    for (int i = 0; i < 10; i++) {
      if (b[i] != 0) {
        return false;
      }
    }
    return (b[10] & 0xff) == 0xff && (b[11] & 0xff) == 0xff;
  }

  private static String extractHost(final URI uri) {
    if (uri == null) {
      return null;
    }
    String host = uri.getHost();
    if (host == null && uri.getRawAuthority() != null) {
      String auth = uri.getRawAuthority();
      final int at = auth.lastIndexOf('@');
      if (at >= 0) {
        auth = auth.substring(at + 1);
      }
      if (auth.startsWith("[")) {
        final int close = auth.indexOf(']');
        if (close > 0) {
          host = auth.substring(0, close + 1);
        }
      } else {
        final int port = auth.indexOf(':');
        host = port >= 0 ? auth.substring(0, port) : auth;
      }
    }
    return host;
  }

  /** Parses standard dotted-quad as well as single-integer / hex / octal IPv4 literals. */
  private static byte[] parseIpv4Literal(final String host) {
    final String[] parts = host.split("\\.", -1);
    if (parts.length < 1 || parts.length > 4) {
      return null;
    }
    final long[] vals = new long[parts.length];
    for (int i = 0; i < parts.length; i++) {
      final Long v = parseIpv4Part(parts[i]);
      if (v == null || v < 0) {
        return null;
      }
      vals[i] = v;
    }
    final long addr;
    if (parts.length == 1) {
      if (vals[0] > 0xffffffffL) {
        return null;
      }
      addr = vals[0];
    } else if (parts.length == 2) {
      if (vals[0] > 0xff || vals[1] > 0xffffffL) {
        return null;
      }
      addr = (vals[0] << 24) | vals[1];
    } else if (parts.length == 3) {
      if (vals[0] > 0xff || vals[1] > 0xff || vals[2] > 0xffffL) {
        return null;
      }
      addr = (vals[0] << 24) | (vals[1] << 16) | vals[2];
    } else {
      for (int i = 0; i < 4; i++) {
        if (vals[i] > 0xff) {
          return null;
        }
      }
      addr = (vals[0] << 24) | (vals[1] << 16) | (vals[2] << 8) | vals[3];
    }
    return new byte[] {
        (byte) ((addr >> 24) & 0xff),
        (byte) ((addr >> 16) & 0xff),
        (byte) ((addr >> 8) & 0xff),
        (byte) (addr & 0xff)
    };
  }

  private static Long parseIpv4Part(final String part) {
    if (part.isEmpty()) {
      return null;
    }
    try {
      if (part.startsWith("0x") || part.startsWith("0X")) {
        if (part.length() <= 2) {
          return null;
        }
        return Long.parseLong(part.substring(2), 16);
      }
      if (part.length() > 1 && part.startsWith("0")) {
        return Long.parseLong(part, 8);
      }
      return Long.parseLong(part, 10);
    } catch (final NumberFormatException e) {
      return null;
    }
  }
}
