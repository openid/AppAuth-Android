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

package net.openid.appauth;

import android.support.annotation.NonNull;

import net.openid.appauth.browser.AnyBrowserMatcher;
import net.openid.appauth.browser.BrowserMatcher;

/**
 * Defines configuration properties that control the behavior of the AppAuth library, independent
 * of the OAuth2 specific details that are described.
 */
public class AppAuthConfiguration {

    /**
     * The default configuration that is used if no configuration is explicitly specified
     * when constructing an {@link AuthorizationService}.
     */
    public static final AppAuthConfiguration DEFAULT =
            new AppAuthConfiguration.Builder().build();

    @NonNull
    private final BrowserMatcher mBrowserMatcher;

    private AppAuthConfiguration(
            @NonNull BrowserMatcher browserMatcher) {
        mBrowserMatcher = browserMatcher;
    }

    /**
     * Controls which browsers can be used for the authorization flow.
     */
    @NonNull
    public BrowserMatcher getBrowserMatcher() {
        return mBrowserMatcher;
    }

    /**
     * Creates {@link AppAuthConfiguration} instances.
     */
    public static class Builder {

        private BrowserMatcher mBrowserMatcher = AnyBrowserMatcher.INSTANCE;

        /**
         * Specify the browser matcher to use, which controls the browsers that can be used
         * for authorization.
         */
        public Builder setBrowserMatcher(@NonNull BrowserMatcher browserMatcher) {
            Preconditions.checkNotNull(browserMatcher, "browserMatcher cannot be null");
            mBrowserMatcher = browserMatcher;
            return this;
        }

        /**
         * Creates the instance from the configured properties.
         */
        public AppAuthConfiguration build() {
            return new AppAuthConfiguration(mBrowserMatcher);
        }
    }
}
