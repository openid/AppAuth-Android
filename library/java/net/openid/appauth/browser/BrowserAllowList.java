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

import java.util.Arrays;
import java.util.List;

/**
 * A allowList of browsers which can be used as part of an authorization flows. Examples:
 *
 * ```java
 * // only allow Chrome, and only as a standalone browser
 * new BrowserAllowList(VersionedBrowserMatcher.CHROME_BROWSER);
 *
 * // allow Chrome custom tabs only, but exclude a version range
 * new BrowserAllowList(
 *     new VersionedBrowserMatcher(
 *         Browsers.Chrome.PACKAGE_NAME,
 *         Browsers.Chrome.SIGNATURE_SET,
 *         true,
 *         VersionRange.atMost("45.1")),
 *     new VersionedBrowserMatcher(
 *         Browsers.Chrome.PACKAGE_NAME,
 *         Browsers.Chrome.SIGNATURE_SET,
 *         true,
 *         VersionRange.atLeast("45.3"));
 * ```
 */
public class BrowserAllowList implements BrowserMatcher {

    private List<BrowserMatcher> mBrowserMatchers;

    /**
     * Creates a browser allowList, which will match if any of the provided matchers do.
     */
    public BrowserAllowList(BrowserMatcher... matchers) {
        mBrowserMatchers = Arrays.asList(matchers);
    }

    @Override
    public boolean matches(@NonNull BrowserDescriptor descriptor) {
        for (BrowserMatcher matcher : mBrowserMatchers) {
            if (matcher.matches(descriptor)) {
                return true;
            }
        }

        return false;
    }
}
