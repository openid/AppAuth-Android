package net.openid.appauth;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.browser.customtabs.CustomTabsIntent;

import net.openid.appauth.internal.Logger;

import static net.openid.appauth.Preconditions.checkNotNull;

/**
 * Created by Marina Wageed on 22,September,2020
 * Trufla Technology,
 * Cairo, Egypt.
 */

public class LogoutService
{
    @VisibleForTesting
    Context mContext;

    @NonNull
    private final BrowserHandler mBrowserHandler;

    private boolean mDisposed = false;

    public LogoutService(@NonNull Context context) {
        this(context, new BrowserHandler(context));
    }

    @VisibleForTesting
    LogoutService(@NonNull Context context,
                  @NonNull BrowserHandler browserHandler) {
        mContext = checkNotNull(context);
        mBrowserHandler = checkNotNull(browserHandler);
    }

    public CustomTabsIntent.Builder createCustomTabsIntentBuilder() {
        checkNotDisposed();
        return mBrowserHandler.createCustomTabsIntentBuilder();
    }

    public void performLogoutRequest(
        @NonNull LogoutRequest request,
        @NonNull PendingIntent resultHandlerIntent) {
        performLogoutRequest(request,
            resultHandlerIntent,
            createCustomTabsIntentBuilder().build());
    }

    public void performLogoutRequest(
        @NonNull LogoutRequest request,
        @NonNull PendingIntent resultHandlerIntent,
        @NonNull CustomTabsIntent customTabsIntent) {
        checkNotDisposed();
        Uri requestUri = request.toUri();
        PendingLogoutIntentStore.getInstance().addPendingIntent(request, resultHandlerIntent);
        Intent intent = customTabsIntent.intent;
        intent.setData(requestUri);
        if (TextUtils.isEmpty(intent.getPackage())) {
            intent.setPackage(mBrowserHandler.getBrowserPackage());
        }

        Logger.debug("Using %s as browser for auth", intent.getPackage());
        intent.putExtra(CustomTabsIntent.EXTRA_TITLE_VISIBILITY_STATE, CustomTabsIntent.NO_TITLE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        mContext.startActivity(intent);
    }

    public void dispose() {
        if (mDisposed) {
            return;
        }
        mBrowserHandler.unbind();
        mDisposed = true;
    }

    private void checkNotDisposed() {
        if (mDisposed) {
            throw new IllegalStateException("Service has been disposed and rendered inoperable");
        }
    }
}
