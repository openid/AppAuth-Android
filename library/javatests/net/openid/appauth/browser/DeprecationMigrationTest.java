package net.openid.appauth.browser;

import net.openid.appauth.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class DeprecationMigrationTest {

    @Test
    public void testMatches_emptyDenyList() {
        BrowserDenyList denyList = new BrowserBlacklist();
        assertThat(denyList.matches(Browsers.Chrome.customTab("46"))).isTrue();
        assertThat(denyList.matches(Browsers.Firefox.standaloneBrowser("10"))).isTrue();
        assertThat(denyList.matches(Browsers.SBrowser.standaloneBrowser("11"))).isTrue();
    }

    @Test
    public void testMatches_singleBrowser() {
        BrowserDenyList denyList = new BrowserBlacklist(VersionedBrowserMatcher.FIREFOX_BROWSER);
        assertThat(denyList.matches(Browsers.Chrome.customTab("46"))).isTrue();
        assertThat(denyList.matches(Browsers.Firefox.standaloneBrowser("10"))).isFalse();
        assertThat(denyList.matches(Browsers.SBrowser.standaloneBrowser("11"))).isTrue();
    }

    @Test
    public void testMatches_customTabs() {
        BrowserDenyList denyList = new BrowserBlacklist(
            VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
            VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB);

        assertThat(denyList.matches(Browsers.Chrome.standaloneBrowser("46"))).isTrue();
        assertThat(denyList.matches(Browsers.Chrome.customTab("46"))).isFalse();
        assertThat(denyList.matches(Browsers.SBrowser.standaloneBrowser("11"))).isTrue();
        assertThat(denyList.matches(Browsers.SBrowser.customTab("11"))).isFalse();
    }


    @Test
    public void testMatches_emptyAllowList() {
        BrowserAllowList allowList = new BrowserWhitelist();
        assertThat(allowList.matches(Browsers.Chrome.customTab("46"))).isFalse();
        assertThat(allowList.matches(Browsers.Firefox.standaloneBrowser("10"))).isFalse();
        assertThat(allowList.matches(Browsers.Firefox.customTab("57"))).isFalse();
        assertThat(allowList.matches(Browsers.SBrowser.standaloneBrowser("11"))).isFalse();
    }

    @Test
    public void testMatches_chromeBrowserOnly() {
        BrowserAllowList allowList = new BrowserWhitelist(VersionedBrowserMatcher.CHROME_BROWSER);
        assertThat(allowList.matches(Browsers.Chrome.standaloneBrowser("46"))).isTrue();
        assertThat(allowList.matches(Browsers.Chrome.customTab("46"))).isFalse();
        assertThat(allowList.matches(Browsers.Firefox.standaloneBrowser("10"))).isFalse();
        assertThat(allowList.matches(Browsers.Firefox.customTab("57"))).isFalse();
    }

    @Test
    public void testMatches_chromeCustomTabOrBrowser() {
        BrowserAllowList allowList = new BrowserWhitelist(
            VersionedBrowserMatcher.CHROME_BROWSER,
            VersionedBrowserMatcher.CHROME_CUSTOM_TAB);
        assertThat(allowList.matches(Browsers.Chrome.standaloneBrowser("46"))).isTrue();
        assertThat(allowList.matches(Browsers.Chrome.customTab("46"))).isTrue();
        assertThat(allowList.matches(Browsers.Firefox.standaloneBrowser("10"))).isFalse();
        assertThat(allowList.matches(Browsers.Firefox.customTab("57"))).isFalse();
    }

    @Test
    public void testMatches_firefoxOrSamsung() {
        BrowserAllowList allowList = new BrowserWhitelist(
            VersionedBrowserMatcher.FIREFOX_BROWSER,
            VersionedBrowserMatcher.FIREFOX_CUSTOM_TAB,
            VersionedBrowserMatcher.SAMSUNG_BROWSER,
            VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB);
        assertThat(allowList.matches(Browsers.Chrome.standaloneBrowser("46"))).isFalse();
        assertThat(allowList.matches(Browsers.Chrome.customTab("46"))).isFalse();
        assertThat(allowList.matches(Browsers.Firefox.standaloneBrowser("10"))).isTrue();
        assertThat(allowList.matches(Browsers.Firefox.customTab("56"))).isFalse();
        assertThat(allowList.matches(Browsers.Firefox.customTab("57"))).isTrue();
        assertThat(allowList.matches(Browsers.SBrowser.standaloneBrowser("10"))).isTrue();
        assertThat(allowList.matches(Browsers.SBrowser.customTab("4.0"))).isTrue();
        assertThat(allowList.matches(Browsers.SBrowser.customTab("3.9"))).isFalse();
    }
}
