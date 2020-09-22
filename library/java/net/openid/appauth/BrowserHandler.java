package net.openid.appauth;

import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import net.openid.appauth.internal.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Marina Wageed on 22,September,2020
 * Trufla Technology,
 * Cairo, Egypt.
 */
class BrowserHandler
{

    /**
     * Wait for at most this amount of time for the browser connection to be established.
     */
    private static final long CLIENT_WAIT_TIME = 1L;

    @NonNull
    private final Context mContext;

    @NonNull
    private final String mBrowserPackage;

    @Nullable
    private final CustomTabsServiceConnection mConnection;

    @NonNull
    private final AtomicReference<CustomTabsClient> mClient;

    @NonNull
    private final CountDownLatch mClientLatch;

    BrowserHandler(@NonNull Context context) {
        mContext = context;
        mBrowserPackage = BrowserPackageHelper.getInstance().getPackageNameToUse(context);
        mClient = new AtomicReference<>();
        mClientLatch = new CountDownLatch(1);
        mConnection = bindCustomTabsService();
    }

    private CustomTabsServiceConnection bindCustomTabsService() {
        CustomTabsServiceConnection connection = new CustomTabsServiceConnection() {
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
            mBrowserPackage,
            connection)) {
            // this is expected if the browser does not support custom tabs
            Logger.info("Unable to bind custom tabs service");
            mClientLatch.countDown();
            return null;
        }

        return connection;
    }

    public CustomTabsIntent.Builder createCustomTabsIntentBuilder() {
        return new CustomTabsIntent.Builder(createSession());
    }

    public String getBrowserPackage() {
        return mBrowserPackage;
    }

    public void unbind() {
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
