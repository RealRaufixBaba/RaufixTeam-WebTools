#ifndef RWT_ANDROID_LFS_COMPAT_H
#define RWT_ANDROID_LFS_COMPAT_H

#ifndef _LARGEFILE64_SOURCE
#define _LARGEFILE64_SOURCE 1
#endif

#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>

/* Bionic omits 64-bit fseeko/ftello on 32-bit targets below API 24.
   Provide a 64-bit lseek64 shim so armeabi-v7a builds cleanly at android-22
   and satisfies htswarc.c's 64-bit ftello() static assertion. */
#if defined(__ANDROID__) && defined(__ANDROID_API__) && (__ANDROID_API__ < 24) && !defined(__LP64__)
static inline off64_t rwt_ftello64(FILE *f) {
  if (fflush(f) != 0) {
    return (off64_t)-1;
  }
  return lseek64(fileno(f), (off64_t)0, SEEK_CUR);
}

static inline int rwt_fseeko64(FILE *f, off64_t offset, int whence) {
  if (fflush(f) != 0) {
    return -1;
  }
  return lseek64(fileno(f), offset, whence) == (off64_t)-1 ? -1 : 0;
}

#undef fseeko
#undef ftello
#define fseeko(f, o, w) rwt_fseeko64((f), (off64_t)(o), (w))
#define ftello(f) rwt_ftello64((f))
#endif

#endif /* RWT_ANDROID_LFS_COMPAT_H */
