package net.openid.appauth;

import android.net.Uri;
import androidx.annotation.VisibleForTesting;

/**
 * Created by Marina Wageed on 22,September,2020
 * Trufla Technology,
 * Cairo, Egypt.
 */

public class LogoutRequest
{
    @VisibleForTesting
    static final String PARAM_REDIRECT_URI = "redirect_uri";

    private Uri logoutEndpoint;
    private Uri redirectUri;

    public LogoutRequest(Uri logoutEndpoint, Uri redirectUri) {
        this.logoutEndpoint = logoutEndpoint;
        this.redirectUri = redirectUri;
    }

    public Uri toUri() {
        Uri.Builder uriBuilder = logoutEndpoint.buildUpon()
            .appendQueryParameter(PARAM_REDIRECT_URI, redirectUri.toString());
        return uriBuilder.build();
    }
}
