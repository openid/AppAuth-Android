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

/**
 * Matches only the specified browser.
 */
public class ExactBrowserMatcher implements BrowserMatcher {

    private BrowserDescriptor mBrowser;

    /**
     * Creates a browser matcher that will only match the specified browser.
     */
    public ExactBrowserMatcher(BrowserDescriptor browser) {
        mBrowser = browser;
    }

    @Override
    public boolean matches(@NonNull BrowserDescriptor descriptor) {
        return mBrowser.equals(descriptor);
    }
}
