# Phase 01 Audit Report — Repository Audit, Submodules, and License-Compliant Fork (`RaufixTeam WebTools`)

**Phase:** `01 / 15` (`RWT-1-01`: Repository audit, submodules, and license-compliant fork)  
**Audit Date:** 2026-11-23  
**Current Branch:** `arena/59703b72-raufixteam-webtools` (branched from `main` `d8b543813be7ef4bf22fb170144006666ba10510`)  
**Target Brand / Future `applicationId`:** **RaufixTeam WebTools** / `com.raufixteam.webtools` (not yet changed in Phase 01; upstream baseline `com.httrack.android` preserved during Phase 01 audit)

---

## 1. Upstream Commit & Recursive Submodule Manifest

Upstream `master` is mutable, so this fork pins and merges the exact reviewed upstream commit rather than tracking a floating branch head.

| Component | Repository / Source URL | Path in Repo | Pinned Commit / Version | Tag / Describe |
| :--- | :--- | :--- | :--- | :--- |
| **httrack-android** (App + JNI) | `https://github.com/xroche/httrack-android.git` | `/` | `39f0f2de2fbce28f95b19bfdc8205510e3e07d26` | `HEAD` / `master` (2026-11-08 snapshot) |
| **coffeecatch** (JNI signal catcher) | `https://github.com/xroche/coffeecatch.git` | `app/src/main/jni/coffeecatch` | `b14cdb0e76a26f92bf6d097c88e6bc2ea9a747ef` | `heads/master` |
| **httrack** (C mirroring engine) | `https://github.com/xroche/httrack.git` | `app/src/main/jni/httrack` | `7a923f4047df1bec18e2ce1b4403c7038d055021` | `3.51.0-112-g7a923f40` (`HTTRACK_VERSION "3.51-0"`, `HTTRACK_VERSIONID "3.51.0"`) |
| **coucal** (Cuckoo hash table, nested submodule) | `https://github.com/xroche/coucal.git` | `app/src/main/jni/httrack/src/coucal` | `031eaf18ad0a8e1def4ec8dd2f7e077c5dac03b0` | `031eaf1` |

### 1.1 Missing Submodule Content in `master.zip` Archive

Inspection of `https://github.com/xroche/httrack-android/archive/refs/heads/master.zip` (362 archive entries, 2,044,515 bytes uncompressed) confirms that GitHub's source ZIP **cannot** build the native libraries:

- `httrack-android-master/app/src/main/jni/coffeecatch/` — **empty directory (`0` bytes)**; all C sources and `LICENSE` are missing.
- `httrack-android-master/app/src/main/jni/httrack/` — **empty directory (`0` bytes)**; the entire HTTrack C engine, `COPYING`, `html/`, `templates/`, `lang-symbols.tsv`, and `src/minizip/` are missing.
- `httrack-android-master/app/src/main/jni/httrack/src/coucal/` — **completely absent** (nested submodule inside `httrack`).
- Build-time external native inputs (`app/src/main/jni/libiconv-1.17/` via `tools/ci/vendor-libiconv.sh` and `app/src/main/prebuild/<abi>/lib{ssl,crypto}.a` via `docker/build-openssl.sh` + `tools/ci/fetch-openssl-statics.sh`) are intentionally gitignored and absent from both Git and `master.zip`.

### 1.2 Reproducible Full-Source Checkout Commands

To reproduce a complete, pinned upstream checkout with recursive submodules:

```bash
git clone --recurse-submodules https://github.com/xroche/httrack-android.git
cd httrack-android
git checkout 39f0f2de2fbce28f95b19bfdc8205510e3e07d26
git submodule update --init --recursive
git submodule status --recursive
```

To clone this fork with all submodules initialized:

```bash
git clone --recurse-submodules https://github.com/RealRaufixBaba/RaufixTeam-WebTools.git
cd RaufixTeam-WebTools
git submodule status --recursive
```

---

## 2. Verified Baseline Inventory (Re-verified Against Checkout)

Every baseline expectation was re-checked directly against the merged checkout files:

| Property | Expected Baseline | Verified in Checkout | Source File(s) |
| :--- | :--- | :--- | :--- |
| `applicationId` / `namespace` | `com.httrack.android` | `com.httrack.android` / `com.httrack.android` | `app/build.gradle` |
| Android Gradle Plugin (AGP) | `8.6.0` | `com.android.application` version `8.6.0` | `build.gradle` |
| Gradle Wrapper | — | `8.7` (`gradle-8.7-bin.zip`, sha256 `544c35d6bd849ae8a5ed0bcea39ba677dc40f49df7d1835561582da2009b961d`) | `gradle/wrapper/gradle-wrapper.properties` |
| `compileSdk` / `targetSdk` | `36` / `36` | `36` / `36`, `buildToolsVersion "36.0.0"` | `app/build.gradle` |
| `minSdk` | `24` | `24` (`APP_PLATFORM := android-24` in `Application.mk`) | `app/build.gradle`, `app/src/main/jni/Application.mk` |
| Java Compatibility | Java 17 | `JavaVersion.VERSION_17` (source & target) | `app/build.gradle` |
| NDK Version | `27.3` | `ndkVersion "27.3.13750724"` (`NDK_VERSION: r27d` in CI) | `app/build.gradle`, `.github/workflows/android.yml` |
| ABI Filters | `arm64-v8a`, `x86_64` | `abiFilters "arm64-v8a", "x86_64"` & `APP_ABI := arm64-v8a x86_64` | `app/build.gradle`, `app/src/main/jni/Application.mk` |
| Version Code / Name | — | `versionCode 110`, `versionName "3.51.0.110"` | `app/build.gradle` |
| `gradle.properties` | — | `android.useAndroidX=true`, `android.nonFinalResIds=false`, `-Xmx2048m`, `org.gradle.parallel=true` | `gradle.properties` |
| Default HTML Footer | `<!-- Mirrored from ... by HTTrack Website Copier ... -->` | `"<!-- Mirrored from {url} by HTTrack Website Copier/3.x [XR&CO], {date} -->"` | `OptionsMapper.java:273-274`, `strings.xml:129`, `FooterDefaultTest.java` |
| Native Modules (`Android.mk`) | — | `libiconv` (shared, 1.17), `libcrypto.a` + `libssl.a` (OpenSSL 3.5.8 static), `libhttrack` (shared), `htslibjni` (shared, with `coffeecatch`) | `app/src/main/jni/Android.mk`, `docker/build-openssl.sh`, `tools/ci/vendor-libiconv.sh` |
| Stale Doc Note Discrepancy | — | `app/src/main/jni/libiconv.txt` still mentions `1.15`, whereas `Android.mk` and `vendor-libiconv.sh` build **`libiconv-1.17`** | `app/src/main/jni/libiconv.txt`, `Android.mk` |
| Java Dependencies | — | `kotlin-bom:1.8.22`, `androidx.core:core:1.13.1`, `androidx.fragment:fragment:1.8.2`, `androidx.documentfile:documentfile:1.0.1`, `androidx.browser:browser:1.8.0`, `org.nanohttpd:nanohttpd:2.3.1`, `junit:junit:4.13.2` | `app/build.gradle` |
| Unit Tests | — | 64 JUnit4 test classes in `app/src/test/java/com/httrack/android/` + 3 `android.util` test stubs | `app/src/test/java/` |
| CI Workflows | — | `android.yml` (`native`, `assemble`, `play-tools`), `codeql.yml`, `docker-image.yml`, `dependabot-automerge.yml`, `play-{halt,promote,publish,reviews,vitals}.yml` | `.github/workflows/` |

---

## 3. License & Distribution Obligations Audit

### 3.1 Root `LICENSE` Reconciliation (Critical Phase 01 Fix)

Before this audit, `/home/user/RaufixTeam-WebTools/LICENSE` was an **MIT License** (`Copyright (c) 2026 RaufixBaba`). Because `httrack` and `httrack-android` are licensed under **GPL-3.0-or-later**, distributing the combined/derived Android app under MIT violates GPLv3 Section 5(c).  
In Phase 01, root `LICENSE` was replaced with the verbatim **GNU General Public License v3.0** text (`app/src/main/jni/httrack/COPYING`), and `NOTICE` was added to preserve all upstream and third-party copyright notices.

### 3.2 Component-by-Component License Table (Verified from `COPYING`/`LICENSE`/Headers)

| Component | Path / Artifact | Verified License | Copyright Holder(s) | Evidence Inspected | Distribution Obligations |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **httrack-android** (Java UI, Service, MirrorServer, JNI glue) | `app/src/main/java/`, `app/src/main/jni/htslibjni.*`, `app/src/main/res/raw/legal.txt` | **GPL-3.0-or-later** | Xavier Roche and other contributors | `legal.txt`, file headers in `HTTrackActivity.java`, `htslibjni.c`, `gpl-3.0-standalone.html` | Keep GPLv3+ license, preserve copyright headers, mark modified versions clearly, provide corresponding source code on distribution. |
| **httrack** (Core C engine, docs, templates) | `app/src/main/jni/httrack/` | **GPL-3.0-or-later** | 1998–2026 Xavier Roche and other contributors | `httrack/COPYING`, `httrack/license.txt`, `httrack/debian/copyright` | Strong copyleft for combined binaries (`libhttrack.so`, APK/AAB); preserve `COPYING`, `license.txt`, `greetings.txt`, and source access. |
| **coffeecatch** (JNI signal/fault catcher) | `app/src/main/jni/coffeecatch/` | **BSD-2-Clause** | 2013 Xavier Roche (`http://www.httrack.com/`) | `coffeecatch/LICENSE`, `coffeecatch/coffeecatch.h` | Permissive, GPLv3-compatible. Retain copyright notice, two conditions, and warranty disclaimer in source and binary distributions/docs. |
| **coucal** (Cuckoo hash table) | `app/src/main/jni/httrack/src/coucal/` | **BSD-3-Clause** | 2013–2014 Xavier Roche | `coucal/LICENSE`, `coucal/coucal.h`, `httrack/debian/copyright` | Permissive, GPLv3-compatible. Retain copyright/conditions/disclaimer; do not use author/contributor names for endorsement. |
| **murmurhash3** (inside `coucal`) | `app/src/main/jni/httrack/src/coucal/murmurhash3.h` | **Public Domain** | Austin Appleby | `httrack/debian/copyright`, `murmurhash3.h` | No restrictions; retain attribution note. |
| **minizip** (inside `httrack`) | `app/src/main/jni/httrack/src/minizip/*` | **Zlib License** | 1998–2010 Gilles Vollant, Even Rouault, Mathias Svensson, Info-ZIP | `minizip/ioapi.h`, `httrack/debian/copyright` | Permissive, GPLv3-compatible. Do not misrepresent origin; mark altered source versions; retain notice. |
| **md5.c** (inside `httrack`) | `app/src/main/jni/httrack/src/md5.c` | **Public Domain** | Colin Plumb (1993) | `md5.c` header, `httrack/debian/copyright` | No restrictions. |
| **htspsl_data.c** (Public Suffix List) | `app/src/main/jni/httrack/src/htspsl_data.c` | **MPL-2.0** | Public Suffix List contributors | `htspsl_data.c` header, `httrack/debian/copyright` | File-level weak copyleft (compatible with GPLv3 under MPL-2.0 §3.3). Keep MPL-2.0 notice and source availability for any edits to `htspsl_data.c`. |
| **League Spartan font** | `app/src/main/jni/httrack/html/fonts/*` | **SIL OFL-1.1** | 2020 The League Spartan Project Authors | `httrack/debian/copyright` | May bundle in `resources.zip`/APK; must retain OFL-1.1 copyright/notice and not sell the font standalone. |
| **GNU libiconv 1.17** (vendored at build time) | `tools/ci/vendor-libiconv.sh` -> `libiconv.so` | **LGPL-2.1-or-later** | Free Software Foundation | `vendor-libiconv.sh`, `Android.mk` | Linked as shared `libiconv.so`. Preserve LGPL license/copyright notice and provide corresponding source/build script (`vendor-libiconv.sh`). |
| **OpenSSL 3.5.8** (built at build time) | `docker/build-openssl.sh` -> static `libssl.a`, `libcrypto.a` | **Apache-2.0** | OpenSSL Project | `docker/Dockerfile`, `docker/build-openssl.sh` | GPLv3-compatible (unlike OpenSSL 1.0.2k). Retain Apache-2.0 license/notices in binary distribution. |
| **NanoHTTPD 2.3.1** | `org.nanohttpd:nanohttpd:2.3.1` (`app/build.gradle`) | **BSD-3-Clause** | NanoHTTPD contributors | `app/build.gradle` (verified Maven POM artifact) | Permissive, GPLv3-compatible. Retain BSD-3-Clause notice in third-party notices. |
| **AndroidX / Kotlin BOM** | `androidx.*`, `kotlin-bom:1.8.22` | **Apache-2.0** | Google / AOSP / JetBrains | `app/build.gradle` | GPLv3-compatible. Retain Apache-2.0 notices. |

---

## 4. Architecture Sketch

```text
+-----------------------------------------------------------------------------------+
|                               Android App Process                                 |
|                                                                                   |
|  HTTrackApplication (tracks liveActivities, registers API 35+ EdgeToEdge insets)  |
|   ├── HTTrackActivity (singleTop wizard: Welcome -> Name -> Setup -> Progress ->  |
|   │    │                 Finished; StoragePaths + SAF import + permissions)       |
|   │    ├── RunnerFragment (retained Fragment: owns CrawlRun on API <= 33 or fallback)
|   │    ├── OptionsActivity (tabbed @Fields annotation editor -> OptionsMapper)     |
|   │    ├── FileChooserActivity (DirScanner / File.listFiles() directory picker)   |
|   │    ├── CleanupActivity (ListActivity: project delete / select)                |
|   │    └── BrowserLoginActivity (WebView HTTPS-only login -> cookies -> bundle)   |
|   │                                                                               |
|   ├── MirrorJobService (JobService, API 34+ user-initiated data transfer job)     |
|   │    └── drives CrawlRun + foreground JobInfo notification + retry attempt lock |
|   │                                                                               |
|   ├── MirrorSession (in-memory handover bus: progress, stats, final verdict)      |
|   └── MirrorServer (NanoHTTPD on 127.0.0.1:{8080,3128,8081,3129,0})               |
|        └── serves scoped-storage mirrors & extracted help docs to Chrome/CustomTab|
+-----------------------------------------+-----------------------------------------+
                                          | JNI (com.httrack.android.jni.HTTrackLib)
                                          v
+-----------------------------------------------------------------------------------+
|                        Native Layer (arm64-v8a, x86_64)                           |
|                                                                                   |
|  libhtslibjni.so (htslibjni.c + coffeecatch.c + coffeejni.c)                      |
|   ├── Hardcoded JNI signatures: Java_com_httrack_android_jni_HTTrackLib_*         |
|   ├── Fault containment: COFFEE_TRY_JNI_RECOVER + workerThreadRunner + engineFaulted
|   └── Emergency crash log: <projectRoot>/error.txt & stdio -> <projectRoot>/log.txt
|                                                                                   |
|  libhttrack.so (53 engine C files + coucal.c + minizip/*.c)                       |
|   ├── Static: libssl.a + libcrypto.a (OpenSSL 3.5.8, TLS_SECURITY_LEVEL=1)        |
|   └── Shared: libiconv.so (GNU libiconv 1.17, -DLIBICONV_PLUG), libz, libdl       |
+-----------------------------------------------------------------------------------+
```

---

## 5. Behavior Map

1. **Application Startup & Edge-to-Edge (`HTTrackApplication`, `EdgeToEdge`)**:
   - Tracks `liveActivities` count (used by `NativeFaultPolicy` so a background `MirrorJobService` process without an Activity can safely `exit` on a caught native fault).
   - On API 35+, `EdgeToEdge.fitSystemWindows` disables decor fitting, applies `systemBars | displayCutout | ime` insets to decor, pads content below the Holo ActionBar height, and adjusts light/dark status & navigation bar icons.
2. **Storage & Legacy Import (`HTTrackActivity`, `StoragePaths`, `LegacyMirrorImport`, `FileChooserActivity`)**:
   - Public storage root `/sdcard/HTTrack/Websites` is used when all-files access is held (`Environment.isExternalStorageManager()` on API 30+, `WRITE_EXTERNAL_STORAGE` on API <= 29); otherwise falls back to private `getExternalFilesDir(null)/Websites` or `getFilesDir()/Websites`.
   - `LegacyMirrorImport` launches `Intent.ACTION_OPEN_DOCUMENT_TREE` once to copy old `Download/HTTrack/Websites` or user-picked public folders into app storage (read-only on source, idempotent, capped at `MAX_DEPTH = 100`).
   - `FileChooserActivity` still browses filesystem paths directly via `File.listFiles()` (`DirScanner`), rather than SAF.
   - `getFilesDir()/logins/` holds per-project login cookie files and `--bundle-dir` `.hts` bundles (private to app, never placed inside public mirror folders).
3. **Options & CLI Translation (`OptionsActivity`, `OptionsMapper`, `CrawlArgv`, `ResumeArgv`)**:
   - `OptionsActivity` binds widget IDs via `@Fields` annotations (`android.nonFinalResIds=false` required).
   - `OptionsMapper` serializes/deserializes `<target>/hts-cache/winprofile.ini` and generates `httrack` CLI `argv` tokens (including `-%F` footer defaulting to `"<!-- Mirrored from {url} by HTTrack Website Copier/3.x [XR&CO], {date} -->"` and `FollowRobotsTxt` defaulting to `"2"`).
4. **Crawl Execution & Lifecycle Handover (`HTTrackActivity`, `RunnerFragment`, `MirrorJobService`, `CrawlRun`, `MirrorSession`)**:
   - `CrawlOwnerPolicy.ownsInJob(SDK_INT)` delegates crawls to `MirrorJobService` on **API 34+**, using `setUserInitiated(true)`, `PRIORITY_MAX`, `NETWORK_TYPE_ANY`, and `JobService.setNotification(...)`. If `JobScheduler.schedule` fails or `SDK_INT < 34`, retained `RunnerFragment` drives `CrawlRun` inside the Activity.
   - `CrawlRun` acquires a `FileLock` on `winprofile.ini`, runs `HTTrackLib.main(argv)` on thread `"httrack-crawl"`, writes `hts-cache/interrupted.lock` during active mirrors, and publishes stats/verdict through `MirrorSession`.
   - `Back` during a crawl uses `BackgroundPolicy` (`moveTaskToBack`, fallback to launcher home intent) rather than `finish()`, to avoid tearing down `RunnerFragment`.
5. **Mirror Viewing & Docs (`MirrorServer`, `Loopback`, `Help`, `buildResourcesBundle`)**:
   - `buildResourcesBundle` packs `httrack/html`, `httrack/templates`, `license/gpl-3.0-standalone.html`, `greetings.txt`, `history.txt`, `license.txt` into `R.raw.resources` (`resources.zip`).
   - Because Android 11+ blocks `file://` URIs in external browsers, `MirrorServer` (`NanoHTTPD` bound strictly to `127.0.0.1` on ports `{8080, 3128, 8081, 3129, 0}`) serves mirrors and docs over `http://127.0.0.1:<port>/...`, validates `Host` against DNS rebinding (`isOwnAuthority`), canonicalizes paths (`resolveWithinRoot`), and shuts down after 30 minutes idle.
6. **Authenticated Mirroring (`BrowserLoginActivity`, `BrowserLoginPolicy`, `BrowserLoginCookies`)**:
   - WebView allows JS & DOM storage for login forms, disables file/content access (`setAllowFileAccess(false)`, `setAllowContentAccess(false)`), disables `saveFormData`, uses **no** `addJavascriptInterface`, restricts navigation to `https://` only, and hands captured cookies to native `HTTrackLib.writeLoginBundle(...)` which writes a scoped `.hts` login bundle.
7. **Crash Containment (`coffeecatch`, `htslibjni.c`, `NativeFaultPolicy`)**:
   - `COFFEE_TRY_JNI_RECOVER` catches fatal signals (`SIGSEGV`, `SIGABRT`, etc.) via `sigsetjmp`/`siglongjmp`, sets `engineFaulted = 1` permanently for the process lifetime, disarms the 30s `SIGALRM` watchdog after throwing Java `Error`, and `workerThreadRunner` wraps engine-spawned pthreads.

---

## 6. Untouched Engine Components List

In accordance with Phase 01 rules ("Do not rewrite the C/C++ HTTrack engine without evidence"), the following native components **must remain unmodified** unless a specific Android ABI, security, or API-level requirement provides concrete evidence for a minimal, isolated patch:

1. **Core HTTrack Engine (`app/src/main/jni/httrack/src/` — 53 source files compiled in `Android.mk`):**
   - `htscore.c`, `htsparse.c`, `htsback.c`, `htscache.c`, `htscatchurl.c`, `htsfilters.c`, `htsftp.c`, `htshash.c`, `htshelp.c`, `htslib.c`, `htscoremain.c`, `htsname.c`, `htsrobots.c`, `htstools.c`, `htswizard.c`, `htsalias.c`, `htsthread.c`, `htsindex.c`, `htsbauth.c`, `htsbundle.c`, `htsbase.c`, `htsmd5.c`, `htszlib.c`, `htswrap.c`, `htsconcat.c`, `htsmodules.c`, `htscharset.c`, `punycode.c`, `htsencoding.c`, `md5.c`, `htssniff.c`, `htscodec.c`, `htsproxy.c`, `htsdate.c`, `htsurlport.c`, `htswarc.c`, `htsescape.c`, `htsnet.c`, `htsrandom.c`, `htssitemap.c`, `htssinglefile.c`, `htschanges.c`, `htscmdline.c`, `htslines.c`, `htsfetch.c`, `htspsl.c`, `htspsl_data.c`, `htscrashtest.c` (plus corresponding `.h` headers).
2. **Engine Self-Tests (excluded from APK via `-DHTS_NO_SELFTEST`, validated by `EngineSelftestsExcludedTest.java`):**
   - All `*_selftest.c` / `htsselftest*.c` files in `app/src/main/jni/httrack/src/`.
3. **Coucal Hash Table (`app/src/main/jni/httrack/src/coucal/`):**
   - `coucal.c`, `coucal.h`, `murmurhash3.h`.
4. **Minizip (`app/src/main/jni/httrack/src/minizip/`):**
   - `ioapi.c`, `mztools.c`, `unzip.c`, `zip.c` *(exception: only `ioapi.c` / large-file `fseeko`/`ftello` gating may need minimal inspection if `minSdk` is ever lowered below 24)*.
5. **CoffeeCatch (`app/src/main/jni/coffeecatch/`):**
   - `coffeecatch.c`, `coffeecatch.h`, `coffeejni.c`, `coffeejni.h` *(already includes upstream `x86_64` `ucontext` fix `#46`)*.

*(Note: `app/src/main/jni/htslibjni.c` and `htslibjni.h` are JNI glue, not core engine; they only require updates if Java JNI package names change or if a clean host/IP pre-check or callback is added.)*

---

## 7. Critical Android API Dependencies (API 22–36)

| API Level | Android Version | Current Upstream Handling | Impact on RaufixTeam WebTools Target (API 22–34+ / Universal APK) |
| :--- | :--- | :--- | :--- |
| **API 22–23** | Android 5.1.1–6.0 | **Unsupported upstream** (`minSdk 24`, `APP_PLATFORM := android-24`). `Application.mk` states `minizip/ioapi.c` calls `fseeko`/`ftello` (>2GB zip-cache seeks), which bionic only exposes at API 24+. Also 32-bit `armeabi-v7a` was dropped upstream. | **Blocker for API 22**: Reaching `minSdk 22` + `armeabi-v7a` requires (a) patching/gating `minizip` 64-bit `fseeko`/`ftello` calls (`USE_FILE32API` or `lseek64` wrapper for `<24`), (b) adding `armeabi-v7a` to `abiFilters` / `APP_ABI` + `build-openssl.sh`, (c) keeping `vendor-libiconv.sh` `HAVE_LANGINFO_CODESET` undef (bionic `nl_langinfo` is API 26+), and (d) guarding runtime permission checks for API 22 (install-time `WRITE_EXTERNAL_STORAGE` vs API 23+ runtime prompt). |
| **API 24–28** | Android 7.0–9.0 | Baseline `minSdk 24`. `NotificationManager` channels guarded for API 26+. `WebSettings.setSaveFormData(false)` kept for `< 26`. | Supported cleanly; ensure `NotificationCompat` channels and `FileProvider` (if files shared externally) work without `FileUriExposedException` (API 24+ forbids `file://` in `Intent`s). |
| **API 29–30** | Android 10–11 (Scoped Storage) | `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion="29"`), `requestLegacyExternalStorage="true"` (API 29), `hasFragileUserData="true"`, `MANAGE_EXTERNAL_STORAGE` (API 30+), `LegacyMirrorImport` SAF tree copy, `MirrorServer` HTTP loopback for browsers. | **Play Store Policy Risk**: `MANAGE_EXTERNAL_STORAGE` faces strict Google Play review rejection for non-file-manager apps. RaufixTeam WebTools should pivot primary user-selected export/storage to **SAF (`ACTION_OPEN_DOCUMENT_TREE`)** and app-private storage + export, removing or making `MANAGE_EXTERNAL_STORAGE` unnecessary. |
| **API 33** | Android 13 | Runtime `POST_NOTIFICATIONS` requested in `HTTrackActivity.ensureNotificationsAreAllowed()` before starting crawl. | Must keep graceful degradation if user denies notifications (crawl proceeds without progress/finish notification). |
| **API 34** | Android 14 (Background limits) | `RUN_USER_INITIATED_JOBS` + `MirrorJobService` (`setUserInitiated(true)`, `NETWORK_TYPE_ANY`, `setNotification()` within 10s). Falls back to `RunnerFragment` if scheduler rejects. | Maintain `MirrorJobService` for API 34+, and ensure `MirrorServer` reliability if browser reading is backgrounded (currently relies on cached process priority). |
| **API 35–36** | Android 15–16 | `EdgeToEdge.fitSystemWindows()` handles mandatory edge-to-edge; `APP_LDFLAGS := -Wl,-z,max-page-size=16384` + `check-so-alignment.sh` enforce 16 KB ELF LOAD alignment. | Must preserve 16 KB `.so` alignment (including when re-adding `armeabi-v7a` if targeted) and edge-to-edge padding across any new/redesigned UI screens. |

---

## 8. Data Migration & `applicationId` Impact Analysis

> **Explicit Upgrade Verdict:** Changing `applicationId` from `com.httrack.android` to an independent ID such as `com.raufixteam.webtools` **will NOT update or upgrade an existing official `com.httrack.android` installation in place**. Android identifies applications strictly by `(applicationId, signing certificate)`.

Consequences of adopting `com.raufixteam.webtools`:
1. **Side-by-Side Installation:** Both Official HTTrack (`com.httrack.android`) and RaufixTeam WebTools (`com.raufixteam.webtools`) can be installed simultaneously without replacing each other.
2. **Private Data & Settings Isolation:**
   - `SharedPreferences` (`HTTrackActivity` `PREFS_NAME`, persisted `BASE_NAME`, `NOTIFY_ASKED_NAME`, `STORAGE_ASKED_NAME`), `getFilesDir()` (including `logins/` cookies and `.hts` login bundles), and `getExternalFilesDir(null)/Websites` (scoped-storage private mirrors) will start **empty** in `com.raufixteam.webtools`.
   - Android prevents `com.raufixteam.webtools` from directly reading `/sdcard/Android/data/com.httrack.android/files/Websites` on API 30+.
3. **Public Storage Collision Risk:**
   - Upstream's `StoragePaths.defaultRoot(...)` defaults to `/sdcard/HTTrack/Websites` whenever all-files access is granted. If both apps use that exact folder simultaneously, concurrent writes or project name collisions on `winprofile.ini` / `hts-cache/` could corrupt mirrors. RaufixTeam WebTools should default to its own workspace/SAF root and provide an explicit read-only **SAF Import** (`LegacyMirrorImport`) from older `HTTrack/Websites` folders.
4. **JNI Namespace Coupling (`htslibjni.c` / `proguard-rules.pro`):**
   - Changing `applicationId` in `app/build.gradle` does **not** require renaming Java packages if `namespace 'com.httrack.android'` is kept temporarily, **but** if the Java packages are refactored to `com.raufixteam.webtools`, then `htslibjni.c`, `htslibjni.h` (`Java_com_httrack_android_jni_HTTrackLib_*`, `FindClass("com/httrack/android/jni/...")`), and `app/proguard-rules.pro` (`-keep class com.httrack.android.jni.**`) MUST be updated in lockstep.

---

## 9. Risk Register

| ID | Category | Risk Description | Severity | Mitigation / Phase |
| :--- | :--- | :--- | :--- | :--- |
| **R-01** | License | Root `LICENSE` was MIT while upstream `httrack`/`httrack-android` is GPLv3+. | **Critical (Resolved in Phase 01)** | Replaced root `LICENSE` with GPLv3 (`COPYING`), added `NOTICE` covering `coffeecatch` (BSD-2-Clause), `coucal` (BSD-3-Clause), `minizip` (Zlib), `OpenSSL` (Apache-2.0), `libiconv` (LGPL-2.1+), etc. |
| **R-02** | Build / Source | GitHub `master.zip` omits all Git submodules (`httrack`, `coffeecatch`, nested `coucal`). | **High (Resolved in Phase 01)** | Documented & enforced `git clone --recurse-submodules` and pinned commit hashes. |
| **R-03** | API / ABI | Target goal includes API 22 & `armeabi-v7a`, but upstream requires API 24 (`minizip` `fseeko`/`ftello`) and dropped `armeabi-v7a`. | **High** | Audit/patch `minizip` API < 24 file I/O and re-enable `armeabi-v7a` in Phase 02/03, verified on API 22 & 34. |
| **R-04** | Security / SSRF | Upstream `httrack` engine does **not** block `127.0.0.1`, RFC1918, link-local (`169.254.0.0/16` metadata), or IPv6 ULA/loopback targets by default. | **High** | Add default private/loopback/link-local/metadata IP & host blocklist with explicit user opt-in for LAN archiving (Phase 06/07). |
| **R-05** | Policy / Storage | `MANAGE_EXTERNAL_STORAGE` risks Google Play rejection; `FileChooserActivity` uses legacy `File.listFiles()`. | **High** | Replace custom `FileChooserActivity` with SAF `ACTION_OPEN_DOCUMENT_TREE` and scoped storage + export (Phase 04/05). |
| **R-06** | Branding / Defaults | `OptionsMapper.java` & `strings.xml` enforce forced `"<!-- Mirrored from {url} by HTTrack Website Copier... -->"` footer (`FooterDefaultTest`) and HTTrack User-Agent/links/update URLs. | **Medium** | Clear default footer to empty string, rebrand UI strings/icons/links to RaufixTeam WebTools, update `FooterDefaultTest` (Phase 02/08). |
| **R-07** | Security / Logging | `htslibjni.c` redirects stdout/stderr to `log.txt` and `error.txt`; care needed to ensure cookies/auth headers/tokens are never logged. | **Medium** | Audit Java & JNI logging paths; ensure `BrowserLoginActivity` cookies/credentials and `OtherHeaders` are never logged (Phase 07). |
| **R-08** | Native Stability | `coffeecatch` uses `siglongjmp`, leaving engine heap/mutexes dirty (`engineFaulted = 1` permanently locks process until restart). | **Medium** | Keep `NativeFaultPolicy` process isolation/exit behavior on `MirrorJobService` and clear user guidance on UI process. |
| **R-09** | Legacy Docs Drift | `app/src/main/jni/libiconv.txt` references `1.15` while build scripts use `1.17`. | **Low** | Documented in Phase 01 audit; clean up in Phase 02 build hardening. |

---

## 10. Gap Analysis Across the 7 Measurable Product Pillars

*(Note: No unmeasured speed claims such as "7x faster" are used; all gaps are tracked as measurable engineering pillars.)*

| # | Pillar | Upstream Baseline State (`39f0f2de`) | RaufixTeam WebTools Target State |
| :--- | :--- | :--- | :--- |
| **1** | **Independent Brand & License Compliance** | `com.httrack.android`, HTTrack name/icons/links, forced HTTrack HTML footer in `OptionsMapper.java`. | `com.raufixteam.webtools`, **RaufixTeam WebTools** branding, empty default HTML footer, full GPLv3+ & third-party `NOTICE` compliance, ad-free & tracker-free. |
| **2** | **Broad Device, ABI & API Coverage** | `minSdk 24`, `targetSdk 36`, 64-bit only (`arm64-v8a`, `x86_64`). | Universal APK supporting `armeabi-v7a` + `arm64-v8a` (plus `x86_64` for emulators), Android 5.1.1 (API 22) through Android 14+ (API 34+), 16 KB ELF alignment preserved. |
| **3** | **Scoped Storage & SAF-First UX** | Relies on `MANAGE_EXTERNAL_STORAGE` for public `HTTrack/Websites`; custom `FileChooserActivity` uses `File.listFiles()`; SAF used only for `LegacyMirrorImport`. | Primary folder selection via Android **SAF** (`ACTION_OPEN_DOCUMENT_TREE`), safe private workspace + SAF export, zero Play-policy `MANAGE_EXTERNAL_STORAGE` dependency. |
| **4** | **Network & SSRF Safety** | Respects `robots.txt` (`FollowRobotsTxt=2`) by default, WebView login enforces HTTPS + `setAllowFileAccess(false)`, but engine allows localhost/RFC1918/metadata targets. | Default-deny SSRF protection blocking localhost, loopback, RFC1918, link-local, ULA, and cloud metadata IPs (with explicit LAN opt-in); strict `robots.txt`/rate-limit defaults; zero secret/cookie logging. |
| **5** | **Reliable Background Execution & Recovery** | `MirrorJobService` (API 34+) + `RunnerFragment` (`<34`), `coffeecatch` fault latch, `MirrorServer` loopback HTTP viewer (30m idle). | Measurable pause/resume/retry reliability across API 22–34+, clean foreground/job lifecycle, deterministic error reporting, and offline browsing reliability. |
| **6** | **Modern, Accessible, Localized UI** | 2017-era Holo theme + multi-pane wizard (`HTTrackActivity` 3,707 lines) + `OptionsActivity` with `@Fields` reflection. | Clean modern Material/AndroidX UI (deferred to Phase 08+ — **NOT** modified in Phase 01), RTL/night/tablet support, non-blocking I/O. |
| **7** | **Automated Verification & Reproducible CI** | 64 JUnit4 unit tests + CI script tests + CodeQL + 16 KB/BTI/PAC checks; no instrumentation smoke suite active yet. | Reproducible NDK/SDK/Docker builds, unit + fixture-server integration + instrumentation smoke matrix across API 22/29/34 and 32/64-bit ABIs. |

---

## 11. 15-Phase Change Plan

1. **Phase 01 (Current):** Repository audit, recursive submodules, license reconciliation (MIT -> GPLv3+ & `NOTICE`), baseline verification, risk register, and Phase 01 audit tests. *(No UI rewrite, no APK claimed.)*
2. **Phase 02:** Fork identity & build baseline (`com.raufixteam.webtools`, JNI/namespace strategy, empty default HTML footer, `libiconv.txt` cleanup, universal ABI + API 22 `minizip` evaluation).
3. **Phase 03:** Native toolchain & ABI verification (`armeabi-v7a` + `arm64-v8a` + `x86_64`, OpenSSL 3.5.8, libiconv 1.17, 16 KB ELF alignment, ARM64 PAC/BTI).
4. **Phase 04:** Storage modernization & SAF-first directory picker (replace `FileChooserActivity` `File.listFiles()` with `ACTION_OPEN_DOCUMENT_TREE`, phase out `MANAGE_EXTERNAL_STORAGE` requirement).
5. **Phase 05:** Legacy/official HTTrack SAF import & data-isolation UX (non-destructive import from user-selected `HTTrack/Websites` into RaufixTeam WebTools storage).
6. **Phase 06:** Network safety & SSRF guardrails (default blocklist for localhost/RFC1918/loopback/link-local/ULA/metadata + explicit LAN opt-in toggle; enforce `robots.txt` & rate-limit defaults).
7. **Phase 07:** Credential, cookie, WebView & logging hardening (audit `BrowserLoginActivity`, `htslibjni.c` stdio/error logs, `OtherHeaders` redaction, no secret logging).
8. **Phase 08:** Rebranding & strings cleanup (`RaufixTeam WebTools` app name, launcher/notification icons, help/about/legal screens, removal of forced HTTrack footer/promo links while preserving GPLv3+ attributions).
9. **Phase 09:** Architecture refactoring of `HTTrackActivity` (3,707 lines) into clean ViewModel/Presenter & repository boundaries without breaking state restoration.
10. **Phase 10:** Modern UI/UX redesign (Material theme, responsive phone/tablet/foldable/RTL/dark layouts, Edge-to-Edge across API 22–36).
11. **Phase 11:** Options & preset profiles overhaul (`OptionsActivity`/`OptionsMapper` simplification, safe defaults, preset templates, validation).
12. **Phase 12:** Background crawl & notification reliability (`MirrorJobService` API 34+, WorkManager/ForegroundService fallback for API 22–33, pause/resume/cancel).
13. **Phase 13:** Offline mirror viewer (`MirrorServer`) & export tools (in-app/Custom Tab preview, ZIP/WARC export via SAF, MIME/charset handling).
14. **Phase 14:** Comprehensive test & emulator/device matrix (JUnit4, local HTTP fixture server integration tests, instrumentation tests across API 22/29/34 & `armeabi-v7a`/`arm64-v8a`).
15. **Phase 15:** Release hardening, reproducible unsigned/CI AAB & APK verification, ProGuard/R8 rules verification, final legal/GPL source bundle check.

---

## 12. Build Commands & Current Verification Results

### 12.1 Reference Build Commands (Upstream Toolchain)

From `.github/workflows/android.yml` and `docker/Dockerfile`:

```bash
# 1. Native cross-build (requires Android NDK r27d, gcc/make/perl/wget for OpenSSL & libiconv)
OUT=/tmp/openssl-android bash docker/build-openssl.sh
tools/ci/vendor-libiconv.sh
OPENSSL_ANDROID_ROOT=/tmp/openssl-android tools/ci/fetch-openssl-statics.sh
(cd app/src/main && "$ANDROID_NDK_ROOT/ndk-build" APP_ABI="arm64-v8a x86_64" APP_PLATFORM=android-24 -j"$(nproc)")
tools/ci/check-branch-protection.sh app/src/main/libs

# 2. Android APK & Unit Tests (requires JDK 17 + Android SDK 36 + Build-Tools 36.0.0 + NDK 27.3.13750724)
./gradlew --no-daemon assembleDebug assembleRelease testDebugUnitTest lint
for apk in app/build/outputs/apk/*/*.apk; do tools/ci/check-so-alignment.sh "$apk"; done
```

### 12.2 Actual Commands Executed in This Environment

Because this sandbox container has **no JDK (`java`/`javac`), no Gradle distribution cache, no Android SDK, and no Android NDK**, no APK or native `.so` binary was compiled or claimed. All feasible repository, submodule, CI script, and Phase 01 audit checks were executed directly.
