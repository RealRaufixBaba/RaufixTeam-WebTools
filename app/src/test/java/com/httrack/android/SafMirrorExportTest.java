package com.httrack.android;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SafMirrorExportTest {
  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  private static final class MemoryDir implements SafMirrorExport.DestDir {
    final Map<String, MemoryDir> dirs = new LinkedHashMap<String, MemoryDir>();
    final Map<String, ByteArrayOutputStream> files =
        new LinkedHashMap<String, ByteArrayOutputStream>();
    final Map<String, String> mimes = new LinkedHashMap<String, String>();

    @Override
    public SafMirrorExport.DestDir findOrCreateDir(final String name) {
      MemoryDir child = dirs.get(name);
      if (child == null) {
        child = new MemoryDir();
        dirs.put(name, child);
      }
      return child;
    }

    @Override
    public OutputStream openOrCreateFile(final String name, final String mimeType) {
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      files.put(name, out);
      mimes.put(name, mimeType);
      return out;
    }
  }

  @Test
  public void exportsNestedMirrorTreeWithExpectedContents() throws IOException {
    final File src = tmp.newFolder("example-mirror");
    write(new File(src, "index.html"), "<html>ok</html>");
    final File assets = new File(src, "assets");
    assertTrue(assets.mkdirs());
    write(new File(assets, "style.css"), "body{}");
    write(new File(assets, "empty.txt"), "");

    final MemoryDir root = new MemoryDir();
    final SafMirrorExport.Result result =
        SafMirrorExport.exportWithRoot(root, src, "MyMirror");

    assertTrue(result.isComplete());
    assertEquals(3, result.copied);
    assertEquals(0, result.failed);

    final MemoryDir exported = root.dirs.get("MyMirror");
    assertArrayEquals("<html>ok</html>".getBytes(StandardCharsets.UTF_8),
        exported.files.get("index.html").toByteArray());
    assertEquals("text/html", exported.mimes.get("index.html"));
    final MemoryDir exportedAssets = exported.dirs.get("assets");
    assertArrayEquals("body{}".getBytes(StandardCharsets.UTF_8),
        exportedAssets.files.get("style.css").toByteArray());
    assertEquals("text/css", exportedAssets.mimes.get("style.css"));
    assertEquals(0, exportedAssets.files.get("empty.txt").toByteArray().length);
  }

  @Test
  public void missingSourceFailsCleanly() throws IOException {
    final File missing = new File(tmp.getRoot(), "does-not-exist");
    final MemoryDir root = new MemoryDir();
    final SafMirrorExport.Result result =
        SafMirrorExport.exportWithRoot(root, missing, "Nope");
    assertFalse(result.isComplete());
    assertEquals(1, result.failed);
  }

  private static void write(final File file, final String text) throws IOException {
    try (FileOutputStream out = new FileOutputStream(file)) {
      out.write(text.getBytes(StandardCharsets.UTF_8));
    }
  }
}
