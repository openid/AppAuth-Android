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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import net.openid.appauth.AuthorizationException.AuthorizationRequestErrors;

/**
 * Activity that receives the redirect Uri sent by the OpenID endpoint. This activity gets launched
 * when the user approves the app for use and it starts the {@link PendingIntent} given in
 * {@link AuthorizationService#performAuthorizationRequest}.
 *
 * <p>App developers using this library <em>must</em> to register this activity in the manifest
 * with one intent filter for each redirect URI they are intending to use.
 *
 * <pre>
 * {@code
 * < intent-filter>
 *   < action android:name="android.intent.action.VIEW"/>
 *   < category android:name="android.intent.category.DEFAULT"/>
 *   < category android:name="android.intent.category.BROWSABLE"/>
 *   < data android:scheme="REDIRECT_URI_SCHEME"/>
 * < /intent-filter>
 * }
 * </pre>
 */
@SuppressLint("Registered")
public class RedirectUriReceiverActivity extends Activity {

    private static final String KEY_STATE = "state";
    private Clock mClock = SystemClock.INSTANCE;

    @Override
    public void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        Intent intent = getIntent();
        Uri data = intent.getData();
        String state = data.getQueryParameter(KEY_STATE);
        AuthorizationRequest request = PendingIntentStore.getInstance().getOriginalRequest(state);
        PendingIntent target = PendingIntentStore.getInstance().getPendingIntent(state);

        if (request == null) {
            Logger.error("Response received for unknown request with state %s", state);
            finish();
            return;
        }

        Intent responseData;
        if (data.getQueryParameterNames().contains(AuthorizationException.PARAM_ERROR)) {
            String error = data.getQueryParameter(AuthorizationException.PARAM_ERROR);
            AuthorizationException ex = AuthorizationException.fromOAuthTemplate(
                        AuthorizationRequestErrors.byString(error),
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

        Logger.debug("Forwarding redirect");
        try {
            target.send(this, 0, responseData);
        } catch (PendingIntent.CanceledException e) {
            Logger.errorWithStack(e, "Unable to send pending intent");
        }

        finish();
    }

    @VisibleForTesting
    void setClock(Clock clock) {
        mClock = clock;
    }
}
