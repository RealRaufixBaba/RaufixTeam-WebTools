# Universal ABI coverage: 32-bit armeabi-v7a, 64-bit arm64-v8a, and x86_64.
# APP_PLATFORM is android-22 (Android 5.1.1); android_lfs_compat.h provides
# 64-bit fseeko/ftello over lseek64 for 32-bit targets below API 24.
APP_ABI := armeabi-v7a arm64-v8a x86_64
APP_PLATFORM := android-22

# Play requires 16 KB-aligned LOAD segments at targetSdk 35+; kept explicit though r27's lld defaults to 16 KB.
APP_LDFLAGS := -Wl,-z,max-page-size=16384
