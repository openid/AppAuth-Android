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

import static net.openid.appauth.AdditionalParamsProcessor.builtInParams;
import static net.openid.appauth.AdditionalParamsProcessor.checkAdditionalParams;
import static net.openid.appauth.Preconditions.checkNotNull;
import static net.openid.appauth.Preconditions.checkNullOrNotEmpty;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import net.openid.appauth.internal.UriUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An OpenID end session request.
 *
 * @see "OpenID Connect RP-Initiated Logout 1.0 - draft 01
 * <https://openid.net/specs/openid-connect-rpinitiated-1_0.html>"
 */
public class EndSessionRequest implements AuthorizationManagementRequest {

    @VisibleForTesting
    static final String PARAM_ID_TOKEN_HINT = "id_token_hint";

    @VisibleForTesting
    static final String PARAM_POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";

    @VisibleForTesting
    static final String PARAM_STATE = "state";

    @VisibleForTesting
    static final String PARAM_UI_LOCALES = "ui_locales";

    private static final Set<String> BUILT_IN_PARAMS = builtInParams(
            PARAM_ID_TOKEN_HINT,
            PARAM_POST_LOGOUT_REDIRECT_URI,
            PARAM_STATE,
            PARAM_UI_LOCALES);

    private static final String KEY_CONFIGURATION = "configuration";
    private static final String KEY_ID_TOKEN_HINT = "id_token_hint";
    private static final String KEY_POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";
    private static final String KEY_STATE = "state";
    private static final String KEY_UI_LOCALES = "ui_locales";
    private static final String KEY_ADDITIONAL_PARAMETERS = "additionalParameters";

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
     * Previously issued ID Token passed to the end session endpoint as a hint about the End-User's
     * current authenticated session with the Client
     *
     * @see "OpenID Connect Session Management 1.0 - draft 28, 5 RP-Initiated Logout
     * <https://openid.net/specs/openid-connect-rpinitiated-1_0.html#RPLogout>"
     * @see "OpenID Connect Core ID Token, Section 2
     * <http://openid.net/specs/openid-connect-core-1_0.html#IDToken>"
     */
    @Nullable
    public final String idTokenHint;

    /**
     * The client's redirect URI.
     *
     * @see "OpenID Connect RP-Initiated Logout 1.0 - draft 1, 3.  Redirection to RP After Logout
     * <https://openid.net/specs/openid-connect-rpinitiated-1_0.html#RedirectionAfterLogout>"
     */
    @Nullable
    public final Uri postLogoutRedirectUri;

    /**
     * An opaque value used by the client to maintain state between the request and callback. If
     * this value is not explicitly set, this library will automatically add state and perform
     * appropriate  validation of the state in the authorization response. It is recommended that
     * the default implementation of this parameter be used wherever possible. Typically used to
     * prevent CSRF attacks, as recommended in
     *
     * @see "OpenID Connect RP-Initiated Logout 1.0 - draft 1, 2.  RP-Initiated Logout
     * <https://openid.net/specs/openid-connect-rpinitiated-1_0.html#RPLogout>"
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 5.3.5
     * <https://tools.ietf.org/html/rfc6749#section-5.3.5>"
     */
    @Nullable
    public final String state;

    /**
     * This is a space-separated list of BCP47 [RFC5646] language tag values, ordered by preference.
     * It represents End-User's preferred languages and scripts for the user interface.
     *
     * @see "OpenID Connect RP-Initiated Logout 1.0 - draft 01
     * <https://openid.net/specs/openid-connect-rpinitiated-1_0.html#RPLogout>"
     */
    @Nullable
    public final String uiLocales;

    /**
     * Additional parameters to be passed as part of the request.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1
     * <https://tools.ietf.org/html/rfc6749#section-3.1>"
     */
    @NonNull
    public final Map<String, String> additionalParameters;

    /**
     * Creates instances of {@link EndSessionRequest}.
     */
    public static final class Builder {

        @NonNull
        private AuthorizationServiceConfiguration mConfiguration;

        @Nullable
        private String mIdTokenHint;

        @Nullable
        private Uri mPostLogoutRedirectUri;

        @Nullable
        private String mState;

        @Nullable
        private String mUiLocales;

        @NonNull
        private Map<String, String> mAdditionalParameters = new HashMap<>();

        /**
         * Creates an end-session request builder with the specified mandatory properties
         * and preset value for {@link AuthorizationRequest#state}.
         */
        public Builder(@NonNull AuthorizationServiceConfiguration configuration) {
            setAuthorizationServiceConfiguration(configuration);
            setState(AuthorizationManagementUtil.generateRandomState());
        }

        /** @see EndSessionRequest#configuration */
        @NonNull
        public Builder setAuthorizationServiceConfiguration(
                @NonNull AuthorizationServiceConfiguration configuration) {
            mConfiguration = checkNotNull(configuration, "configuration cannot be null");
            return this;
        }

        /** @see EndSessionRequest#idTokenHint */
        @NonNull
        public Builder setIdTokenHint(@Nullable String idTokenHint) {
            mIdTokenHint = checkNullOrNotEmpty(idTokenHint, "idTokenHint must not be empty");
            return this;
        }

        /** @see EndSessionRequest#postLogoutRedirectUri */
        @NonNull
        public Builder setPostLogoutRedirectUri(@Nullable Uri postLogoutRedirectUri) {
            mPostLogoutRedirectUri = postLogoutRedirectUri;
            return this;
        }

        /** @see EndSessionRequest#state */
        @NonNull
        public Builder setState(@Nullable String state) {
            mState = checkNullOrNotEmpty(state, "state must not be empty");
            return this;
        }

        /** @see EndSessionRequest#uiLocales */
        @NonNull
        public Builder setUiLocales(@Nullable String uiLocales) {
            mUiLocales = checkNullOrNotEmpty(uiLocales, "uiLocales must be null or not empty");
            return this;
        }

        /** @see EndSessionRequest#uiLocales */
        @NonNull
        public Builder setUiLocalesValues(@Nullable String... uiLocalesValues) {
            if (uiLocalesValues == null) {
                mUiLocales = null;
                return this;
            }

            return setUiLocalesValues(Arrays.asList(uiLocalesValues));
        }

        /** @see EndSessionRequest#uiLocales */
        @NonNull
        public Builder setUiLocalesValues(
                @Nullable Iterable<String> uiLocalesValues) {
            mUiLocales = AsciiStringListUtil.iterableToString(uiLocalesValues);
            return this;
        }

        /** @see EndSessionRequest#additionalParameters */
        @NonNull
        public Builder setAdditionalParameters(@Nullable Map<String, String> additionalParameters) {
            mAdditionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS);
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
                mIdTokenHint,
                mPostLogoutRedirectUri,
                mState,
                mUiLocales,
                Collections.unmodifiableMap(new HashMap<>(mAdditionalParameters)));
        }
    }

    private EndSessionRequest(
            @NonNull AuthorizationServiceConfiguration configuration,
            @Nullable String idTokenHint,
            @Nullable Uri postLogoutRedirectUri,
            @Nullable String state,
            @Nullable String uiLocales,
            @NonNull Map<String, String> additionalParameters) {
        this.configuration = configuration;
        this.idTokenHint = idTokenHint;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
        this.state = state;
        this.uiLocales = uiLocales;
        this.additionalParameters = additionalParameters;
    }

    @Override
    @Nullable
    public String getState() {
        return state;
    }

    public Set<String> getUiLocales() {
        return AsciiStringListUtil.stringToSet(uiLocales);
    }

    @Override
    public Uri toUri() {
        Uri.Builder uriBuilder = configuration.endSessionEndpoint.buildUpon();

        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_ID_TOKEN_HINT, idTokenHint);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_STATE, state);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_UI_LOCALES, uiLocales);

        if (postLogoutRedirectUri != null) {
            uriBuilder.appendQueryParameter(PARAM_POST_LOGOUT_REDIRECT_URI,
                    postLogoutRedirectUri.toString());
        }

        for (Map.Entry<String, String> entry : additionalParameters.entrySet()) {
            uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
        }

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
        JsonUtil.putIfNotNull(json, KEY_ID_TOKEN_HINT, idTokenHint);
        JsonUtil.putIfNotNull(json, KEY_POST_LOGOUT_REDIRECT_URI, postLogoutRedirectUri);
        JsonUtil.putIfNotNull(json, KEY_STATE, state);
        JsonUtil.putIfNotNull(json, KEY_UI_LOCALES, uiLocales);
        JsonUtil.put(json, KEY_ADDITIONAL_PARAMETERS,
                JsonUtil.mapToJsonObject(additionalParameters));
        return json;
    }

    /**
     * Produces a JSON string representation of the request for persistent storage or
     * local transmission (e.g. between activities). This method is just a convenience wrapper
     * for {@link #jsonSerialize()}, converting the JSON object to its string form.
     */
    @Override
    public String jsonSerializeString() {
        return jsonSerialize().toString();
    }

    /**
     * Reads an authorization request from a JSON string representation produced by
     * {@link #jsonSerialize()}.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    public static EndSessionRequest jsonDeserialize(@NonNull JSONObject json)
            throws JSONException {
        checkNotNull(json, "json cannot be null");
        return new EndSessionRequest(
                AuthorizationServiceConfiguration.fromJson(json.getJSONObject(KEY_CONFIGURATION)),
                JsonUtil.getStringIfDefined(json, KEY_ID_TOKEN_HINT),
                JsonUtil.getUriIfDefined(json, KEY_POST_LOGOUT_REDIRECT_URI),
                JsonUtil.getStringIfDefined(json, KEY_STATE),
                JsonUtil.getStringIfDefined(json, KEY_UI_LOCALES),
                JsonUtil.getStringMap(json, KEY_ADDITIONAL_PARAMETERS));
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
}
