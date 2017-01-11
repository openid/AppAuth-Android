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

package net.openid.appauthdemo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorRes;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceDiscovery;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.ClientSecretBasic;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;

/**
 * A sample activity to serve as a client to the Native Oauth library.
 */
public class TokenActivity extends AppCompatActivity {

    // constants for intents
    private static final String INTENT_EXTRA_AUTH_CALLBACK = "authorizationCallback";
    private static final String INTENT_EXTRA_LOGOUT_CALLBACK = "logoutCallback";
    private static final int LOGOUT_CALLBACK_INTENT_CONSTANT = 12345;

    // constants for saving state on disk
    private static final String FILE_SAVED_APP_STATE = "savedAppState";
    private static final String KEY_DISCOVERY_DOC_JSON = "discoveryDocInJson";
    private static final String KEY_AUTH_STATE_JSON = "authStateInJson";
    private static final String KEY_AUTH_MODE_STRING = "authModeAsString";
    private static final String KEY_REFRESH_TIMESTAMP = "refreshTimestamp";

    private static final String TAG = "TokenActivity";

    private static final String KEY_AUTH_STATE = "authState";
    private static final String KEY_USER_INFO = "userInfo";

    private static final String EXTRA_AUTH_SERVICE_DISCOVERY = "authServiceDiscovery";
    private static final String EXTRA_AUTH_STATE = "authState";

    private static final int BUFFER_SIZE = 1024;

    private AuthState mAuthState;
    private AuthorizationService mAuthService;
    private JSONObject mUserInfoJson;
    private IdentityProvider mIdentityProvider;
    private LogoutService mLogoutService;
    private TokenResponse tokenResponse;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);

        mAuthService = new AuthorizationService(this);

        mIdentityProvider = IdentityProvider.getEnabledProviders(TokenActivity.this).get(0);
        mLogoutService = new LogoutService(TokenActivity.this);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_AUTH_STATE)) {
                try {
                    mAuthState = AuthState.jsonDeserialize(
                            savedInstanceState.getString(KEY_AUTH_STATE));
                } catch (JSONException ex) {
                    Log.e(TAG, "Malformed authorization JSON saved", ex);
                }
            }

            if (savedInstanceState.containsKey(KEY_USER_INFO)) {
                try {
                    mUserInfoJson = new JSONObject(savedInstanceState.getString(KEY_USER_INFO));
                } catch (JSONException ex) {
                    Log.e(TAG, "Failed to parse saved user info JSON", ex);
                }
            }
        }

        if (mAuthState == null) {
            mAuthState = getAuthStateFromIntent(getIntent());
            AuthorizationResponse response = AuthorizationResponse.fromIntent(getIntent());
            AuthorizationException ex = AuthorizationException.fromIntent(getIntent());
            mAuthState.update(response, ex);

            if (response != null) {
                Log.d(TAG, "Received AuthorizationResponse.");
                showSnackbar(R.string.exchange_notification);
                exchangeAuthorizationCode(response);
            } else {
                Log.i(TAG, "Authorization failed: " + ex);
                showSnackbar(R.string.authorization_failed);
            }
        }

        refreshUi();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        if (mAuthState != null) {
            state.putString(KEY_AUTH_STATE, mAuthState.jsonSerializeString());
        }

        if (mUserInfoJson != null) {
            state.putString(KEY_USER_INFO, mUserInfoJson.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        refreshUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        if (mAuthService != null) {
            mAuthService.dispose();
        }
        mAuthService = null;
        if (mLogoutService != null) {
            mLogoutService.dispose();
            mLogoutService = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        refreshUi();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
        saveAuthState(true);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        if (mAuthService != null) {
            mAuthService.dispose();
            mAuthService = null;
        }
        if (mLogoutService != null) {
            mLogoutService.dispose();
            mLogoutService = null;
        }

    }


    // save AuthState internal state to permanent storage
    private void saveAuthState(boolean immediately) {
        Log.d(TAG, "saveAuthState(): mAuthState.len=" + (mAuthState != null ?
                mAuthState.jsonSerializeString().length() : "0"));
        if (mAuthState != null) {
            SharedPreferences appPrefs = getSharedPreferences(FILE_SAVED_APP_STATE, MODE_PRIVATE);
            if (immediately) {
                appPrefs.edit()
                        .putString(KEY_AUTH_STATE_JSON, mAuthState.jsonSerializeString())
                        .commit();
            } else {
                appPrefs.edit()
                        .putString(KEY_AUTH_STATE_JSON, mAuthState.jsonSerializeString())
                        .apply();
            }
        }
    }

    private void receivedTokenResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        Log.d(TAG, "Token request complete");
        this.tokenResponse = tokenResponse;
        if (this.tokenResponse != null &&
                this.tokenResponse.idToken != null && tokenResponse.idToken.length() > 0) {
            performTokenValidation(tokenResponse);
        } else {
            mAuthState.update(tokenResponse, authException);
            showSnackbar((tokenResponse != null)
                    ? R.string.exchange_complete
                    : R.string.refresh_failed);
            refreshUi();
        }
    }

    private void refreshUi() {
        TextView refreshTokenInfoView = (TextView) findViewById(R.id.refresh_token_info);
        TextView accessTokenInfoView = (TextView) findViewById(R.id.access_token_info);
        TextView idTokenInfoView = (TextView) findViewById(R.id.id_token_info);
        Button refreshTokenButton = (Button) findViewById(R.id.refresh_token);

        if (mAuthState.isAuthorized()) {
            refreshTokenInfoView.setText((mAuthState.getRefreshToken() == null)
                    ? R.string.no_refresh_token_returned
                    : R.string.refresh_token_returned);

            idTokenInfoView.setText((mAuthState.getIdToken()) == null
                    ? R.string.no_id_token_returned
                    : R.string.id_token_returned);

            if (mAuthState.getAccessToken() == null) {
                accessTokenInfoView.setText(R.string.no_access_token_returned);
            } else {
                Long expiresAt = mAuthState.getAccessTokenExpirationTime();
                String expiryStr;
                if (expiresAt == null) {
                    expiryStr = getResources().getString(R.string.unknown_expiry);
                } else {
                    expiryStr = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL)
                            .format(new Date(expiresAt));
                }
                String tokenInfo = String.format(
                        getResources().getString(R.string.access_token_expires_at),
                        expiryStr);
                accessTokenInfoView.setText(tokenInfo);
            }
        }

        refreshTokenButton.setVisibility(mAuthState.getRefreshToken() != null
                ? View.VISIBLE
                : View.GONE);
        refreshTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshAccessToken();
            }
        });

        Button viewProfileButton = (Button) findViewById(R.id.view_profile);

        AuthorizationServiceDiscovery discoveryDoc = getDiscoveryDocFromIntent(getIntent());
        if (!mAuthState.isAuthorized()
                || discoveryDoc == null
                || discoveryDoc.getUserinfoEndpoint() == null) {
            viewProfileButton.setVisibility(View.GONE);
        } else {
            viewProfileButton.setVisibility(View.VISIBLE);
            viewProfileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            fetchUserInfo();
                            return null;
                        }
                    }.execute();
                }
            });
        }

        Button viewLogoutButton = (Button) findViewById(R.id.logout);

        if (!mAuthState.isAuthorized()
                || discoveryDoc == null
                || discoveryDoc.getUserinfoEndpoint() == null) {
            viewLogoutButton.setVisibility(View.GONE);
        } else {
            viewLogoutButton.setVisibility(View.VISIBLE);
            viewLogoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            makeLogoutRequest();
                            return null;
                        }
                    }.execute();
                }
            });
        }
        View userInfoCard = findViewById(R.id.userinfo_card);
        if (mUserInfoJson == null) {
            userInfoCard.setVisibility(View.INVISIBLE);
        } else {
            try {
                String name = "???";
                if (mUserInfoJson.has("name")) {
                    name = mUserInfoJson.getString("name");
                }
                ((TextView) findViewById(R.id.userinfo_name)).setText(name);

                if (mUserInfoJson.has("picture")) {
                    Glide.with(TokenActivity.this)
                            .load(Uri.parse(mUserInfoJson.getString("picture")))
                            .fitCenter()
                            .into((ImageView) findViewById(R.id.userinfo_profile));
                }

                ((TextView) findViewById(R.id.userinfo_json)).setText(mUserInfoJson.toString(2));

                userInfoCard.setVisibility(View.VISIBLE);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed to read userinfo JSON", ex);
            }
        }
    }

    private void refreshAccessToken() {
        performTokenRequest(mAuthState.createTokenRefreshRequest());
    }

    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
        performTokenRequest(authorizationResponse.createTokenExchangeRequest());
    }

    private void performTokenRequest(TokenRequest request) {
        ClientAuthentication clientAuthentication;
        try {
            clientAuthentication = mAuthState.getClientAuthentication();
            if ((mAuthState.getClientSecret() == null
                    || TextUtils.isEmpty(mAuthState.getClientSecret())
                    && IdentityProvider.getEnabledProviders(TokenActivity.this).get(0).getClientSecret() != null)) {
                clientAuthentication = new ClientSecretBasic(IdentityProvider.getEnabledProviders(TokenActivity.this).get(0).getClientSecret());
            }
        } catch (ClientAuthentication.UnsupportedAuthenticationMethod ex) {
            Log.d(TAG, "Token request cannot be made, client authentication for the token "
                    + "endpoint could not be constructed (%s)", ex);
            return;
        }

        mAuthService.performTokenRequest(
                request,
                clientAuthentication,
                new AuthorizationService.TokenResponseCallback() {
                    @Override
                    public void onTokenRequestCompleted(
                            @Nullable TokenResponse tokenResponse,
                            @Nullable AuthorizationException ex) {
                        receivedTokenResponse(tokenResponse, ex);
                    }
                });
    }

    private void performTokenValidation(TokenResponse response) {
        ClientAuthentication clientAuthentication;
        try {
            clientAuthentication = mAuthState.getClientAuthentication();
            if ((mAuthState.getClientSecret() == null
                    || TextUtils.isEmpty(mAuthState.getClientSecret())
                    && IdentityProvider.getEnabledProviders(TokenActivity.this).get(0).getClientSecret() != null)) {
                clientAuthentication = new ClientSecretBasic(IdentityProvider.getEnabledProviders(TokenActivity.this).get(0).getClientSecret());
            }
        } catch (ClientAuthentication.UnsupportedAuthenticationMethod ex) {
            Log.d(TAG, "Token request cannot be made, client authentication for the token "
                    + "endpoint could not be constructed (%s)", ex);
            return;
        }
        mAuthService = new AuthorizationService(this);

        mAuthService.performTokenValidationRequest(
                response,
                clientAuthentication,
                new AuthorizationService.TokenValidationResponseCallback() {
                    @Override
                    public void onTokenValidationRequestCompleted(boolean isTokenValid, @Nullable AuthorizationException ex) {
                        if (isTokenValid) {
                            mAuthState.update(tokenResponse, ex);
                            showSnackbar((tokenResponse != null)
                                    ? R.string.exchange_complete
                                    : R.string.refresh_failed);
                            refreshUi();
                        } else {
                            mAuthState.update(tokenResponse, ex);
                            showSnackbar((tokenResponse != null)
                                    ? R.string.exchange_complete
                                    : R.string.refresh_failed);
                            refreshUi();
                        }
                    }
                });
    }


    private void fetchUserInfo() {
        if (mAuthState.getAuthorizationServiceConfiguration() == null) {
            Log.e(TAG, "Cannot make userInfo request without service configuration");
        }

        mAuthState.performActionWithFreshTokens(mAuthService, new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, AuthorizationException ex) {
                if (ex != null) {
                    Log.e(TAG, "Token refresh failed when fetching user info");
                    return;
                }

                AuthorizationServiceDiscovery discoveryDoc = getDiscoveryDocFromIntent(getIntent());
                if (discoveryDoc == null) {
                    throw new IllegalStateException("no available discovery doc");
                }

                URL userInfoEndpoint;
                try {
                    userInfoEndpoint = new URL(discoveryDoc.getUserinfoEndpoint().toString());
                } catch (MalformedURLException urlEx) {
                    Log.e(TAG, "Failed to construct user info endpoint URL", urlEx);
                    return;
                }

                InputStream userInfoResponse = null;
                try {
                    HttpURLConnection conn = (HttpURLConnection) userInfoEndpoint.openConnection();
                    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                    conn.setInstanceFollowRedirects(false);
                    userInfoResponse = conn.getInputStream();
                    String response = readStream(userInfoResponse);
                    updateUserInfo(new JSONObject(response));
                } catch (IOException ioEx) {
                    Log.e(TAG, "Network error when querying userinfo endpoint", ioEx);
                } catch (JSONException jsonEx) {
                    Log.e(TAG, "Failed to parse userinfo response");
                } finally {
                    if (userInfoResponse != null) {
                        try {
                            userInfoResponse.close();
                        } catch (IOException ioEx) {
                            Log.e(TAG, "Failed to close userinfo response stream", ioEx);
                        }
                    }
                }
            }
        });
    }

    private void makeLogoutRequest() {
        final Activity activityContext = TokenActivity.this;

        if (mAuthState.getAuthorizationServiceConfiguration() == null) {
            Log.e(TAG, "Cannot make userInfo request without service configuration");
        }

        mAuthState.performActionWithFreshTokens(mAuthService, new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, AuthorizationException ex) {
                if (ex != null) {
                    Log.e(TAG, "Token refresh failed when performing logout", ex);
                    Toast.makeText(TokenActivity.this, "Token refresh failed when performing logout: "
                            + ex.getMessage(), Toast.LENGTH_SHORT).show();
                    cleanLocalData();
                    refreshUi();
                    return;
                }

                Log.d(TAG, "makeLogoutRequest():  calling logoutService");
                mLogoutService.performLogoutRequest(mAuthState.getIdToken(),
                        mIdentityProvider,
                        createPostLogoutIntent(activityContext),
                        mAuthService.createCustomTabsIntentBuilder()
                                .setToolbarColor(getColorCompat(R.color.colorAccent))
                                .build());
            }
        });
    }

    private void cleanLocalData() {
        Log.d(TAG,"cleanLocalData()");
        mAuthState = new AuthState();

        SharedPreferences appPrefs = getSharedPreferences(FILE_SAVED_APP_STATE, MODE_PRIVATE);
        appPrefs.edit()
                .clear()
                .apply();

        Intent intent = new Intent(TokenActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // intent for starting up main activity after logout response redirect
    private static PendingIntent createPostLogoutIntent(
            @NonNull Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(INTENT_EXTRA_LOGOUT_CALLBACK, true);
        return PendingIntent.getActivity(context, LOGOUT_CALLBACK_INTENT_CONSTANT, intent, 0);
        // PendingIntent.FLAG_UPDATE_CURRENT  );
    }

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressWarnings("deprecation")
    private int getColorCompat(@ColorRes int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(color);
        } else {
            return getResources().getColor(color);
        }
    }

    private void updateUserInfo(final JSONObject jsonObject) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mUserInfoJson = jsonObject;
                refreshUi();
            }
        });
    }

    @MainThread
    private void showSnackbar(@StringRes int messageId) {
        Snackbar.make(findViewById(R.id.coordinator),
                getResources().getString(messageId),
                Snackbar.LENGTH_SHORT)
                .show();
    }

    private static String readStream(InputStream stream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        char[] buffer = new char[BUFFER_SIZE];
        StringBuilder sb = new StringBuilder();
        int readCount;
        while ((readCount = br.read(buffer)) != -1) {
            sb.append(buffer, 0, readCount);
        }
        return sb.toString();
    }

    static PendingIntent createPostAuthorizationIntent(
            @NonNull Context context,
            @NonNull AuthorizationRequest request,
            @Nullable AuthorizationServiceDiscovery discoveryDoc,
            @NonNull AuthState authState) {
        Intent intent = new Intent(context, TokenActivity.class);
        intent.putExtra(EXTRA_AUTH_STATE, authState.jsonSerializeString());
        if (discoveryDoc != null) {
            intent.putExtra(EXTRA_AUTH_SERVICE_DISCOVERY, discoveryDoc.docJson.toString());
        }

        return PendingIntent.getActivity(context, request.hashCode(), intent, 0);
    }

    static AuthorizationServiceDiscovery getDiscoveryDocFromIntent(Intent intent) {
        if (!intent.hasExtra(EXTRA_AUTH_SERVICE_DISCOVERY)) {
            return null;
        }
        String discoveryJson = intent.getStringExtra(EXTRA_AUTH_SERVICE_DISCOVERY);
        try {
            return new AuthorizationServiceDiscovery(new JSONObject(discoveryJson));
        } catch (JSONException | AuthorizationServiceDiscovery.MissingArgumentException ex) {
            throw new IllegalStateException("Malformed JSON in discovery doc");
        }
    }

    static AuthState getAuthStateFromIntent(Intent intent) {
        if (!intent.hasExtra(EXTRA_AUTH_STATE)) {
            throw new IllegalArgumentException("The AuthState instance is missing in the intent.");
        }
        try {
            return AuthState.jsonDeserialize(intent.getStringExtra(EXTRA_AUTH_STATE));
        } catch (JSONException ex) {
            Log.e(TAG, "Malformed AuthState JSON saved", ex);
            throw new IllegalArgumentException("The AuthState instance is missing in the intent.");
        }
    }
}
