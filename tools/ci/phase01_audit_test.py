#!/usr/bin/env python3
"""Deterministic verification tests for RaufixTeam WebTools (Phases 01-15)."""

from pathlib import Path
import re
import subprocess
import unittest


REPO_ROOT = Path(__file__).resolve().parents[2]


class Phase01AuditTest(unittest.TestCase):
    def read(self, rel_path: str) -> str:
        path = REPO_ROOT / rel_path
        self.assertTrue(path.is_file(), f"Missing expected file: {rel_path}")
        return path.read_text(encoding="utf-8", errors="replace")

    def test_pinned_submodules_checked_out(self) -> None:
        out = subprocess.check_output(
            ["git", "submodule", "status", "--recursive"],
            cwd=REPO_ROOT,
            text=True,
        )
        expected = {
            "app/src/main/jni/coffeecatch": "b14cdb0e76a26f92bf6d097c88e6bc2ea9a747ef",
            "app/src/main/jni/httrack": "7a923f4047df1bec18e2ce1b4403c7038d055021",
            "app/src/main/jni/httrack/src/coucal": "031eaf18ad0a8e1def4ec8dd2f7e077c5dac03b0",
        }
        for path, sha in expected.items():
            self.assertRegex(
                out,
                rf"(?m)^[ +-]{re.escape(sha)}\s+{re.escape(path)}\b",
                f"Submodule {path} not pinned to {sha}",
            )
            self.assertTrue(any((REPO_ROOT / path).iterdir()), f"Submodule {path} is empty")

        htsglobal = self.read("app/src/main/jni/httrack/src/htsglobal.h")
        self.assertIn('#define HTTRACK_VERSIONID "3.51.0"', htsglobal)

    def test_raufix_build_and_abi_configuration(self) -> None:
        root_gradle = self.read("build.gradle")
        self.assertIn("id 'com.android.application' version '8.6.0' apply false", root_gradle)

        wrapper = self.read("gradle/wrapper/gradle-wrapper.properties")
        self.assertIn("gradle-8.7-bin.zip", wrapper)

        app_gradle = self.read("app/build.gradle")
        for expected in (
            "namespace 'com.httrack.android'",
            'applicationId "com.raufixteam.webtools"',
            "compileSdk 36",
            "targetSdk 36",
            "minSdk 22",
            'ndkVersion "27.3.13750724"',
            "sourceCompatibility JavaVersion.VERSION_17",
            "targetCompatibility JavaVersion.VERSION_17",
            '"armeabi-v7a"',
            '"arm64-v8a"',
            '"x86_64"',
        ):
            self.assertIn(expected, app_gradle)

        app_mk = self.read("app/src/main/jni/Application.mk")
        self.assertIn("APP_ABI := armeabi-v7a arm64-v8a x86_64", app_mk)
        self.assertIn("APP_PLATFORM := android-22", app_mk)
        self.assertTrue((REPO_ROOT / "app/src/main/jni/include/android_lfs_compat.h").is_file())

    def test_license_compliance_and_notices(self) -> None:
        root_license = self.read("LICENSE")
        self.assertIn("GNU GENERAL PUBLIC LICENSE", root_license)
        self.assertIn("Version 3, 29 June 2007", root_license)
        self.assertNotIn("MIT License", root_license)

        httrack_copying = self.read("app/src/main/jni/httrack/COPYING")
        self.assertEqual(root_license, httrack_copying)

        notice = self.read("NOTICE")
        for term in (
            "39f0f2de2fbce28f95b19bfdc8205510e3e07d26",
            "GPL-3.0-or-later",
            "coffeecatch",
            "coucal",
            "minizip",
            "libiconv",
            "OpenSSL 3.5.8",
            "NanoHTTPD",
        ):
            self.assertIn(term, notice)

    def test_footer_manifest_saf_and_ssrf_hardening(self) -> None:
        mapper = self.read("app/src/main/java/com/httrack/android/OptionsMapper.java")
        self.assertIn('new Pair<String, String>("Footer", "")', mapper)
        self.assertIn('AllowEmptyArgumentOption("-%F")', mapper)

        strings_xml = self.read("app/src/main/res/values/strings.xml")
        self.assertIn("<string name=\"app_name\">RaufixTeam WebTools</string>", strings_xml)
        self.assertTrue((REPO_ROOT / "app/src/main/res/values-tr/strings.xml").is_file())

        manifest = self.read("app/src/main/AndroidManifest.xml")
        self.assertNotIn("MANAGE_EXTERNAL_STORAGE", manifest)
        for component in (
            "android.permission.INTERNET",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.RUN_USER_INITIATED_JOBS",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.ACCESS_NETWORK_STATE",
            "com.httrack.android.HTTrackActivity",
            ".MirrorJobService",
        ):
            self.assertIn(component, manifest)

        self.assertTrue((REPO_ROOT / "app/src/main/java/com/httrack/android/UrlSafetyPolicy.java").is_file())
        self.assertTrue((REPO_ROOT / "app/src/main/java/com/httrack/android/SafMirrorExport.java").is_file())

    def test_jni_coupling_and_engine_untouched(self) -> None:
        jni_c = self.read("app/src/main/jni/htslibjni.c")
        jni_h = self.read("app/src/main/jni/htslibjni.h")
        self.assertIn("Java_com_httrack_android_jni_HTTrackLib_main", jni_c)
        self.assertIn("Java_com_httrack_android_jni_HTTrackLib_setAllowLanArchiving", jni_c)
        self.assertIn("CHAIN_FUNCTION(context->opt, check_link, rwt_check_link", jni_c)
        self.assertIn("Java_com_httrack_android_jni_HTTrackLib_initStatic", jni_h)
        self.assertTrue((REPO_ROOT / "app/src/main/jni/httrack/src/htscore.c").is_file())
        self.assertTrue((REPO_ROOT / "app/src/main/jni/coffeecatch/coffeecatch.c").is_file())

    def test_audit_report_required_deliverables(self) -> None:
        report = self.read("docs/phase-01/AUDIT_REPORT.md")
        for required in (
            "39f0f2de2fbce28f95b19bfdc8205510e3e07d26",
            "git clone --recurse-submodules",
            "15-Phase Change Plan",
            "7 Measurable Product Pillars",
        ):
            self.assertIn(required, report)


if __name__ == "__main__":
    unittest.main(verbosity=2)
