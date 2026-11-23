package com.httrack.android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import org.junit.Test;

public class BrandAndConfigTest {

  private static File repoFile(final String relative) {
    for (final String prefix : new String[] { "", "../" }) {
      final File f = new File(prefix + relative);
      if (f.exists()) {
        return f;
      }
    }
    throw new IllegalStateException("missing " + relative);
  }

  @Test
  public void manifestDropsAllFilesAccessAndRetainsMinimalPermissions() throws IOException {
    final String manifest = TestSources.read(TestSources.mainFile("AndroidManifest.xml"));
    assertFalse("MANAGE_EXTERNAL_STORAGE must not be declared",
        manifest.contains("MANAGE_EXTERNAL_STORAGE"));
    assertTrue(manifest.contains("android.permission.INTERNET"));
    assertTrue(manifest.contains("android.permission.ACCESS_NETWORK_STATE"));
  }

  @Test
  public void buildConfigSetsRaufixIdMinSdk22AndUniversalAbis() throws IOException {
    final String gradle = TestSources.read(repoFile("app/build.gradle"));
    assertTrue(gradle.contains("applicationId \"com.raufixteam.webtools\""));
    assertTrue(gradle.contains("minSdk 22"));
    assertTrue(gradle.contains("targetSdk 36"));
    assertTrue(gradle.contains("\"armeabi-v7a\""));
    assertTrue(gradle.contains("\"arm64-v8a\""));
    assertTrue(gradle.contains("\"x86_64\""));
  }

  @Test
  public void nativeApplicationMkTargetsApi22UniversalAbis() throws IOException {
    final String appMk = TestSources.jniSource("Application.mk");
    assertTrue(appMk.contains("APP_PLATFORM := android-22"));
    assertTrue(appMk.contains("armeabi-v7a"));
    assertTrue(appMk.contains("arm64-v8a"));
    assertTrue(appMk.contains("x86_64"));
  }

  @Test
  public void jniRegistersNativeCheckLinkHook() throws IOException {
    final String jni = TestSources.jniSource("htslibjni.c");
    assertTrue(jni.contains("CHAIN_FUNCTION(context->opt, check_link, rwt_check_link"));
    assertTrue(jni.contains("Java_com_httrack_android_jni_HTTrackLib_setAllowLanArchiving"));
  }

  @Test
  public void brandingAndLicenseAreConsistent() throws IOException {
    final String strings = TestSources.read(TestSources.resFile("values/strings.xml"));
    assertTrue(strings.contains("<string name=\"app_name\">RaufixTeam WebTools</string>"));
    final String license = TestSources.read(repoFile("LICENSE"));
    assertTrue(license.contains("GNU GENERAL PUBLIC LICENSE"));
    assertTrue(license.contains("Version 3, 29 June 2007"));
  }
}
