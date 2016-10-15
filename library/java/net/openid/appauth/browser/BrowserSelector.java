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
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class to obtain the browser package name to be used for
 * {@link net.openid.appauth.AuthorizationService#performAuthorizationRequest(
 * net.openid.appauth.AuthorizationRequest,
 * android.app.PendingIntent)} calls. It prioritizes browsers which support
 * <a href="https://developer.chrome.com/multidevice/android/customtabs">custom tabs</a>. To
 * mitigate man-in-the-middle attacks by malicious apps pretending to be browsers for the
 * specific URI we query, only those which are registered as a handler for <em>all</em> HTTP and
 * HTTPS URIs will be used.
 */
public final class BrowserSelector {

    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";

    /**
     * The service we expect to find on a web browser that indicates it supports custom tabs.
     */
    @VisibleForTesting
    static final String ACTION_CUSTOM_TABS_CONNECTION =
            "android.support.customtabs.action.CustomTabsService";

    /**
     * An arbitrary (but unregistrable, per
     * <a href="https://www.iana.org/domains/reserved">IANA rules</a>) web intent used to query
     * for installed web browsers on the system.
     */
    @VisibleForTesting
    static final Intent BROWSER_INTENT = new Intent(
            Intent.ACTION_VIEW,
            Uri.parse("http://www.example.com"));

    /**
     * Searches through all apps that handle VIEW intents and have a warmup service. Picks
     * the one chosen by the user if this choice has been made, otherwise any browser with a warmup
     * service is returned. If no browser has a warmup service, the default browser will be
     * returned. If no default browser has been chosen, an arbitrary browser package is returned.
     *
     * <p>This is <strong>not</strong> threadsafe.
     *
     * @param context {@link Context} to use for accessing {@link PackageManager}.
     * @return The package name recommended to use for connecting to custom tabs related components.
     */
    @SuppressLint("PackageManagerGetSignatures")
    @Nullable
    public static BrowserDescriptor select(Context context, BrowserMatcher browserMatcher) {
        PackageManager pm = context.getPackageManager();

        // retrieve a list of all the matching handlers for the browser intent.
        // queryIntentActivities will ensure that these are priority ordered, with the default
        // (if set) as the first entry. Ignoring any matches which are not "full" browsers,
        // pick the first browser that matches against the provided browser matcher, preferring
        // use of custom tabs over standalone browsers if possible.
        List<BrowserDescriptor> descriptors = new ArrayList<>();
        List<ResolveInfo> resolvedActivityList =
                pm.queryIntentActivities(BROWSER_INTENT, PackageManager.GET_RESOLVED_FILTER);

        for (ResolveInfo info : resolvedActivityList) {
            // ignore handlers which are not browers
            if (!isFullBrowser(info)) {
                continue;
            }

            BrowserDescriptor descriptor;
            try {
                PackageInfo packageInfo = pm.getPackageInfo(
                        info.activityInfo.packageName,
                        PackageManager.GET_SIGNATURES);
                descriptor = new BrowserDescriptor(
                        packageInfo,
                        hasWarmupService(pm, info.activityInfo.packageName));
                descriptors.add(descriptor);
            } catch (NameNotFoundException e) {
                // a descriptor cannot be generated without the package info
                continue;
            }

            if (!descriptor.useCustomTab) {
                // skip this browser for now, we prefer browsers with custom tab support.
                // If no custom tab supporting browsers match, we will check this browser again
                // on the next pass.
                continue;
            }

            if (!browserMatcher.matches(descriptor)) {
                continue;
            }

            // this browser matches the specified constraints and supports custom tabs, so
            // we prefer it over any other options.
            return descriptor;
        }

        // No browsers have a warmup service, so re-evaluate all browsers in priority order for
        // use as a standalone browser.
        for (BrowserDescriptor descriptor : descriptors) {
            if (descriptor.useCustomTab) {
                // re-evaluate this browser for standalone use
                descriptor = descriptor.changeUseCustomTab(false);
            }
            if (browserMatcher.matches(descriptor)) {
                return descriptor;
            }
        }

        // nothing matched
        return null;
    }

    private static boolean hasWarmupService(PackageManager pm, String packageName) {
        Intent serviceIntent = new Intent();
        serviceIntent.setAction(ACTION_CUSTOM_TABS_CONNECTION);
        serviceIntent.setPackage(packageName);
        return (pm.resolveService(serviceIntent, 0) != null);
    }

    private static boolean isFullBrowser(ResolveInfo resolveInfo) {
        // The filter must match ACTION_VIEW, CATEGORY_BROWSEABLE, and at least one scheme,
        if (!resolveInfo.filter.hasAction(Intent.ACTION_VIEW)
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
