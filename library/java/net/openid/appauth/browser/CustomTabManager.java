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

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import net.openid.appauth.internal.Logger;
import net.openid.appauth.internal.UriUtil;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hides the details of establishing connections and sessions with custom tabs, to make testing
 * easier.
 */
public class CustomTabManager {

    /**
     * Wait for at most this amount of time for the browser connection to be established.
     */
    private static final long CLIENT_WAIT_TIME = 1L;

    @NonNull
    private final WeakReference<Context> mContextRef;

    @NonNull
    private final AtomicReference<CustomTabsClient> mClient;

    @NonNull
    private final CountDownLatch mClientLatch;

    @Nullable
    private CustomTabsServiceConnection mConnection;

    public CustomTabManager(@NonNull Context context) {
        mContextRef = new WeakReference<>(context);
        mClient = new AtomicReference<>();
        mClientLatch = new CountDownLatch(1);
    }

    public synchronized void bind(@NonNull String browserPackage) {
        if (mConnection != null) {
            return;
        }

        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Logger.debug("CustomTabsService is disconnected");
                setClient(null);
            }

            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName,
                                                     CustomTabsClient customTabsClient) {
                Logger.debug("CustomTabsService is connected");
                customTabsClient.warmup(0);
                setClient(customTabsClient);
            }

            private void setClient(@Nullable CustomTabsClient client) {
                mClient.set(client);
                mClientLatch.countDown();
            }
        };

        Context context = mContextRef.get();
        if (context == null || !CustomTabsClient.bindCustomTabsService(
                context,
                browserPackage,
                mConnection)) {
            // this is expected if the browser does not support custom tabs
            Logger.info("Unable to bind custom tabs service");
            mClientLatch.countDown();
        }
    }

    /**
     * Creates a {@link androidx.browser.customtabs.CustomTabsIntent.Builder custom tab builder},
     * with an optional list of optional URIs that may be requested. The URI list
     * should be ordered such that the most likely URI to be requested is first. If the selected
     * browser does not support custom tabs, then the URI list has no effect.
     */
    @WorkerThread
    @NonNull
    public CustomTabsIntent.Builder createTabBuilder(@Nullable Uri... possibleUris) {
        return new CustomTabsIntent.Builder(createSession(null, possibleUris));
    }

    public synchronized void dispose() {
        if (mConnection == null) {
            return;
        }

        Context context = mContextRef.get();
        if (context != null) {
            context.unbindService(mConnection);
        }

        mClient.set(null);
        Logger.debug("CustomTabsService is disconnected");
    }

    /**
     * Creates a {@link androidx.browser.customtabs.CustomTabsSession custom tab session} for
     * use with a custom tab intent, with optional callbacks and optional list of URIs that may
     * be requested. The URI list should be ordered such that the most likely URI to be requested
     * is first. If no custom tab supporting browser is available, this will return {@code null}.
     */
    @WorkerThread
    @Nullable
    public CustomTabsSession createSession(
            @Nullable CustomTabsCallback callbacks,
            @Nullable Uri... possibleUris) {
        CustomTabsClient client = getClient();
        if (client == null) {
            return null;
        }

        CustomTabsSession session = client.newSession(callbacks);
        if (session == null) {
            Logger.warn("Failed to create custom tabs session through custom tabs client");
            return null;
        }

        if (possibleUris != null && possibleUris.length > 0) {
            List<Bundle> additionalUris = UriUtil.toCustomTabUriBundle(possibleUris, 1);
            session.mayLaunchUrl(possibleUris[0], null, additionalUris);
        }

        return session;
    }

    /**
     * Retrieve the custom tab client used to communicate with the custom tab supporting browser,
     * if available.
     */
    @WorkerThread
    public CustomTabsClient getClient() {
        try {
            mClientLatch.await(CLIENT_WAIT_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.info("Interrupted while waiting for browser connection");
            mClientLatch.countDown();
        }

        return mClient.get();
    }
}
