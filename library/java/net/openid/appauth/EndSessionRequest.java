/*
 * Copyright 2016 The AppAuth for Android Authors. All Rights Reserved.
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

import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An OpenID end session request.
 *
 * NOTE: That is a draft implementation
 *
 * @see "OpenID Connect Session Management 1.0 - draft 28, 5 RP-Initiated Logout
 * <https://openid.net/specs/openid-connect-session-1_0.html#RPLogout>"
 */
public class EndSessionRequest extends AuthorizationManagementRequest {

    private static final String PARAM_ID_TOKEN_HINT = "id_token_hint";
    private static final String PARAM_REDIRECT_URI = "post_logout_redirect_uri";
    private static final String PARAM_STATE = "state";
    private static final String KEY_CONFIGURATION = "configuration";

    @VisibleForTesting
    static final String KEY_ID_TOKEN_HINT = "id_token_hint";
    @VisibleForTesting
    static final String KEY_REDIRECT_URI = "post_logout_redirect_uri";
    @VisibleForTesting
    static final String KEY_STATE = "state";

    /**
     * The service's {@link AuthorizationServiceConfiguration configuration}.
     * This configuration specifies how to connect to a particular OAuth provider.
     * Configurations may be
     * {@link
     * AuthorizationServiceConfiguration#AuthorizationServiceConfiguration(Uri, Uri, Uri, Uri)}
     * created manually}, or {@link AuthorizationServiceConfiguration#fetchFromUrl(Uri,
     * AuthorizationServiceConfiguration.RetrieveConfigurationCallback)} via an OpenID Connect
     * Discovery Document}.
     */
    @NonNull
    public final AuthorizationServiceConfiguration configuration;

    /**
     * An OpenID Connect ID Token. Contains claims about the authentication of an End-User by an
     * Authorization Server.
     *
     * @see "OpenID Connect Session Management 1.0 - draft 28, 5 RP-Initiated Logout
     * <https://openid.net/specs/openid-connect-session-1_0.html#RPLogout>"
     * @see "OpenID Connect Core ID Token, Section 2
     * <http://openid.net/specs/openid-connect-core-1_0.html#IDToken>"
     */
    @NonNull
    public final String idToken;

    /**
     * The client's redirect URI.
     *
     * @see "OpenID Connect Session Management 1.0 - draft 28, 5.1.  Redirection to RP After Logout
     * <https://openid.net/specs/openid-connect-session-1_0.html#RedirectionAfterLogout>"
     */
    @NonNull
    public final Uri redirectUri;

    /**
     * An opaque value used by the client to maintain state between the request and callback. If
     * this value is not explicitly set, this library will automatically add state and perform
     * appropriate  validation of the state in the authorization response. It is recommended that
     * the default implementation of this parameter be used wherever possible. Typically used to
     * prevent CSRF attacks, as recommended in
     *
     * @see "OpenID Connect Session Management 1.0 - draft 28, 5  RP-Initiated Logout
     * <https://openid.net/specs/openid-connect-session-1_0.html#RedirectionAfterLogout>"
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 5.3.5
     * <https://tools.ietf.org/html/rfc6749#section-5.3.5>"
     */
    @NonNull
    public final String state;

    /**
     * Creates instances of {@link EndSessionRequest}.
     */
    public static final class Builder {

        @NonNull
        private AuthorizationServiceConfiguration mConfiguration;

        @NonNull
        private String mIdToken;

        @NonNull
        private Uri mRedirectUri;

        @NonNull
        private String mState;

        public Builder(
                @NonNull AuthorizationServiceConfiguration configuration,
                @NonNull String idToken,
                @NonNull Uri redirectUri) {
            setAuthorizationServiceConfiguration(configuration);
            setIdToken(idToken);
            setRedirectUri(redirectUri);
            setState(AuthorizationManagementRequest.generateRandomState());
        }

        /**
         * Specifies the service configuration to be used in dispatching this request.
         */
        public Builder setAuthorizationServiceConfiguration(
                @NonNull AuthorizationServiceConfiguration configuration) {
            mConfiguration = checkNotNull(configuration, "configuration cannot be null");
            return this;
        }

        public Builder setIdToken(@NonNull String idToken) {
            mIdToken = checkNotEmpty(idToken, "idToken cannot be null or empty");
            return this;
        }

        public Builder setRedirectUri(@Nullable Uri redirectUri) {
            mRedirectUri = checkNotNull(redirectUri, "redirect Uri cannot be null");
            return this;
        }

        public Builder setState(@NonNull String state) {
            mState = checkNotEmpty(state, "state cannot be null or empty");
            return this;
        }

        /**
         * Constructs an end session request. All fields must be set.
         * Failure to specify any of these parameters will result in a runtime exception.
         */
        @NonNull
        public EndSessionRequest build() {
            return new EndSessionRequest(
                mConfiguration,
                mIdToken,
                mRedirectUri,
                mState);
        }
    }

    @VisibleForTesting
    private EndSessionRequest(
            @NonNull AuthorizationServiceConfiguration configuration,
            @NonNull String idToken,
            @NonNull Uri redirectUri,
            @NonNull String state) {
        this.configuration = configuration;
        this.idToken = idToken;
        this.redirectUri = redirectUri;
        this.state = state;
    }

    @Override
    @NonNull
    public String getState() {
        return state;
    }

    @Override
    public Uri toUri() {
        Uri.Builder uriBuilder = configuration.endSessionEndpoint.buildUpon()
                .appendQueryParameter(PARAM_REDIRECT_URI, redirectUri.toString())
                .appendQueryParameter(PARAM_ID_TOKEN_HINT, idToken)
                .appendQueryParameter(PARAM_STATE, state);
        return  uriBuilder.build();
    }

    /**
     * Produces a JSON representation of the end session request for persistent storage or local
     * transmission (e.g. between activities).
     */
    @Override
    public JSONObject jsonSerialize() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_CONFIGURATION, configuration.toJson());
        JsonUtil.put(json, KEY_ID_TOKEN_HINT, idToken);
        JsonUtil.put(json, KEY_REDIRECT_URI, redirectUri.toString());
        JsonUtil.put(json, KEY_STATE, state);
        return json;
    }

    /**
     * Reads an authorization request from a JSON string representation produced by
     * {@link #jsonSerialize()}.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    public static EndSessionRequest jsonDeserialize(@NonNull JSONObject jsonObject)
            throws JSONException {
        checkNotNull(jsonObject, "json cannot be null");
        return new EndSessionRequest(
            AuthorizationServiceConfiguration.fromJson(jsonObject.getJSONObject(KEY_CONFIGURATION)),
            JsonUtil.getString(jsonObject, KEY_ID_TOKEN_HINT),
            JsonUtil.getUri(jsonObject, KEY_REDIRECT_URI),
            JsonUtil.getString(jsonObject, KEY_STATE)
        );
    }

    /**
     * Reads an authorization request from a JSON string representation produced by
     * {@link #jsonSerializeString()}. This method is just a convenience wrapper for
     * {@link #jsonDeserialize(JSONObject)}, converting the JSON string to its JSON object form.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static EndSessionRequest jsonDeserialize(@NonNull String jsonStr)
            throws JSONException {
        checkNotNull(jsonStr, "json string cannot be null");
        return jsonDeserialize(new JSONObject(jsonStr));
    }

    static boolean isEndSessionRequest(JSONObject json) {
        return json.has(KEY_REDIRECT_URI);
    }

}
