# RaufixTeam WebTools

Independent GPLv3+ fork of [HTTrack Website Copier for Android](https://github.com/xroche/httrack-android), audited and prepared under the **RaufixTeam WebTools** roadmap.

## Upstream Provenance (Pinned Baseline)

- **Upstream Repository:** `https://github.com/xroche/httrack-android`
- **Pinned Upstream Commit:** `39f0f2de2fbce28f95b19bfdc8205510e3e07d26`
- **Submodules (Recursive):**
  - `app/src/main/jni/coffeecatch` @ `b14cdb0e76a26f92bf6d097c88e6bc2ea9a747ef` (`https://github.com/xroche/coffeecatch.git`)
  - `app/src/main/jni/httrack` @ `7a923f4047df1bec18e2ce1b4403c7038d055021` (`https://github.com/xroche/httrack.git`, `3.51.0-112-g7a923f40`)
  - `app/src/main/jni/httrack/src/coucal` @ `031eaf18ad0a8e1def4ec8dd2f7e077c5dac03b0` (`https://github.com/xroche/coucal.git`)

## Reproducible Full-Source Checkout

GitHub's `master.zip` source archive omits submodule contents (`app/src/main/jni/coffeecatch/` and `app/src/main/jni/httrack/` are empty directories, and nested `src/coucal/` is absent). Always clone recursively:

```bash
git clone --recurse-submodules https://github.com/RealRaufixBaba/RaufixTeam-WebTools.git
cd RaufixTeam-WebTools
git submodule status --recursive
```

To initialize submodules in an existing checkout:

```bash
git submodule update --init --recursive
```

## License

This repository and combined Android application are licensed under the **GNU General Public License v3.0 or later (GPL-3.0-or-later)**; see [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE) for full upstream and third-party copyright and license notices (including `httrack`, `coffeecatch`, `coucal`, `minizip`, `libiconv`, `OpenSSL`, `NanoHTTPD`, and `AndroidX`).

## Phase 01 Audit Deliverables

- [`docs/phase-01/AUDIT_REPORT.md`](docs/phase-01/AUDIT_REPORT.md) — Complete Phase 01 audit: commit/submodule manifest, `master.zip` gap analysis, `app/build.gradle` baseline verification, license & obligations table, architecture sketch, behavior map, untouched engine components, risk register, critical Android API dependencies (API 22–36), `applicationId` data migration impact, 7-pillar gap analysis, 15-phase plan, and build/test verification results.
- [`tools/ci/verify-phase01-audit.sh`](tools/ci/verify-phase01-audit.sh) — Deterministic Phase 01 repository, submodule, baseline, license, and JNI-inventory verification checks.
