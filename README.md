# RaufixTeam WebTools

**RaufixTeam WebTools** is a clean, open-source (**GPLv3+**), ad-free and tracker-free offline web archiver and website mirroring application for Android (`com.raufixteam.webtools`, API 22–36), built on the pinned upstream `xroche/httrack-android` baseline (`39f0f2de2fbce28f95b19bfdc8205510e3e07d26`) and the `httrack` 3.51.0 C engine.

---

## Key Features & Improvements

- **Universal Android Compatibility (API 22–36):** Supports Android 5.1.1 through Android 16 across `armeabi-v7a`, `arm64-v8a`, and `x86_64` with 16 KB ELF `LOAD` segment alignment and ARM64 BTI/PAC branch protection. Includes a clean `lseek64` shim (`app/src/main/jni/include/android_lfs_compat.h`) for 32-bit `< API 24` large-file compatibility without modifying the pinned `httrack` submodule.
- **Scoped Storage & SAF Export (No `MANAGE_EXTERNAL_STORAGE`):** Stores mirrors safely in app-scoped storage and provides on-demand export to any user-chosen folder (Downloads, Documents, SD card) via Android's Storage Access Framework (`SafMirrorExport`).
- **Default-Deny SSRF Protection & LAN Opt-In:** Enforced both in Java (`UrlSafetyPolicy`) and natively inside the C engine via `CHAIN_FUNCTION(..., check_link, rwt_check_link)` in `htslibjni.c`. Blocks `file://`/`javascript:`/`content:`/`data:` schemes, `localhost`, loopback (`127.0.0.0/8`, `::1`), RFC1918 private networks (`10/8`, `172.16/12`, `192.168/16`), link-local (`169.254/16`, `fe80::/10`), IPv6 ULA (`fc00::/7`), CGNAT (`100.64/10`), and cloud metadata endpoints (`169.254.169.254`, `metadata.google.internal`, `fd00:ec2::254`) by default. An explicit **"Allow local / LAN archiving"** toggle lets users archive local servers while keeping cloud metadata and unsafe schemes blocked.
- **Clean Output & Honest Identity:** Default HTML footer is empty (`""`) and explicitly emits `-%F ""` to suppress the engine's built-in promo comment unless customized. Default User-Agent identifies as `RaufixTeam-WebTools/1.0`.
- **Modern Material UI & Localization:** Updated Material Light/Dark themes (`values-v21`, `values-night`), original RaufixTeam WebTools adaptive and legacy launcher icons, and English + Turkish (`values-tr`) UI strings.
- **Ethical Crawling & Privacy:** Respects `robots.txt` by default (`FollowRobotsTxt="2"`), preserves rate-limiting controls, includes zero analytics/ads/telemetry, and logs zero credentials or cookies.

---

## Upstream Provenance & Pinned Submodules

- Upstream Android repository: `xroche/httrack-android` at commit `39f0f2de2fbce28f95b19bfdc8205510e3e07d26`
- Nested submodules:
  - `app/src/main/jni/httrack` -> `7a923f4047df1bec18e2ce1b4403c7038d055021` (`HTTRACK_VERSIONID "3.51.0"`)
  - `app/src/main/jni/httrack/src/coucal` -> `031eaf18ad0a8e1def4ec8dd2f7e077c5dac03b0`
  - `app/src/main/jni/coffeecatch` -> `b14cdb0e76a26f92bf6d097c88e6bc2ea9a747ef`

> **Important:** Always clone with recursive submodules (`git clone --recurse-submodules`). GitHub's auto-generated source archive omits nested submodule contents required for native builds.

---

## Building from Source

```bash
git clone --recurse-submodules https://github.com/RealRaufixBaba/RaufixTeam-WebTools.git
cd RaufixTeam-WebTools

# Stage OpenSSL 3.5.8 static libs (armeabi-v7a, arm64-v8a, x86_64) & GNU libiconv 1.17
OUT=/tmp/openssl-android ABIS="armeabi-v7a arm64-v8a x86_64" bash docker/build-openssl.sh
tools/ci/vendor-libiconv.sh
OPENSSL_ANDROID_ROOT=/tmp/openssl-android ABIS="armeabi-v7a arm64-v8a x86_64" tools/ci/fetch-openssl-statics.sh

# Build universal APKs and run unit tests + lint
./gradlew assembleDebug assembleRelease testDebugUnitTest lint
```

---

## License

Licensed under the **GNU General Public License v3.0 or later (`GPL-3.0-or-later`)** — see [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE) for full component copyright and license notices.
