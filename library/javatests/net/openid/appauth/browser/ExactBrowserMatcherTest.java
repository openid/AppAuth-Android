package net.openid.appauth.browser;

import org.junit.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExactBrowserMatcher}.
 */
public class ExactBrowserMatcherTest {

    private static final BrowserDescriptor CHROME_55 = Browsers.Chrome.standaloneBrowser("55");
    private static final BrowserMatcher MATCHER = new ExactBrowserMatcher(CHROME_55);

    @Test
    public void testMatches_same() {
        assertThat(MATCHER.matches(CHROME_55)).isTrue();
    }

    @Test
    public void testMatches_equal() {
        assertThat(MATCHER.matches(Browsers.Chrome.standaloneBrowser("55"))).isTrue();
    }

    @Test
    public void testMatches_differentVersion() {
        assertThat(MATCHER.matches(Browsers.Chrome.standaloneBrowser("54"))).isFalse();
    }

    @Test
    public void testMatches_differentKey() {
        HashSet<String> badHash = new HashSet<>();
        badHash.add("BADHASH");
        BrowserDescriptor badChrome55Standalone = new BrowserDescriptor(
                Browsers.Chrome.PACKAGE_NAME,
                badHash,
                "55",
                false);
        assertThat(MATCHER.matches(badChrome55Standalone)).isFalse();
    }

    @Test
    public void testMatches_differentBrowser() {
        assertThat(MATCHER.matches(Browsers.Chrome.standaloneBrowser("50"))).isFalse();
    }
}
