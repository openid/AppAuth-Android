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

import net.openid.appauth.BuildConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class VersionedBrowserMatcherTest {

    private VersionedBrowserMatcher mBrowserMatcher;

    @Before
    public void setUp() {
        mBrowserMatcher = new VersionedBrowserMatcher(
                Browsers.Chrome.PACKAGE_NAME,
                Browsers.Chrome.SIGNATURE_SET,
                false,
                VersionRange.between("1.2.3", "2"));
    }

    @Test
    public void testMatches() {
        assertThat(mBrowserMatcher.matches(new BrowserDescriptor(
                Browsers.Chrome.PACKAGE_NAME,
                Browsers.Chrome.SIGNATURE_SET,
                "1.5.0",
                false
        ))).isTrue();

        assertThat(mBrowserMatcher.matches(new BrowserDescriptor(
                Browsers.Chrome.PACKAGE_NAME,
                Browsers.Chrome.SIGNATURE_SET,
                "1.2.3",
                false
        ))).isTrue();

        assertThat(mBrowserMatcher.matches(new BrowserDescriptor(
                Browsers.Chrome.PACKAGE_NAME,
                Browsers.Chrome.SIGNATURE_SET,
                "2.0",
                false
        ))).isTrue();
    }

    @Test
    public void testMatches_differentPackageName() {
        assertThat(mBrowserMatcher.matches(new BrowserDescriptor(
                "com.android.not_chrome",
                Browsers.Chrome.SIGNATURE_SET,
                "1.5.0",
                false
        ))).isFalse();
    }

    @Test
    public void testMatches_differentSignature() {
        assertThat(mBrowserMatcher.matches(new BrowserDescriptor(
                Browsers.Chrome.PACKAGE_NAME,
                Collections.singleton("DIFFERENT_HASH"),
                "1.5.0",
                false
        ))).isFalse();
    }

    @Test
    public void testMatches_additionalSignatures() {
        HashSet<String> signatureHashes = new HashSet<>(Browsers.Chrome.SIGNATURE_SET);
        signatureHashes.add("ANOTHER_SIGNATURE");
        assertThat(mBrowserMatcher.matches(new BrowserDescriptor(
                Browsers.Chrome.PACKAGE_NAME,
                signatureHashes,
                "1.5.0",
                false
        ))).isFalse();
    }

    @Test
    public void testMatches_differentCustomTabMode() {
        assertThat(mBrowserMatcher.matches(new BrowserDescriptor(
                Browsers.Chrome.PACKAGE_NAME,
                Browsers.Chrome.SIGNATURE_SET,
                "1.5.0",
                true
        ))).isFalse();
    }

    @Test
    public void testMatches_belowVersionRange() {
        assertThat(mBrowserMatcher.matches(new BrowserDescriptor(
                Browsers.Chrome.PACKAGE_NAME,
                Browsers.Chrome.SIGNATURE_SET,
                "1.2.2",
                false
        ))).isFalse();
    }

    @Test
    public void testMatches_aboveVersionRange() {
        assertThat(mBrowserMatcher.matches(new BrowserDescriptor(
                Browsers.Chrome.PACKAGE_NAME,
                Browsers.Chrome.SIGNATURE_SET,
                "2.0.1",
                false
        ))).isFalse();
    }
}
