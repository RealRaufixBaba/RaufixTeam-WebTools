package com.httrack.android;

import android.content.ContentResolver;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Exports a local mirror directory into a user-selected Storage Access Framework (SAF) folder.
 *
 * Scoped storage on Android 11+ keeps app-private mirrors inside app-specific storage without
 * requiring MANAGE_EXTERNAL_STORAGE. Users who want a copy in Downloads, Documents, or an SD card
 * pick a destination folder via ACTION_OPEN_DOCUMENT_TREE and export on demand.
 */
final class SafMirrorExport {
  private static final int BUFFER = 64 * 1024;
  static final int MAX_DEPTH = 100;
  private static final int MAX_ERRORS = 20;

  private SafMirrorExport() {
  }

  /** Destination directory abstraction so exports can be unit-tested without DocumentFile. */
  interface DestDir {
    DestDir findOrCreateDir(String name) throws IOException;

    OutputStream openOrCreateFile(String name, String mimeType) throws IOException;
  }

  static final class Result {
    int copied;
    int skipped;
    int failed;
    final List<String> errors = new ArrayList<String>();

    boolean isComplete() {
      return failed == 0;
    }

    String firstError() {
      return errors.isEmpty() ? null : errors.get(0);
    }
  }

  static Result exportTree(final android.content.Context context, final Uri treeUri,
      final File sourceDir, final String exportFolderName) {
    final Result result = new Result();
    if (context == null || treeUri == null || sourceDir == null || !sourceDir.isDirectory()) {
      result.failed++;
      result.errors.add("Invalid export source or destination");
      return result;
    }
    final DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
    if (root == null || !root.canWrite()) {
      result.failed++;
      result.errors.add("Destination folder is not writable");
      return result;
    }
    return exportWithRoot(
        new DocumentDestDir(context.getContentResolver(), root),
        sourceDir,
        exportFolderName);
  }

  static Result exportWithRoot(final DestDir root, final File sourceDir,
      final String exportFolderName) {
    final Result result = new Result();
    if (root == null || sourceDir == null || !sourceDir.isDirectory()) {
      result.failed++;
      result.errors.add("Invalid export source or destination");
      return result;
    }
    final String folderName = (exportFolderName != null && !exportFolderName.trim().isEmpty())
        ? exportFolderName.trim()
        : sourceDir.getName();
    try {
      final DestDir target = root.findOrCreateDir(folderName);
      if (target == null) {
        result.failed++;
        result.errors.add("Could not create export folder " + folderName);
        return result;
      }
      copyDirectory(sourceDir, target, 0, result);
    } catch (final IOException e) {
      recordError(result, e.getMessage());
    }
    return result;
  }

  private static void copyDirectory(final File srcDir, final DestDir dstDir,
      final int depth, final Result result) {
    if (depth >= MAX_DEPTH) {
      result.failed++;
      recordError(result, "Directory nesting exceeded " + MAX_DEPTH + " levels");
      return;
    }
    final File[] entries = srcDir.listFiles();
    if (entries == null) {
      return;
    }
    final byte[] buf = new byte[BUFFER];
    for (final File entry : entries) {
      final String name = entry.getName();
      if (name == null || name.isEmpty() || ".".equals(name) || "..".equals(name)) {
        continue;
      }
      if (entry.isDirectory()) {
        try {
          final DestDir childDir = dstDir.findOrCreateDir(name);
          if (childDir == null) {
            result.failed++;
            recordError(result, "Could not create folder: " + name);
            continue;
          }
          copyDirectory(entry, childDir, depth + 1, result);
        } catch (final IOException e) {
          result.failed++;
          recordError(result, name + ": " + e.getMessage());
        }
      } else if (entry.isFile()) {
        try (InputStream in = new FileInputStream(entry);
             OutputStream out = dstDir.openOrCreateFile(name, guessMimeType(name))) {
          if (out == null) {
            result.skipped++;
            continue;
          }
          int n;
          while ((n = in.read(buf)) >= 0) {
            if (n > 0) {
              out.write(buf, 0, n);
            }
          }
          out.flush();
          result.copied++;
        } catch (final IOException e) {
          result.failed++;
          recordError(result, name + ": " + e.getMessage());
        }
      }
    }
  }

  static String guessMimeType(final String name) {
    final int dot = name.lastIndexOf('.');
    if (dot >= 0 && dot < name.length() - 1) {
      final String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
      if ("html".equals(ext) || "htm".equals(ext)) {
        return "text/html";
      }
      if ("css".equals(ext)) {
        return "text/css";
      }
      if ("js".equals(ext)) {
        return "application/javascript";
      }
      if ("json".equals(ext)) {
        return "application/json";
      }
      if ("png".equals(ext)) {
        return "image/png";
      }
      if ("jpg".equals(ext) || "jpeg".equals(ext)) {
        return "image/jpeg";
      }
      final MimeTypeMap map = MimeTypeMap.getSingleton();
      final String mime = map != null ? map.getMimeTypeFromExtension(ext) : null;
      if (mime != null && !mime.isEmpty()) {
        return mime;
      }
    }
    return "application/octet-stream";
  }

  private static void recordError(final Result result, final String message) {
    if (result.errors.size() < MAX_ERRORS && message != null && !message.isEmpty()) {
      result.errors.add(message);
    }
  }

  private static final class DocumentDestDir implements DestDir {
    private final ContentResolver resolver;
    private final DocumentFile dir;

    DocumentDestDir(final ContentResolver resolver, final DocumentFile dir) {
      this.resolver = resolver;
      this.dir = dir;
    }

    @Override
    public DestDir findOrCreateDir(final String name) throws IOException {
      final DocumentFile existing = dir.findFile(name);
      if (existing != null && existing.isDirectory()) {
        return new DocumentDestDir(resolver, existing);
      }
      final DocumentFile created = dir.createDirectory(name);
      if (created == null) {
        throw new IOException("createDirectory failed for " + name);
      }
      return new DocumentDestDir(resolver, created);
    }

    @Override
    public OutputStream openOrCreateFile(final String name, final String mimeType)
        throws IOException {
      DocumentFile file = dir.findFile(name);
      if (file != null && file.isFile()) {
        file.delete();
      }
      file = dir.createFile(mimeType, name);
      if (file == null) {
        throw new IOException("createFile failed for " + name);
      }
      final OutputStream out = resolver.openOutputStream(file.getUri(), "wt");
      if (out == null) {
        throw new IOException("openOutputStream returned null for " + name);
      }
      return out;
    }
  }
}
