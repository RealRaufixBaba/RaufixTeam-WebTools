package com.httrack.android;

import static org.junit.Assert.assertEquals;

import com.httrack.android.UrlSafetyPolicy.Verdict;
import org.junit.Test;

public class UrlSafetyPolicyTest {

  @Test
  public void publicUrlsAreAllowed() {
    assertEquals(Verdict.ALLOWED, UrlSafetyPolicy.checkUrl("https://example.com/", false));
    assertEquals(Verdict.ALLOWED, UrlSafetyPolicy.checkUrl("http://example.org/docs", false));
    assertEquals(Verdict.ALLOWED, UrlSafetyPolicy.checkUrl("example.com", false));
    assertEquals(Verdict.ALLOWED, UrlSafetyPolicy.checkUrl("https://[2001:db8::1]/", false));
  }

  @Test
  public void unsafeSchemesAreBlockedEvenWithLanOptIn() {
    assertEquals(Verdict.BLOCKED_UNSAFE_SCHEME,
        UrlSafetyPolicy.checkUrl("file:///etc/passwd", false));
    assertEquals(Verdict.BLOCKED_UNSAFE_SCHEME,
        UrlSafetyPolicy.checkUrl("file:///etc/passwd", true));
    assertEquals(Verdict.BLOCKED_UNSAFE_SCHEME,
        UrlSafetyPolicy.checkUrl("javascript:alert(1)", true));
    assertEquals(Verdict.BLOCKED_UNSAFE_SCHEME,
        UrlSafetyPolicy.checkUrl("content://media/external/file", true));
    assertEquals(Verdict.BLOCKED_UNSAFE_SCHEME,
        UrlSafetyPolicy.checkUrl("data:text/html,hi", true));
  }

  @Test
  public void cloudMetadataAlwaysBlockedEvenWithLanOptIn() {
    assertEquals(Verdict.BLOCKED_METADATA,
        UrlSafetyPolicy.checkUrl("http://169.254.169.254/latest/meta-data/", false));
    assertEquals(Verdict.BLOCKED_METADATA,
        UrlSafetyPolicy.checkUrl("http://169.254.169.254/latest/meta-data/", true));
    assertEquals(Verdict.BLOCKED_METADATA,
        UrlSafetyPolicy.checkUrl("http://metadata.google.internal/computeMetadata/v1/", true));
    assertEquals(Verdict.BLOCKED_METADATA,
        UrlSafetyPolicy.checkUrl("http://[fd00:ec2::254]/latest/meta-data/", true));
    assertEquals(Verdict.BLOCKED_METADATA,
        UrlSafetyPolicy.checkUrl("http://100.100.100.200/latest/meta-data/", true));
  }

  @Test
  public void localhostLoopbackAndRfc1918BlockedByDefault() {
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://localhost:8080/", false));
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://127.0.0.1/", false));
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://10.0.0.1/", false));
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://172.16.5.4/", false));
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://192.168.1.1/", false));
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://100.64.0.1/", false));
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://0.0.0.0/", false));
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://[::1]/", false));
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://[fc00::1]/", false));
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://[fe80::1]/", false));
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://[::ffff:127.0.0.1]/", false));
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://[::ffff:192.168.1.10]/", false));
  }

  @Test
  public void obfuscatedIpv4LiteralsAreCaught() {
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://2130706433/", false)); // 127.0.0.1
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://0x7f000001/", false));
    assertEquals(Verdict.BLOCKED_LOCAL_OR_PRIVATE,
        UrlSafetyPolicy.checkUrl("http://0177.0.0.1/", false));
    assertEquals(Verdict.BLOCKED_METADATA,
        UrlSafetyPolicy.checkUrl("http://2852039166/", false)); // 169.254.169.254
  }

  @Test
  public void explicitLanOptInAllowsLocalAndRfc1918Targets() {
    assertEquals(Verdict.ALLOWED,
        UrlSafetyPolicy.checkUrl("http://localhost:8080/", true));
    assertEquals(Verdict.ALLOWED,
        UrlSafetyPolicy.checkUrl("http://127.0.0.1:3000/", true));
    assertEquals(Verdict.ALLOWED,
        UrlSafetyPolicy.checkUrl("http://192.168.1.50/", true));
    assertEquals(Verdict.ALLOWED,
        UrlSafetyPolicy.checkUrl("http://[fc00::10]/", true));
  }
}
