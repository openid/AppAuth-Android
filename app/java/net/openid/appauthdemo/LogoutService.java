package net.openid.appauthdemo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.customtabs.CustomTabsIntent;
import android.text.TextUtils;
import android.util.Log;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.CustomTabManager;
import net.openid.appauth.browser.Browsers;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static net.openid.appauth.Preconditions.checkNotNull;
import static net.openid.appauthdemo.PendingIntentStore.getPendingIntentStoreInstance;

/**
 * New class for implementing logout functionality, which is missing from AppAuth library
 */
public class LogoutService {
    // logging
    private static final String TAG = "LogoutService";

    // HTTP query parameters for logout
    private static final String PARAM_REDIRECT_URI = "post_logout_redirect_uri";
    private static final String PARAM_ID_TOKEN = "id_token_hint";


    @VisibleForTesting
    Context mContext;

    @NonNull
    private final UrlBuilder mUrlBuilder;

    @NonNull

    private boolean mDisposed = false;

    /**
     * Creates an LogoutService instance based on the provided configuration. Note that
     * instances of this class must be manually disposed when no longer required, to avoid
     * leaks (see {@link #dispose()}.
     */
    public LogoutService(@NonNull Context context) {
        this(context,
                DefaultUrlBuilder.INSTANCE
        );
    }

    /**
     * Constructor that injects a url builder into the service for testing.
     */
    @VisibleForTesting
    protected LogoutService(@NonNull Context context,
                            @NonNull UrlBuilder urlBuilder
    ) {
        mContext = checkNotNull(context);
        mUrlBuilder = checkNotNull(urlBuilder);
    }


    public void performLogoutRequest(
            @NonNull String idToken,
            @NonNull IdentityProvider identityProvider,
            @NonNull PendingIntent postLogoutCallbackIntent,
            @NonNull CustomTabsIntent customTabsIntent) {

        Uri.Builder uriBuilder = identityProvider.getLogoutRedirectUri().buildUpon()
                .appendQueryParameter(PARAM_REDIRECT_URI, identityProvider.getLogoutRedirectUri().toString())
                .appendQueryParameter(PARAM_ID_TOKEN, idToken);
        Uri logoutUri = uriBuilder.build();
        //Log.d(TAG,"performLogoutRequest(): logoutUri=" + logoutUri.toString());

        PendingIntentStore.getPendingIntentStoreInstance().addPendingIntent(postLogoutCallbackIntent);

        Intent newCustomTabsIntent = customTabsIntent.intent;
        AppAuthConfiguration.Builder builder = new AppAuthConfiguration.Builder();

        newCustomTabsIntent.setData(logoutUri);

        Log.d(TAG, "Using " + newCustomTabsIntent.getPackage() + " as browser for logout");
        newCustomTabsIntent.putExtra(CustomTabsIntent.EXTRA_TITLE_VISIBILITY_STATE, CustomTabsIntent.NO_TITLE);
        newCustomTabsIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);


        Log.d(TAG, "Initiating logout request to " + identityProvider.getLogoutRedirectUri());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager manager = mContext.getPackageManager();
            List<ResolveInfo> infos = manager.queryIntentActivities(newCustomTabsIntent, 0);
            if (infos.size() > 0) {
                //Then there is an Application(s) can handle your intent
                mContext.startActivity(newCustomTabsIntent);

            } else {
                //No Application can handle your intent
                Log.d(TAG, "performLogoutRequest: No browser found");


            }
        } else {
            Intent i = new Intent(Intent.ACTION_VIEW,
                    (logoutUri));
            i.setPackage(mContext.getPackageName());
            mContext.startActivity(i);
        }

    }


    /**
     * Disposes state that will not normally be handled by garbage collection. This should be
     * called when the logout service is no longer required, including when any owning
     * activity is paused or destroyed (i.e. in {@link android.app.Activity#onStop()}).
     */
    public void dispose() {
        if (mDisposed) {
            return;
        }
        mDisposed = true;
    }

    private void checkNotDisposed() {
        if (mDisposed) {
            throw new IllegalStateException("Service has been disposed and rendered inoperable");
        }
    }


    @VisibleForTesting
    interface UrlBuilder {
        URL buildUrlFromString(String uri) throws IOException;
    }

    static class DefaultUrlBuilder implements UrlBuilder {
        public static final DefaultUrlBuilder INSTANCE = new DefaultUrlBuilder();

        DefaultUrlBuilder() {
        }

        public URL buildUrlFromString(String uri) throws IOException {
            return new URL(uri);
        }
    }
}

