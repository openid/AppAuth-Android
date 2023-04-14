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

import com.google.common.collect.Sets;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class BrowserDescriptorTest {

    @Test
    public void testEquals_toNull() {
        assertThat(Browsers.Chrome.standaloneBrowser("45")).isNotEqualTo(null);
    }

    @Test
    public void testEquals_toSelf() {
        BrowserDescriptor browser = Browsers.Chrome.standaloneBrowser("45");
        assertThat(browser).isEqualTo(browser);
    }

    @Test
    public void testEquals_toEquivalent() {
        BrowserDescriptor a = Browsers.Chrome.standaloneBrowser("45");
        BrowserDescriptor b = Browsers.Chrome.standaloneBrowser("45");
        assertThat(a).isEqualTo(b);
    }

    @Test
    public void testEquals_differentVersion() {
        BrowserDescriptor a = Browsers.Chrome.standaloneBrowser("45");
        BrowserDescriptor b = Browsers.Chrome.standaloneBrowser("46");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    public void testEquals_differentCustomTabSetting() {
        BrowserDescriptor a = Browsers.Chrome.standaloneBrowser("45");
        BrowserDescriptor b = Browsers.Chrome.customTab("45");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    public void testEquals_differentSignatures() {
        BrowserDescriptor a = Browsers.Chrome.standaloneBrowser("45");
        @SuppressWarnings("unchecked")
        BrowserDescriptor b = new BrowserDescriptor(
                a.packageName,
                Sets.newHashSet("DIFFERENT_SIGNATURE"),
                a.version,
                a.useCustomTab);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    public void testEquals_differentPackageNames() {
        BrowserDescriptor a = Browsers.Chrome.standaloneBrowser("45");
        BrowserDescriptor b = new BrowserDescriptor(
                Browsers.Firefox.PACKAGE_NAME,
                a.signatureHashes,
                a.version,
                a.useCustomTab);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    public void testHashCode_equivalent() {
        BrowserDescriptor a = Browsers.Chrome.standaloneBrowser("45");
        BrowserDescriptor b = Browsers.Chrome.standaloneBrowser("45");

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    public void testHashCode_notEquivalent() {
        BrowserDescriptor a = Browsers.Chrome.standaloneBrowser("45");
        BrowserDescriptor b = Browsers.Chrome.standaloneBrowser("46");

        assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
    }
}
