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

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Set;

/**
 * Defines the attributes of some commonly-used browsers on Android, for use in browser matchers.
 */
public final class Browsers {

    /**
     * Constants related to Google Chrome.
     */
    public static final class Chrome {

        /**
         * The package name for Chrome.
         */
        public static final String PACKAGE_NAME = "com.android.chrome";

        /**
         * The SHA-512 hash (Base64 url-safe encoded) of the public key for Chrome.
         */
        public static final String SIGNATURE =
                "7fmduHKTdHHrlMvldlEqAIlSfii1tl35bxj1OXN5Ve8c4lU6URVu4xtSHc3BVZxS"
                        + "6WWJnxMDhIfQN0N0K2NDJg==";

        /**
         * The set of signature hashes for Chrome.
         */
        public static final Set<String> SIGNATURE_SET =
                Collections.singleton(SIGNATURE);

        /**
         * The version in which Custom Tabs were introduced in Chrome.
         */
        public static final DelimitedVersion MINIMUM_VERSION_FOR_CUSTOM_TAB =
                DelimitedVersion.parse("45");

        /**
         * Creates a browser descriptor for the specified version of Chrome, when used as a
         * standalone browser.
         */
        public static BrowserDescriptor standaloneBrowser(@NonNull String version) {
            return new BrowserDescriptor(PACKAGE_NAME, SIGNATURE_SET, version, false);
        }

        /**
         * Creates a browser descriptor for the specified version of Chrome, when used as
         * a custom tab.
         */
        public static BrowserDescriptor customTab(@NonNull String version) {
            return new BrowserDescriptor(PACKAGE_NAME, SIGNATURE_SET, version, true);
        }

        private Chrome() {
            // no need to construct this class
        }
    }

    /**
     * Constants related to Mozilla Firefox.
     */
    public static final class Firefox {

        /**
         * The package name for Firefox.
         */
        public static final String PACKAGE_NAME = "org.mozilla.firefox";

        /**
         * The SHA-512 hash (Base64 url-safe encoded) of the public key for Firefox.
         */
        public static final String SIGNATURE_HASH =
                "2gCe6pR_AO_Q2Vu8Iep-4AsiKNnUHQxu0FaDHO_qa178GByKybdT_BuE8_dYk99G"
                        + "5Uvx_gdONXAOO2EaXidpVQ==";

        /**
         * The set of signature hashes for Firefox.
         */
        public static final Set<String> SIGNATURE_SET =
                Collections.singleton(SIGNATURE_HASH);

        /**
         * The version in which Custom Tabs were introduced in Firefox.
         */
        public static final DelimitedVersion MINIMUM_VERSION_FOR_CUSTOM_TAB =
                DelimitedVersion.parse("57");

        /**
         * Creates a browser descriptor for the specified version of Firefox, when used
         * as a standalone browser.
         */
        public static BrowserDescriptor standaloneBrowser(@NonNull String version) {
            return new BrowserDescriptor(PACKAGE_NAME, SIGNATURE_SET, version, false);
        }

        /**
         * Creates a browser descriptor for the specified version of Firefox, when used as
         * a custom tab.
         */
        public static BrowserDescriptor customTab(@NonNull String version) {
            return new BrowserDescriptor(PACKAGE_NAME, SIGNATURE_SET, version, true);
        }

        private Firefox() {
            // no need to construct this class
        }
    }

    /**
     * Constants related to
     * [SBrowser](https://play.google.com/store/apps/details?id=com.sec.android.app.sbrowser),
     * the default browser on Samsung devices.
     */
    public static final class SBrowser {

        /**
         * The package name for SBrowser.
         */
        public static final String PACKAGE_NAME = "com.sec.android.app.sbrowser";

        /**
         * The SHA-512 hash (Base64 url-safe encoded) of the public key for SBrowser.
         */
        public static final String SIGNATURE_HASH =
                "ABi2fbt8vkzj7SJ8aD5jc4xJFTDFntdkMrYXL3itsvqY1QIw-dZozdop5rgKNxjb"
                        + "rQAd5nntAGpgh9w84O1Xgg==";

        /**
         * The set of signature hashes for SBrowser.
         */
        public static final Set<String> SIGNATURE_SET =
                Collections.singleton(SIGNATURE_HASH);

        /**
         * The version in which Custom Tabs were introduced in Samsung Internet.
         */
        public static final DelimitedVersion MINIMUM_VERSION_FOR_CUSTOM_TAB =
                DelimitedVersion.parse("4.0");

        /**
         * Creates a browser descriptor for the specified version of SBrowser, when
         * used as a standalone browser.
         */
        public static BrowserDescriptor standaloneBrowser(@NonNull String version) {
            return new BrowserDescriptor(PACKAGE_NAME, SIGNATURE_SET, version, false);
        }

        /**
         * Creates a browser descriptor for the specified version of SBrowser, when
         * used as a custom tab.
         */
        public static BrowserDescriptor customTab(@NonNull String version) {
            return new BrowserDescriptor(PACKAGE_NAME, SIGNATURE_SET, version, true);
        }

        private SBrowser() {
            // no need to construct this class
        }
    }

    private Browsers() {
        // no need to construct this class
    }
}
