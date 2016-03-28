/*
 * Copyright 2015 The AppAuth for Android Authors. All Rights Reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowIntentFilterFixed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, shadows = ShadowIntentFilterFixed.class)
public class BrowserPackageHelperTest {

    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";

    private static final ResolveInfo CHROME =
            new ResolveInfoBuilder("com.android.chrome")
                    .withBrowserDefaults()
                    .build();

    private static final ResolveInfo FIREFOX =
            new ResolveInfoBuilder("org.mozilla.firefox")
                    .withBrowserDefaults()
                    .build();

    private static final ResolveInfo DOLPHIN =
            new ResolveInfoBuilder("mobi.mgeek.TunnyBrowser")
                    .withBrowserDefaults()
                    .build();

    private static final ResolveInfo[] NO_BROWSERS = new ResolveInfo[0];

    @Mock Context mContext;
    @Mock PackageManager mPackageManager;

    BrowserPackageHelper mHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mHelper = BrowserPackageHelper.getInstance();
    }

    @After
    public void tearDown() {
        BrowserPackageHelper.clearInstance();
    }

    @Test
    public void testGetPackageNameToUse_warmUpSupportOnFirstMatch() {
        setBrowserList(CHROME, FIREFOX, DOLPHIN);
        setBrowsersWithWarmupSupport(CHROME, FIREFOX);
        checkPackageNameToUse(CHROME);
    }

    @Test
    public void testGetPackageNameToUse_warmUpSupportOnAlternateBrowser() {
        setBrowserList(DOLPHIN, FIREFOX);
        setBrowsersWithWarmupSupport(FIREFOX);
        checkPackageNameToUse(FIREFOX);
    }

    @Test
    public void testGetPackageNameToUse_warmUpSupportOnAlternateBrowsers() {
        setBrowserList(DOLPHIN, CHROME, FIREFOX);
        setBrowsersWithWarmupSupport(CHROME, FIREFOX);
        // first in priority list always wins
        checkPackageNameToUse(CHROME);
    }

    @Test
    public void testGetPackageNameToUse_noWarmUpSupportOnAnyBrowser() {
        setBrowserList(CHROME, DOLPHIN);
        setBrowsersWithWarmupSupport(NO_BROWSERS);
        checkPackageNameToUse(CHROME);
    }

    @Test
    public void testGetPackageNameToUse_noBrowsers() {
        setBrowserList(NO_BROWSERS);
        setBrowsersWithWarmupSupport(NO_BROWSERS);
        assertNull(mHelper.getPackageNameToUse(mContext));
    }

    @Test
    public void testGetPackageNameToUse_ignoreAuthorityRestrictedBrowsers() {
        ResolveInfo authorityRestrictedBrowser =
                new ResolveInfoBuilder("com.badguy.proxy")
                        .withBrowserDefaults()
                        .addAuthority("www.example.com")
                        .build();
        setBrowserList(authorityRestrictedBrowser, CHROME);
        setBrowsersWithWarmupSupport(authorityRestrictedBrowser, CHROME);
        checkPackageNameToUse(CHROME);
    }

    @Test
    public void testGetPackageNameToUse_ignoreBrowsersWithoutBrowseableCategory() {
        ResolveInfo misconfiguredBrowser =
                new ResolveInfoBuilder("com.broken.browser")
                        .addAction(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_DEFAULT)
                        .addScheme(SCHEME_HTTP)
                        .addScheme(SCHEME_HTTPS)
                        .build();
        setBrowserList(misconfiguredBrowser, CHROME);
        setBrowsersWithWarmupSupport(misconfiguredBrowser, CHROME);
        checkPackageNameToUse(CHROME);
    }

    @Test
    public void testGetPackageNameToUse_ignoreBrowsersWithoutHttpsSupport() {
        ResolveInfo noHttpsBrowser =
                new ResolveInfoBuilder("com.broken.browser")
                        .addAction(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .addScheme(SCHEME_HTTP)
                        .build();
        setBrowserList(DOLPHIN, noHttpsBrowser);
        setBrowsersWithWarmupSupport(noHttpsBrowser);
        checkPackageNameToUse(DOLPHIN);
    }

    /**
     * Browsers are expected to be in priority order, such that the default would be first.
     */
    private void setBrowserList(ResolveInfo... browsers) {
        if (browsers == null) {
            return;
        }
        when(mPackageManager.queryIntentActivities(
                BrowserPackageHelper.BROWSER_INTENT,
                PackageManager.GET_RESOLVED_FILTER))
                .thenReturn(Arrays.asList(browsers));
    }

    private void setBrowsersWithWarmupSupport(ResolveInfo... browsers) {
        if (browsers == null) {
            return;
        }
        for (ResolveInfo browser : browsers) {
            when(mPackageManager.resolveService(
                    serviceIntentEq(browser.activityInfo.packageName),
                    eq(0)))
                    .thenReturn(browser);
        }
    }

    private void checkPackageNameToUse(ResolveInfo expected) {
        String result = mHelper.getPackageNameToUse(mContext);
        assertEquals("returned package does not match expected package",
                expected.activityInfo.packageName, result);
    }

    private static class ResolveInfoBuilder {
        private final String mPackageName;
        private final List<String> mActions = new ArrayList<>();
        private final List<String> mCategories = new ArrayList<>();
        private final List<String> mSchemes = new ArrayList<>();
        private final List<String> mAuthorities = new ArrayList<>();

        ResolveInfoBuilder(String packageName) {
            mPackageName = packageName;
        }

        public ResolveInfoBuilder withBrowserDefaults() {
            return addAction(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .addScheme(SCHEME_HTTP)
                    .addScheme(SCHEME_HTTPS);
        }

        public ResolveInfoBuilder addAction(String action) {
            mActions.add(action);
            return this;
        }

        public ResolveInfoBuilder addCategory(String category) {
            mCategories.add(category);
            return this;
        }

        public ResolveInfoBuilder addScheme(String scheme) {
            mSchemes.add(scheme);
            return this;
        }

        public ResolveInfoBuilder addAuthority(String authority) {
            mAuthorities.add(authority);
            return this;
        }

        public ResolveInfo build() {
            ResolveInfo ri = new ResolveInfo();
            ri.activityInfo = new ActivityInfo();
            ri.activityInfo.packageName = mPackageName;
            ri.filter = new IntentFilter();

            for (String action : mActions) {
                ri.filter.addAction(action);
            }

            for (String category : mCategories) {
                ri.filter.addCategory(category);
            }

            for (String scheme : mSchemes) {
                ri.filter.addDataScheme(scheme);
            }

            for (String authority: mAuthorities) {
                ri.filter.addDataAuthority(authority, null);
            }

            return ri;
        }
    }

    /**
     * Custom matcher for verifying the intent fired during token request.
     */
    private static class ServiceIntentMatcher extends ArgumentMatcher<Intent> {

        private String mPackage;

        ServiceIntentMatcher(String pkg) {
            mPackage = pkg;
        }

        @Override
        public boolean matches(Object actual) {
            Intent intent = (Intent) actual;
            return (intent != null)
                    && (BrowserPackageHelper.ACTION_CUSTOM_TABS_CONNECTION.equals(
                            intent.getAction()))
                    && (TextUtils.equals(mPackage, intent.getPackage()));
        }
    }

    private static Intent serviceIntentEq(String pkg) {
        return argThat(new ServiceIntentMatcher(pkg));
    }
}
