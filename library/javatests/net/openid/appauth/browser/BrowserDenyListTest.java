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
public class BrowserDenyListTest {

    @Test
    public void testMatches_emptyDenyList() {
        BrowserDenyList denyList = new BrowserDenyList();
        assertThat(denyList.matches(Browsers.Chrome.customTab("46"))).isTrue();
        assertThat(denyList.matches(Browsers.Firefox.standaloneBrowser("10"))).isTrue();
        assertThat(denyList.matches(Browsers.SBrowser.standaloneBrowser("11"))).isTrue();
    }

    @Test
    public void testMatches_singleBrowser() {
        BrowserDenyList denyList = new BrowserDenyList(VersionedBrowserMatcher.FIREFOX_BROWSER);
        assertThat(denyList.matches(Browsers.Chrome.customTab("46"))).isTrue();
        assertThat(denyList.matches(Browsers.Firefox.standaloneBrowser("10"))).isFalse();
        assertThat(denyList.matches(Browsers.SBrowser.standaloneBrowser("11"))).isTrue();
    }

    @Test
    public void testMatches_customTabs() {
        BrowserDenyList denyList = new BrowserDenyList(
                VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
                VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB);

        assertThat(denyList.matches(Browsers.Chrome.standaloneBrowser("46"))).isTrue();
        assertThat(denyList.matches(Browsers.Chrome.customTab("46"))).isFalse();
        assertThat(denyList.matches(Browsers.SBrowser.standaloneBrowser("11"))).isTrue();
        assertThat(denyList.matches(Browsers.SBrowser.customTab("11"))).isFalse();
    }
}
