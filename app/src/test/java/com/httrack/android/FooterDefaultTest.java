package com.httrack.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * RaufixTeam WebTools defaults to an empty HTML footer and must pass -%F "" to the
 * engine so HTS_DEFAULT_FOOTER is suppressed unless the user configures one.
 */
public class FooterDefaultTest {
  private static final Pattern MAPPER_DEFAULT = Pattern.compile(
      "\"Footer\",\\s*\"(.*?)\"\\)", Pattern.DOTALL);

  private static final Pattern HINT = Pattern
      .compile("<string name=\"hint_footer\"[^>]*>(.*?)</string>", Pattern.DOTALL);

  private static String group(final Pattern pattern, final String text) {
    final Matcher m = pattern.matcher(text);
    assertTrue("no match for " + pattern, m.find());
    return m.group(1);
  }

  private static String mapperDefault() throws IOException {
    return group(MAPPER_DEFAULT, TestSources.javaSource("OptionsMapper"));
  }

  @Test
  public void defaultFooterIsEmpty() throws IOException {
    assertEquals("", mapperDefault());
  }

  @Test
  public void emptyFooterEmitsExplicitSuppressionFlag() {
    final OptionsMapper mapper = new OptionsMapper();
    mapper.setMap(R.id.fieldWebsiteURLs, "https://example.com/");
    final List<String> cmd = mapper.buildCommandline();
    final int idx = cmd.indexOf("-%F");
    assertTrue("expected -%F flag in " + cmd, idx >= 0);
    assertEquals("", cmd.get(idx + 1));
  }

  @Test
  public void customFooterIsPreserved() {
    final OptionsMapper mapper = new OptionsMapper();
    mapper.setMap(R.id.fieldWebsiteURLs, "https://example.com/");
    mapper.setMap(R.id.editHtmlFooter, "<!-- archived -->");
    final List<String> cmd = mapper.buildCommandline();
    final int idx = cmd.indexOf("-%F");
    assertTrue("expected -%F flag in " + cmd, idx >= 0);
    assertEquals("<!-- archived -->", cmd.get(idx + 1));
  }

  @Test
  public void hintMatchesEmptyDefaultWithoutLegacyBranding() throws IOException {
    final String hint = group(HINT,
        TestSources.read(TestSources.resFile("values/strings.xml")));
    assertFalse(hint, hint.contains("%s"));
    assertFalse(hint, hint.contains("HTTrack"));
  }
}
