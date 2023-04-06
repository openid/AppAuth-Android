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

package net.openid.appauth.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.browser.customtabs.CustomTabsService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class to obtain the browser package name to be used for
 * {@link net.openid.appauth.AuthorizationService#performAuthorizationRequest(
 * net.openid.appauth.AuthorizationRequest,
 * android.app.PendingIntent)} calls. It prioritizes browsers which support
 * [custom tabs](https://developer.chrome.com/multidevice/android/customtabs). To mitigate
 * man-in-the-middle attacks by malicious apps pretending to be browsers for the specific URI we
 * query, only those which are registered as a handler for _all_ HTTP and HTTPS URIs will be
 * used.
 */
public final class BrowserSelector {

    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";

    /**
     * The service we expect to find on a web browser that indicates it supports custom tabs.
     */
    @VisibleForTesting
    static final String ACTION_CUSTOM_TABS_CONNECTION =
            CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION;

    /**
     * Intent for querying installed web browsers as seen at
     * https://cs.android.com/android/platform/superproject/+/master:packages/modules/Permission/PermissionController/src/com/android/permissioncontroller/role/model/BrowserRoleBehavior.java
     */
    @VisibleForTesting
    static final Intent BROWSER_INTENT = new Intent()
            .setAction(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.fromParts("http", "", null));

    /**
     * Retrieves the full list of browsers installed on the device. Two entries will exist
     * for each browser that supports custom tabs, with the {@link BrowserDescriptor#useCustomTab}
     * flag set to `true` in one and `false` in the other. The list is in the
     * order returned by the package manager, so indirectly reflects the user's preferences
     * (i.e. their default browser, if set, should be the first entry in the list).
     */
    @SuppressLint("PackageManagerGetSignatures")
    @NonNull
    public static List<BrowserDescriptor> getAllBrowsers(Context context) {
        PackageManager pm = context.getPackageManager();
        List<BrowserDescriptor> browsers = new ArrayList<>();
        String defaultBrowserPackage = null;

        int queryFlag = PackageManager.GET_RESOLVED_FILTER;
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            queryFlag |= PackageManager.MATCH_ALL;
        }
        // When requesting all matching activities for an intent from the package manager,
        // the user's preferred browser is not guaranteed to be at the head of this list.
        // Therefore, the preferred browser must be separately determined and the resultant
        // list of browsers reordered to restored this desired property.
        ResolveInfo resolvedDefaultActivity =
                pm.resolveActivity(BROWSER_INTENT, 0);
        if (resolvedDefaultActivity != null) {
            defaultBrowserPackage = resolvedDefaultActivity.activityInfo.packageName;
        }
        List<ResolveInfo> resolvedActivityList =
                pm.queryIntentActivities(BROWSER_INTENT, queryFlag);

        for (ResolveInfo info : resolvedActivityList) {
            // ignore handlers which are not browsers
            if (!isFullBrowser(info)) {
                continue;
            }

            try {
                int defaultBrowserIndex = 0;
                PackageInfo packageInfo = pm.getPackageInfo(
                        info.activityInfo.packageName,
                        PackageManager.GET_SIGNATURES);

                if (hasWarmupService(pm, info.activityInfo.packageName)) {
                    BrowserDescriptor customTabBrowserDescriptor =
                            new BrowserDescriptor(packageInfo, true);
                    if (info.activityInfo.packageName.equals(defaultBrowserPackage)) {
                        // If the default browser is having a WarmupService,
                        // will it be added to the beginning of the list.
                        browsers.add(defaultBrowserIndex, customTabBrowserDescriptor);
                        defaultBrowserIndex++;
                    } else {
                        browsers.add(customTabBrowserDescriptor);
                    }
                }

                BrowserDescriptor fullBrowserDescriptor =
                        new BrowserDescriptor(packageInfo, false);
                if (info.activityInfo.packageName.equals(defaultBrowserPackage)) {
                    // The default browser is added to the beginning of the list.
                    // If there is support for Custom Tabs, will the one disabling Custom Tabs
                    // be added as the second entry.
                    browsers.add(defaultBrowserIndex, fullBrowserDescriptor);
                } else {
                    browsers.add(fullBrowserDescriptor);
                }
            } catch (NameNotFoundException e) {
                // a descriptor cannot be generated without the package info
            }
        }

        return browsers;
    }

    /**
     * Searches through all browsers for the best match based on the supplied browser matcher.
     * Custom tab supporting browsers are preferred, if the matcher permits them, and browsers
     * are evaluated in the order returned by the package manager, which should indirectly match
     * the user's preferences.
     *
     * @param context {@link Context} to use for accessing {@link PackageManager}.
     * @return The package name recommended to use for connecting to custom tabs related components.
     */
    @SuppressLint("PackageManagerGetSignatures")
    @Nullable
    public static BrowserDescriptor select(Context context, BrowserMatcher browserMatcher) {
        List<BrowserDescriptor> allBrowsers = getAllBrowsers(context);
        BrowserDescriptor bestMatch = null;
        for (BrowserDescriptor browser : allBrowsers) {
            if (!browserMatcher.matches(browser)) {
                continue;
            }

            if (browser.useCustomTab) {
                // directly return the first custom tab supporting browser that is matched
                return browser;
            }

            if (bestMatch == null) {
                // store this as the best match for use if we don't find any matching
                // custom tab supporting browsers
                bestMatch = browser;
            }
        }

        return bestMatch;
    }

    private static boolean hasWarmupService(PackageManager pm, String packageName) {
        Intent serviceIntent = new Intent();
        serviceIntent.setAction(ACTION_CUSTOM_TABS_CONNECTION);
        serviceIntent.setPackage(packageName);
        return (pm.resolveService(serviceIntent, 0) != null);
    }

    private static boolean isFullBrowser(ResolveInfo resolveInfo) {
        // The filter must match ACTION_VIEW, CATEGORY_BROWSEABLE, and at least one scheme,
        if (resolveInfo.filter == null
                || !resolveInfo.filter.hasAction(Intent.ACTION_VIEW)
                || !resolveInfo.filter.hasCategory(Intent.CATEGORY_BROWSABLE)
                || resolveInfo.filter.schemesIterator() == null) {
            return false;
        }

        // The filter must not be restricted to any particular set of authorities
        if (resolveInfo.filter.authoritiesIterator() != null) {
            return false;
        }

        // The filter must support both HTTP and HTTPS.
        boolean supportsHttp = false;
        boolean supportsHttps = false;
        Iterator<String> schemeIter = resolveInfo.filter.schemesIterator();
        while (schemeIter.hasNext()) {
            String scheme = schemeIter.next();
            supportsHttp |= SCHEME_HTTP.equals(scheme);
            supportsHttps |= SCHEME_HTTPS.equals(scheme);

            if (supportsHttp && supportsHttps) {
                return true;
            }
        }

        // at least one of HTTP or HTTPS is not supported
        return false;
    }
}
