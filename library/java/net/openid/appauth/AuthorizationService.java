/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static net.openid.appauth.Preconditions.checkNotNull;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.customtabs.CustomTabsIntent;
import android.text.TextUtils;

import net.openid.appauth.AuthorizationException.GeneralErrors;
import net.openid.appauth.AuthorizationException.TokenRequestErrors;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Dispatches requests to an OAuth2 authorization service. Note that instances of this class
 * <em>must be manually disposed</em> when no longer required, to avoid leaks
 * (see {@link #dispose()}.
 */
public class AuthorizationService {

    /**
     * Scope value which requests access to the end-user's stable identifier.
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">
     *     "Requesting Claims using Scope Values", OpenID Connect Core 1.0 Specification,
     *     Section 5.4</a>
     */
    public static final String SCOPE_OPENID = "openid";

    /**
     * Scope value which requests access to the end-user's default profile claims.
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">
     *     "Requesting Claims using Scope Values", OpenID Connect Core 1.0 Specification,
     *     Section 5.4</a>
     */
    public static final String SCOPE_PROFILE = "profile";

    /**
     * Scope value which requests access to the email and email_verified claims.
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">
     *     "Requesting Claims using Scope Values", OpenID Connect Core 1.0 Specification,
     *     Section 5.4</a>
     */
    public static final String SCOPE_EMAIL = "email";

    /**
     * Scope value which requests access to the address claim.
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">
     *     "Requesting Claims using Scope Values", OpenID Connect Core 1.0 Specification,
     *     Section 5.4</a>
     */
    public static final String SCOPE_ADDRESS = "address";

    /**
     * REQUIRED.  The client identifier issued to the client during the registration process
     * described by Section 2.2.
     */
    @VisibleForTesting
    static final String CLIENT_ID = "client_id";

    /**
     * REQUIRED. OpenID Connect requests MUST contain the openid scope value.
     * If the openid scope value is not present, the behavior is entirely unspecified.
     * Other scope values MAY be present. Scope values used that are not understood by an
     * implementation SHOULD be ignored.
     */
    @VisibleForTesting
    static final String SCOPE = "scope";

    @VisibleForTesting
    static final String RESPONSE_MODE = "response_mode";

    /**
     * REQUIRED. Response Type value that determines the authorization processing flow to be used,
     * including what parameters are returned from the endpoints used.
     * When using the Authorization Code Flow, this value is <code>code</code>.
     */
    @VisibleForTesting
    static final String RESPONSE_TYPE = "response_type";

    /**
     * REQUIRED. Redirection URI to which the response will be sent. This URI MUST exactly
     * match one of the Redirection URI values for the Client pre-registered at the OpenID Provider,
     * with the matching performed as described in Section 6.2.1 of [RFC3986]
     * (Simple String Comparison). When using this flow, the Redirection URI SHOULD use the
     * https scheme; however, it MAY use the http scheme, provided that the Client Type is
     * confidential, as defined in Section 2.1 of OAuth 2.0, and provided the OP allows the use
     * of http Redirection URIs in this case. The Redirection URI MAY use an alternate scheme,
     * such as one that is intended to identify a callback into a native application.
     */
    @VisibleForTesting
    static final String REDIRECT_URI = "redirect_uri";

    @VisibleForTesting
    static final String STATE = "state";

    @VisibleForTesting
    static final String CODE = "code";

    @VisibleForTesting
    static final String CODE_VERIFIER = "code_verifier";

    @VisibleForTesting
    static final String CODE_CHALLENGE = "code_challenge";

    @VisibleForTesting
    static final String CODE_CHALLENGE_METHOD = "code_challenge_method";

    @VisibleForTesting
    static final String REFRESH_TOKEN = "refresh_token";

    @VisibleForTesting
    static final String GRANT_TYPE = "grant_type";

    @VisibleForTesting
    static final String GRANT_TYPE_AUTH_CODE = "authorization_code";

    @VisibleForTesting
    static final String GRANT_TYPE_REFRESH = "refresh_token";

    @VisibleForTesting
    static final String ACCESS_TOKEN = "access_token";

    @VisibleForTesting
    Context mContext;

    @NonNull
    private final UrlBuilder mUrlBuilder;

    @NonNull
    private final BrowserHandler mBrowserHandler;

    private boolean mDisposed = false;

    /**
     * Creates an AuthorizationService instance based on the provided configuration. Note that
     * instances of this class must be manually disposed when no longer required, to avoid
     * leaks (see {@link #dispose()}.
     */
    public AuthorizationService(@NonNull Context context) {
        this(context,
                DefaultUrlBuilder.INSTANCE,
                new BrowserHandler(context));
    }

    /**
     * Constructor that injects a url builder into the service for testing.
     */
    @VisibleForTesting
    AuthorizationService(@NonNull Context context,
                         @NonNull UrlBuilder urlBuilder,
                         @NonNull BrowserHandler browserHandler) {
        mContext = checkNotNull(context);
        mUrlBuilder = checkNotNull(urlBuilder);
        mBrowserHandler = checkNotNull(browserHandler);
    }

    /**
     * Creates a custom tab builder, that will use a tab session from an existing connection to
     * a web browser, if available.
     */
    public CustomTabsIntent.Builder createCustomTabsIntentBuilder() {
        checkNotDisposed();
        return mBrowserHandler.createCustomTabsIntentBuilder();
    }

    /**
     * Sends an authorization request to the authorization service, using a
     * <a href="https://developer.chrome.com/multidevice/android/customtabs">custom tab</a>.
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link AuthorizationRequest request object}. Upon completion
     * of this request, the provided {@link PendingIntent result handler intent} will be invoked.
     */
    public void performAuthorizationRequest(
            @NonNull AuthorizationRequest request,
            @NonNull PendingIntent resultHandlerIntent) {
        performAuthorizationRequest(request,
                resultHandlerIntent,
                createCustomTabsIntentBuilder().build());
    }

    /**
     * Sends an authorization request to the authorization service, using a
     * <a href="https://developer.chrome.com/multidevice/android/customtabs">custom tab</a>.
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link AuthorizationRequest request object}. Upon completion
     * of this request, the provided {@link PendingIntent result handler intent} will be invoked.
     *
     * @param customTabsIntent
     *     The intent that will be used to start the custom tab. It is recommended that this intent
     *     be created with the help of {@link #createCustomTabsIntentBuilder()}, which will ensure
     *     that a warmed-up version of the browser will be used, minimizing latency.
     */
    public void performAuthorizationRequest(
            @NonNull AuthorizationRequest request,
            @NonNull PendingIntent resultHandlerIntent,
            @NonNull CustomTabsIntent customTabsIntent) {
        checkNotDisposed();
        Uri.Builder uriBuilder = request.configuration.authorizationEndpoint.buildUpon()
                .appendQueryParameter(REDIRECT_URI, request.redirectUri.toString())
                .appendQueryParameter(CLIENT_ID, request.clientId)
                .appendQueryParameter(RESPONSE_TYPE, request.responseType);

        if (request.state != null) {
            uriBuilder.appendQueryParameter(STATE, request.state);
        }

        if (request.codeVerifier != null) {
            uriBuilder
                    .appendQueryParameter(CODE_CHALLENGE,
                            request.codeVerifierChallenge)
                    .appendQueryParameter(CODE_CHALLENGE_METHOD,
                            request.codeVerifierChallengeMethod);
        }

        if (request.scope != null) {
            uriBuilder.appendQueryParameter(SCOPE, request.scope);
        }

        if (request.responseMode != null) {
            uriBuilder.appendQueryParameter(RESPONSE_MODE, request.responseMode);
        }

        for (String key : request.additionalParameters.keySet()) {
            String value = request.additionalParameters.get(key);
            uriBuilder.appendQueryParameter(key, value);
        }
        Uri requestUri = uriBuilder.build();
        PendingIntentStore.getInstance().addPendingIntent(request, resultHandlerIntent);

        Intent intent = customTabsIntent.intent;
        intent.setData(requestUri);
        if (TextUtils.isEmpty(intent.getPackage())) {
            intent.setPackage(mBrowserHandler.getBrowserPackage());
        }

        Logger.debug("Using %s as browser for auth", intent.getPackage());
        intent.putExtra(CustomTabsIntent.EXTRA_TITLE_VISIBILITY_STATE, CustomTabsIntent.NO_TITLE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        Logger.debug("Initiating authorization request to %s",
                request.configuration.authorizationEndpoint);
        mContext.startActivity(intent);
    }

    /**
     * Sends a request to the authorization service to exchange a code granted as part of an
     * authorization request for a token. The result of this request will be sent to the provided
     * callback handler.
     */
    public void performTokenRequest(
            @NonNull net.openid.appauth.TokenRequest request,
            @NonNull TokenResponseCallback callback) {
        checkNotDisposed();
        Uri.Builder uri = request.configuration.tokenEndpoint.buildUpon();
        uri.appendQueryParameter(GRANT_TYPE, request.grantType);
        uri.appendQueryParameter(CLIENT_ID, request.clientId);

        if (GRANT_TYPE_AUTH_CODE.equals(request.grantType)) {
            uri.appendQueryParameter(CODE, request.authorizationCode);

            if (request.redirectUri != null) {
                uri.appendQueryParameter(REDIRECT_URI, request.redirectUri.toString());
            }

            if (request.codeVerifier != null) {
                uri.appendQueryParameter(CODE_VERIFIER, request.codeVerifier);
            }
        }

        if (GRANT_TYPE_REFRESH.equals(request.grantType)) {
            uri.appendQueryParameter(REFRESH_TOKEN, request.refreshToken);
        }

        for (Map.Entry<String, String> additionalParam : request.additionalParameters.entrySet()) {
            uri.appendQueryParameter(additionalParam.getKey(), additionalParam.getValue());
        }

        Logger.debug("Initiating code exchange request to %s",
                request.configuration.tokenEndpoint);
        new TokenRequestTask(uri.build(), request, callback)
                .execute();
    }

    /**
     * Disposes state that will not normally be handled by garbage collection. This should be
     * called when the authorization service is no longer required, including when any owning
     * activity is paused or destroyed (i.e. in {@link android.app.Activity#onStop()}).
     */
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

    private class TokenRequestTask
            extends AsyncTask<Void, Void, JSONObject> {
        private Uri mRequestUri;
        private net.openid.appauth.TokenRequest mRequest;
        private TokenResponseCallback mCallback;

        private AuthorizationException mException;

        TokenRequestTask(Uri requestUri,
                         net.openid.appauth.TokenRequest request,
                         TokenResponseCallback callback) {
            mRequestUri = requestUri;
            mRequest = request;
            mCallback = callback;
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            String queryData = mRequestUri.getEncodedQuery();
            InputStream is = null;
            try {
                /*
                 * TODO: handle MalformedUrlException from url construction separately
                 * this is usually indicative of programmer error rather than network error
                 */
                URL url = mUrlBuilder.buildUrlFromString(mRequestUri.getScheme()
                        + "://" + mRequestUri.getHost()
                        + mRequestUri.getPath());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");

                // required by some providers to ensure JSON response
                conn.setRequestProperty("Accept", "application/json");

                conn.setInstanceFollowRedirects(false);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Length", String.valueOf(queryData.length()));
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(queryData);
                wr.flush();

                is = conn.getInputStream();
                String response = Utils.readInputStream(is);
                return new JSONObject(response);
            } catch (IOException ex) {
                Logger.debugWithStack(ex, "Failed to complete exchange request");
                mException = AuthorizationException.fromTemplate(
                        GeneralErrors.NETWORK_ERROR, ex);
            } catch (JSONException ex) {
                Logger.debugWithStack(ex, "Failed to complete exchange request");
                mException = AuthorizationException.fromTemplate(
                        GeneralErrors.JSON_DESERIALIZATION_ERROR, ex);
            } finally {
                Utils.closeQuietly(is);
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            if (mException != null) {
                mCallback.onTokenRequestCompleted(null, mException);
                return;
            }

            if (json.has(AuthorizationException.PARAM_ERROR)) {
                AuthorizationException ex;
                try {
                    String error = json.getString(AuthorizationException.PARAM_ERROR);
                    ex = AuthorizationException.fromOAuthTemplate(
                            TokenRequestErrors.byString(error),
                            error,
                            json.getString(AuthorizationException.PARAM_ERROR_DESCRIPTION),
                            UriUtil.parseUriIfAvailable(
                                    json.getString(AuthorizationException.PARAM_ERROR_URI)));
                } catch (JSONException jsonEx) {
                    ex = AuthorizationException.fromTemplate(
                            GeneralErrors.JSON_DESERIALIZATION_ERROR,
                            jsonEx);
                }
                mCallback.onTokenRequestCompleted(null, ex);
                return;
            }

            TokenResponse response;
            try {
                response = new TokenResponse.Builder(mRequest).fromJson(json).build();
            } catch (JSONException jsonEx) {
                mCallback.onTokenRequestCompleted(null,
                        AuthorizationException.fromTemplate(
                                GeneralErrors.JSON_DESERIALIZATION_ERROR,
                                jsonEx));
                return;
            }

            Logger.debug("Token exchange with %s completed",
                    mRequest.configuration.tokenEndpoint);
            mCallback.onTokenRequestCompleted(response, null);
        }
    }

    /**
     * Callback interface for token endpoint requests.
     * @see AuthorizationService#performTokenRequest
     */
    public interface TokenResponseCallback {
        /**
         * Invoked when the request completes successfully or fails.
         *
         * <p>Exactly one of {@code response} or {@code ex} will be non-null. If
         * {@code response} is {@code null}, a failure occurred during the request. This can
         * happen if a bad URI was provided, no connection to the server could be established, or
         * the response JSON was incomplete or badly formatted.
         *
         * @param response the retrieved token response, if successful; {@code null} otherwise.
         * @param ex a description of the failure, if one occurred: {@code null} otherwise.
         *
         * @see AuthorizationException.TokenRequestErrors
         */
        void onTokenRequestCompleted(@Nullable TokenResponse response,
                @Nullable AuthorizationException ex);
    }

    @VisibleForTesting
    interface UrlBuilder {
        URL buildUrlFromString(String uri) throws IOException;
    }

    static class DefaultUrlBuilder implements UrlBuilder {
        public static final DefaultUrlBuilder INSTANCE = new DefaultUrlBuilder();

        DefaultUrlBuilder() {}

        public URL buildUrlFromString(String uri) throws IOException {
            return new URL(uri);
        }
    }
}
