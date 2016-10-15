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

import android.content.ComponentName;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hides the details of establishing connections and sessions with custom tabs, to make testing
 * easier.
 */
class CustomTabManager {

    /**
     * Wait for at most this amount of time for the browser connection to be established.
     */
    private static final long CLIENT_WAIT_TIME = 1L;

    @NonNull
    private final Context mContext;

    @NonNull
    private final AtomicReference<CustomTabsClient> mClient;

    @NonNull
    private final CountDownLatch mClientLatch;

    @Nullable
    private CustomTabsServiceConnection mConnection;

    CustomTabManager(@NonNull Context context) {
        mContext = context;
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

        if (!CustomTabsClient.bindCustomTabsService(
                mContext,
                browserPackage,
                mConnection)) {
            // this is expected if the browser does not support custom tabs
            Logger.info("Unable to bind custom tabs service");
            mClientLatch.countDown();
        }
    }

    public CustomTabsIntent.Builder createCustomTabsIntentBuilder() {
        return new CustomTabsIntent.Builder(createSession());
    }

    public synchronized void unbind() {
        if (mConnection == null) {
            return;
        }

        mContext.unbindService(mConnection);
        mClient.set(null);
        Logger.debug("CustomTabsService is disconnected");
    }

    private CustomTabsSession createSession() {
        try {
            mClientLatch.await(CLIENT_WAIT_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.info("Interrupted while waiting for browser connection");
            mClientLatch.countDown();
        }

        CustomTabsClient client = mClient.get();
        if (client != null) {
            return client.newSession(null);
        }

        return null;
    }
}
