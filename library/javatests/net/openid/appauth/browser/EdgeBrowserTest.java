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

import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class EdgeBrowserTest {

    @Test
    public void testEdgePackageName() {
        assertThat(Browsers.Edge.PACKAGE_NAME).isEqualTo("com.microsoft.emmx");
    }

    @Test
    public void testEdgeSignatureSetNotEmpty() {
        assertThat(Browsers.Edge.SIGNATURE_SET).isNotEmpty();
        assertThat(Browsers.Edge.SIGNATURE_SET).hasSize(1);
    }

    @Test
    public void testEdgeMinimumVersionForCustomTab() {
        assertThat(Browsers.Edge.MINIMUM_VERSION_FOR_CUSTOM_TAB)
                .isEqualTo(DelimitedVersion.parse("45"));
    }

    @Test
    public void testEdgeCustomTabDescriptor() {
        BrowserDescriptor descriptor = Browsers.Edge.customTab("50");
        assertThat(descriptor.packageName).isEqualTo(Browsers.Edge.PACKAGE_NAME);
        assertThat(descriptor.signatureHashes).isEqualTo(Browsers.Edge.SIGNATURE_SET);
        assertThat(descriptor.version).isEqualTo("50");
        assertThat(descriptor.useCustomTab).isTrue();
    }

    @Test
    public void testEdgeStandaloneBrowserDescriptor() {
        BrowserDescriptor descriptor = Browsers.Edge.standaloneBrowser("50");
        assertThat(descriptor.packageName).isEqualTo(Browsers.Edge.PACKAGE_NAME);
        assertThat(descriptor.signatureHashes).isEqualTo(Browsers.Edge.SIGNATURE_SET);
        assertThat(descriptor.version).isEqualTo("50");
        assertThat(descriptor.useCustomTab).isFalse();
    }

    @Test
    public void testEdgeCustomTabMatcher_matchesEdgeCustomTab() {
        BrowserDescriptor edgeCustomTab = Browsers.Edge.customTab("50");
        assertThat(VersionedBrowserMatcher.EDGE_CUSTOM_TAB.matches(edgeCustomTab)).isTrue();
    }

    @Test
    public void testEdgeCustomTabMatcher_doesNotMatchOldVersion() {
        BrowserDescriptor oldEdge = Browsers.Edge.customTab("44");
        assertThat(VersionedBrowserMatcher.EDGE_CUSTOM_TAB.matches(oldEdge)).isFalse();
    }

    @Test
    public void testEdgeCustomTabMatcher_doesNotMatchStandalone() {
        BrowserDescriptor edgeStandalone = Browsers.Edge.standaloneBrowser("50");
        assertThat(VersionedBrowserMatcher.EDGE_CUSTOM_TAB.matches(edgeStandalone)).isFalse();
    }

    @Test
    public void testEdgeCustomTabMatcher_doesNotMatchChrome() {
        BrowserDescriptor chromeCustomTab = Browsers.Chrome.customTab("50");
        assertThat(VersionedBrowserMatcher.EDGE_CUSTOM_TAB.matches(chromeCustomTab)).isFalse();
    }

    @Test
    public void testEdgeBrowserMatcher_matchesEdgeStandalone() {
        BrowserDescriptor edgeStandalone = Browsers.Edge.standaloneBrowser("50");
        assertThat(VersionedBrowserMatcher.EDGE_BROWSER.matches(edgeStandalone)).isTrue();
    }

    @Test
    public void testEdgeBrowserMatcher_doesNotMatchEdgeCustomTab() {
        BrowserDescriptor edgeCustomTab = Browsers.Edge.customTab("50");
        assertThat(VersionedBrowserMatcher.EDGE_BROWSER.matches(edgeCustomTab)).isFalse();
    }

    @Test
    public void testEdgeBrowserMatcher_doesNotMatchDifferentPackage() {
        BrowserDescriptor chrome = Browsers.Chrome.standaloneBrowser("50");
        assertThat(VersionedBrowserMatcher.EDGE_BROWSER.matches(chrome)).isFalse();
    }

    @Test
    public void testEdgeBrowserMatcher_doesNotMatchDifferentSignature() {
        BrowserDescriptor fakeEdge = new BrowserDescriptor(
                Browsers.Edge.PACKAGE_NAME,
                Collections.singleton("FAKE_SIGNATURE_HASH"),
                "50",
                false);
        assertThat(VersionedBrowserMatcher.EDGE_BROWSER.matches(fakeEdge)).isFalse();
    }

    @Test
    public void testDenyList_excludeEdgeCustomTab() {
        BrowserDenyList denyList = new BrowserDenyList(
                VersionedBrowserMatcher.EDGE_CUSTOM_TAB);

        // Edge custom tab should be denied
        assertThat(denyList.matches(Browsers.Edge.customTab("50"))).isFalse();

        // Edge standalone should still be allowed
        assertThat(denyList.matches(Browsers.Edge.standaloneBrowser("50"))).isTrue();

        // Other browsers should not be affected
        assertThat(denyList.matches(Browsers.Chrome.customTab("50"))).isTrue();
        assertThat(denyList.matches(Browsers.Firefox.customTab("60"))).isTrue();
        assertThat(denyList.matches(Browsers.SBrowser.customTab("5"))).isTrue();
    }

    @Test
    public void testDenyList_excludeEdgeBoth() {
        BrowserDenyList denyList = new BrowserDenyList(
                VersionedBrowserMatcher.EDGE_CUSTOM_TAB,
                VersionedBrowserMatcher.EDGE_BROWSER);

        // Both Edge modes should be denied
        assertThat(denyList.matches(Browsers.Edge.customTab("50"))).isFalse();
        assertThat(denyList.matches(Browsers.Edge.standaloneBrowser("50"))).isFalse();

        // Other browsers should not be affected
        assertThat(denyList.matches(Browsers.Chrome.customTab("50"))).isTrue();
        assertThat(denyList.matches(Browsers.Chrome.standaloneBrowser("50"))).isTrue();
        assertThat(denyList.matches(Browsers.Firefox.standaloneBrowser("60"))).isTrue();
    }

    @Test
    public void testAllowList_onlyChromeAndFirefox() {
        // Simulates the recommended workaround: only allow Chrome and Firefox
        BrowserAllowList allowList = new BrowserAllowList(
                VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
                VersionedBrowserMatcher.FIREFOX_CUSTOM_TAB);

        assertThat(allowList.matches(Browsers.Chrome.customTab("50"))).isTrue();
        assertThat(allowList.matches(Browsers.Firefox.customTab("60"))).isTrue();
        assertThat(allowList.matches(Browsers.Edge.customTab("50"))).isFalse();
        assertThat(allowList.matches(Browsers.Edge.standaloneBrowser("50"))).isFalse();
    }
}
