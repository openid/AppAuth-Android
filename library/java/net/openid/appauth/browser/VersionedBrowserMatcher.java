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
 * Matches a browser based on its package name, set of signatures, version and whether it is
 * being used as a custom tab. This can be used as part of a browser allowList or denyList.
 */
public class VersionedBrowserMatcher implements BrowserMatcher {

    /**
     * Matches any version of Chrome for use as a custom tab.
     */
    public static final VersionedBrowserMatcher CHROME_CUSTOM_TAB = new VersionedBrowserMatcher(
            Browsers.Chrome.PACKAGE_NAME,
            Browsers.Chrome.SIGNATURE_SET,
            true,
            VersionRange.atLeast(Browsers.Chrome.MINIMUM_VERSION_FOR_CUSTOM_TAB));

    /**
     * Matches any version of Google Chrome for use as a standalone browser.
     */
    public static final VersionedBrowserMatcher CHROME_BROWSER = new VersionedBrowserMatcher(
            Browsers.Chrome.PACKAGE_NAME,
            Browsers.Chrome.SIGNATURE_SET,
            false,
            VersionRange.ANY_VERSION);

    /**
     * Matches any version of Firefox for use as a custom tab.
     */
    public static final VersionedBrowserMatcher FIREFOX_CUSTOM_TAB = new VersionedBrowserMatcher(
            Browsers.Firefox.PACKAGE_NAME,
            Browsers.Firefox.SIGNATURE_SET,
            true,
            VersionRange.atLeast(Browsers.Firefox.MINIMUM_VERSION_FOR_CUSTOM_TAB));

    /**
     * Matches any version of Mozilla Firefox.
     */
    public static final VersionedBrowserMatcher FIREFOX_BROWSER = new VersionedBrowserMatcher(
            Browsers.Firefox.PACKAGE_NAME,
            Browsers.Firefox.SIGNATURE_SET,
            false,
            VersionRange.ANY_VERSION);

    /**
     * Matches any version of SBrowser for use as a standalone browser.
     */
    public static final VersionedBrowserMatcher SAMSUNG_BROWSER = new VersionedBrowserMatcher(
            Browsers.SBrowser.PACKAGE_NAME,
            Browsers.SBrowser.SIGNATURE_SET,
            false,
            VersionRange.ANY_VERSION);

    /**
     * Matches any version of SBrowser for use as a custom tab.
     */
    public static final VersionedBrowserMatcher SAMSUNG_CUSTOM_TAB = new VersionedBrowserMatcher(
            Browsers.SBrowser.PACKAGE_NAME,
            Browsers.SBrowser.SIGNATURE_SET,
            true,
            VersionRange.atLeast(Browsers.SBrowser.MINIMUM_VERSION_FOR_CUSTOM_TAB));

    private String mPackageName;
    private Set<String> mSignatureHashes;
    private VersionRange mVersionRange;
    private boolean mUsingCustomTab;

    /**
     * Creates a browser matcher that requires an exact match on package name, single signature
     * hash, custom tab usage mode, and a version range.
     */
    public VersionedBrowserMatcher(
            @NonNull String packageName,
            @NonNull String signatureHash,
            boolean usingCustomTab,
            @NonNull VersionRange versionRange) {
        this(packageName,
                Collections.singleton(signatureHash),
                usingCustomTab,
                versionRange);
    }

    /**
     * Creates a browser matcher that requires an exact match on package name, set of signature
     * hashes, custom tab usage mode, and a version range.
     */
    public VersionedBrowserMatcher(
            @NonNull String packageName,
            @NonNull Set<String> signatureHashes,
            boolean usingCustomTab,
            @NonNull VersionRange versionRange) {
        mPackageName = packageName;
        mSignatureHashes = signatureHashes;
        mUsingCustomTab = usingCustomTab;
        mVersionRange = versionRange;
    }

    @Override
    public boolean matches(@NonNull BrowserDescriptor descriptor) {
        return mPackageName.equals(descriptor.packageName)
                && mUsingCustomTab == descriptor.useCustomTab
                && mVersionRange.matches(descriptor.version)
                && mSignatureHashes.equals(descriptor.signatureHashes);
    }
}
