package net.openid.appauth;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import net.openid.appauth.internal.Logger;

/**
 * Created by Marina Wageed on 22,September,2020
 * Trufla Technology,
 * Cairo, Egypt.
 */


class LogoutUriReceiverActivity extends Activity
{
    private static final String KEY_STATE = "state";
    private Clock mClock = SystemClock.INSTANCE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        // Uri data = intent.getData();
        // String state = data.getQueryParameter(KEY_STATE);

        LogoutRequest request = PendingLogoutIntentStore.getInstance().getOriginalRequest();
        PendingIntent target = PendingLogoutIntentStore.getInstance().getPendingIntent();

        /*
        Intent responseData;
        if (data.getQueryParameterNames().contains(AuthorizationException.PARAM_ERROR)) {
            String error = data.getQueryParameter(AuthorizationException.PARAM_ERROR);
            AuthorizationException ex = AuthorizationException.fromOAuthTemplate(
                    AuthorizationException.AuthorizationRequestErrors.byString(error),
                    error,
                    data.getQueryParameter(AuthorizationException.PARAM_ERROR_DESCRIPTION),
                    UriUtil.parseUriIfAvailable(
                            data.getQueryParameter(AuthorizationException.PARAM_ERROR_URI)));
            responseData = ex.toIntent();
        } else {
            AuthorizationResponse response = new AuthorizationResponse.Builder(request)
                    .fromUri(data, mClock)
                    .build();
            responseData = response.toIntent();
        }
        */

        Logger.debug("Forwarding redirect");
        try {
            target.send(this, 0, null);
        } catch (PendingIntent.CanceledException e) {
            Logger.errorWithStack(e, "Unable to send pending intent");
        }

        finish();
    }
}
