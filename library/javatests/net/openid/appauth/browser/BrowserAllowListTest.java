/*
 * Copyright 2016 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openid.appauth.browser;

import static org.assertj.core.api.Assertions.assertThat;

import net.openid.appauth.BuildConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class BrowserAllowListTest {

    @Test
    public void testMatches_emptyAllowList() {
        BrowserAllowList allowList = new BrowserAllowList();
        assertThat(allowList.matches(Browsers.Chrome.customTab("46"))).isFalse();
        assertThat(allowList.matches(Browsers.Firefox.standaloneBrowser("10"))).isFalse();
        assertThat(allowList.matches(Browsers.Firefox.customTab("57"))).isFalse();
        assertThat(allowList.matches(Browsers.SBrowser.standaloneBrowser("11"))).isFalse();
    }

    @Test
    public void testMatches_chromeBrowserOnly() {
        BrowserAllowList allowList = new BrowserAllowList(VersionedBrowserMatcher.CHROME_BROWSER);
        assertThat(allowList.matches(Browsers.Chrome.standaloneBrowser("46"))).isTrue();
        assertThat(allowList.matches(Browsers.Chrome.customTab("46"))).isFalse();
        assertThat(allowList.matches(Browsers.Firefox.standaloneBrowser("10"))).isFalse();
        assertThat(allowList.matches(Browsers.Firefox.customTab("57"))).isFalse();
    }

    @Test
    public void testMatches_chromeCustomTabOrBrowser() {
        BrowserAllowList allowList = new BrowserAllowList(
                VersionedBrowserMatcher.CHROME_BROWSER,
                VersionedBrowserMatcher.CHROME_CUSTOM_TAB);
        assertThat(allowList.matches(Browsers.Chrome.standaloneBrowser("46"))).isTrue();
        assertThat(allowList.matches(Browsers.Chrome.customTab("46"))).isTrue();
        assertThat(allowList.matches(Browsers.Firefox.standaloneBrowser("10"))).isFalse();
        assertThat(allowList.matches(Browsers.Firefox.customTab("57"))).isFalse();
    }

    @Test
    public void testMatches_firefoxOrSamsung() {
        BrowserAllowList allowList = new BrowserAllowList(
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
