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

import static net.openid.appauth.AdditionalParamsProcessor.checkAdditionalParams;
import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;
import static net.openid.appauth.Preconditions.checkNullOrNotEmpty;

import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An OAuth2 token request. These are used to exchange codes for tokens, or exchange a refresh
 * token for updated tokens.
 *
 * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.3
 * <https://tools.ietf.org/html/rfc6749#section-4.1.3>"
 */
public class TokenRequest {

    @VisibleForTesting
    static final String KEY_CONFIGURATION = "configuration";
    @VisibleForTesting
    static final String KEY_CLIENT_ID = "clientId";
    @VisibleForTesting
    static final String KEY_NONCE = "nonce";
    @VisibleForTesting
    static final String KEY_GRANT_TYPE = "grantType";
    @VisibleForTesting
    static final String KEY_REDIRECT_URI = "redirectUri";
    @VisibleForTesting
    static final String KEY_SCOPE = "scope";
    @VisibleForTesting
    static final String KEY_AUTHORIZATION_CODE = "authorizationCode";
    @VisibleForTesting
    static final String KEY_REFRESH_TOKEN = "refreshToken";
    @VisibleForTesting
    static final String KEY_CODE_VERIFIER = "codeVerifier";
    @VisibleForTesting
    static final String KEY_ADDITIONAL_PARAMETERS = "additionalParameters";

    public static final String PARAM_CLIENT_ID = "client_id";

    @VisibleForTesting
    static final String PARAM_CODE = "code";

    @VisibleForTesting
    static final String PARAM_CODE_VERIFIER = "code_verifier";

    @VisibleForTesting
    static final String PARAM_GRANT_TYPE = "grant_type";

    @VisibleForTesting
    static final String PARAM_REDIRECT_URI = "redirect_uri";

    @VisibleForTesting
    static final String PARAM_REFRESH_TOKEN = "refresh_token";

    @VisibleForTesting
    static final String PARAM_SCOPE = "scope";

    private static final Set<String> BUILT_IN_PARAMS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    PARAM_CLIENT_ID,
                    PARAM_CODE,
                    PARAM_CODE_VERIFIER,
                    PARAM_GRANT_TYPE,
                    PARAM_REDIRECT_URI,
                    PARAM_REFRESH_TOKEN,
                    PARAM_SCOPE)));


    /**
     * The grant type used when requesting an access token using a username and password.
     * This grant type is not directly supported by this library.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.3.2
     * <https://tools.ietf.org/html/rfc6749#section-4.3.2>"
     */
    public static final String GRANT_TYPE_PASSWORD = "password";

    /**
     * The grant type used when requesting an access token using client credentials, typically
     * TLS client certificates. This grant type is not directly supported by this library.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.4.2
     * <https://tools.ietf.org/html/rfc6749#section-4.4.2>"
     */
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    /**
     * The service's {@link AuthorizationServiceConfiguration configuration}.
     * This configuration specifies how to connect to a particular OAuth provider.
     * Configurations may be
     * {@link
     * AuthorizationServiceConfiguration#AuthorizationServiceConfiguration(Uri, Uri, Uri, Uri)
     * created manually}, or
     * {@link AuthorizationServiceConfiguration#fetchFromUrl(Uri,
     * AuthorizationServiceConfiguration.RetrieveConfigurationCallback)
     * via an OpenID Connect Discovery Document}.
     */
    @NonNull
    public final AuthorizationServiceConfiguration configuration;

    /**
     * The (optional) nonce associated with the current session.
     */
    @Nullable
    public final String nonce;

    /**
     * The client identifier.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4
     * <https://tools.ietf.org/html/rfc6749#section-4>"
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.1
     * <https://tools.ietf.org/html/rfc6749#section-4.1.1>"
     */
    @NonNull
    public final String clientId;

    /**
     * The type of token being sent to the token endpoint.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.3
     * <https://tools.ietf.org/html/rfc6749#section-4.1.3>"
     */
    @NonNull
    public final String grantType;

    /**
     * The client's redirect URI. Required if this token request is to exchange an authorization
     * code for one or more tokens, and must be identical to the value specified in the original
     * authorization request.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.2
     * <https://tools.ietf.org/html/rfc6749#section-3.1.2>"
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.3
     * <https://tools.ietf.org/html/rfc6749#section-4.1.3>"
     */
    @Nullable
    public final Uri redirectUri;

    /**
     * An authorization code to be exchanged for one or more tokens.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.3
     * <https://tools.ietf.org/html/rfc6749#section-4.1.3>"
     */
    @Nullable
    public final String authorizationCode;

    /**
     * A space-delimited set of scopes used to determine the scope of any returned tokens.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.3
     * <https://tools.ietf.org/html/rfc6749#section-3.3>"
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 6
     * <https://tools.ietf.org/html/rfc6749#section-6>"
     */
    @Nullable
    public final String scope;

    /**
     * A refresh token to be exchanged for a new token.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 6
     * <https://tools.ietf.org/html/rfc6749#section-6>"
     */
    @Nullable
    public final String refreshToken;

    /**
     * The code verifier that was used to generate the challenge in the original authorization
     * request, if one was used.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4
     * <https://tools.ietf.org/html/rfc7636#section-4>"
     */
    @Nullable
    public final String codeVerifier;

    /**
     * Additional parameters to be passed as part of the request.
     */
    @NonNull
    public final Map<String, String> additionalParameters;

    /**
     * Creates instances of {@link TokenRequest}.
     */
    public static final class Builder {

        @NonNull
        private AuthorizationServiceConfiguration mConfiguration;

        @NonNull
        private String mClientId;

        @Nullable
        private String mNonce;

        @Nullable
        private String mGrantType;

        @Nullable
        private Uri mRedirectUri;

        @Nullable
        private String mScope;

        @Nullable
        private String mAuthorizationCode;

        @Nullable
        private String mRefreshToken;

        @Nullable
        private String mCodeVerifier;

        @NonNull
        private Map<String, String> mAdditionalParameters;

        /**
         * Creates a token request builder with the specified mandatory properties.
         */
        public Builder(
                @NonNull AuthorizationServiceConfiguration configuration,
                @NonNull String clientId) {
            setConfiguration(configuration);
            setClientId(clientId);
            mAdditionalParameters = new LinkedHashMap<>();
        }

        /**
         * Specifies the authorization service configuration for the request, which must not
         * be null or empty.
         */
        @NonNull
        public Builder setConfiguration(@NonNull AuthorizationServiceConfiguration configuration) {
            mConfiguration = checkNotNull(configuration);
            return this;
        }

        /**
         * Specifies the client ID for the token request, which must not be null or empty.
         */
        @NonNull
        public Builder setClientId(@NonNull String clientId) {
            mClientId = checkNotEmpty(clientId, "clientId cannot be null or empty");
            return this;
        }

        /**
         * Specifies the (optional) nonce for the current session.
         */
        @NonNull
        public Builder setNonce(@Nullable String nonce) {
            if (TextUtils.isEmpty(nonce)) {
                mNonce = null;
            } else {
                this.mNonce = nonce;
            }
            return this;
        }

        /**
         * Specifies the grant type for the request, which must not be null or empty.
         */
        @NonNull
        public Builder setGrantType(@NonNull String grantType) {
            mGrantType = checkNotEmpty(grantType, "grantType cannot be null or empty");
            return this;
        }

        /**
         * Specifies the redirect URI for the request. This is required for authorization code
         * exchanges, but otherwise optional. If specified, the redirect URI must have a scheme.
         */
        @NonNull
        public Builder setRedirectUri(@Nullable Uri redirectUri) {
            if (redirectUri != null) {
                checkNotNull(redirectUri.getScheme(), "redirectUri must have a scheme");
            }
            mRedirectUri = redirectUri;
            return this;
        }

        /**
         * Specifies the encoded scope string, which is a space-delimited set of
         * case-sensitive scope identifiers. Replaces any previously specified scope.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.3
         * <https://tools.ietf.org/html/rfc6749#section-3.3>"
         */
        @NonNull
        public Builder setScope(@Nullable String scope) {
            if (TextUtils.isEmpty(scope)) {
                mScope = null;
            } else {
                setScopes(scope.split(" +"));
            }
            return this;
        }

        /**
         * Specifies the set of case-sensitive scopes. Replaces any previously specified set of
         * scopes. Individual scope strings cannot be null or empty.
         *
         * Scopes specified here are used to obtain a "down-scoped" access token, where the
         * set of scopes specified _must_ be a subset of those already granted in
         * previous requests.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.3
         * <https://tools.ietf.org/html/rfc6749#section-3.3>"
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 6
         * <https://tools.ietf.org/html/rfc6749#section-6>"
         */
        @NonNull
        public Builder setScopes(String... scopes) {
            if (scopes == null) {
                scopes = new String[0];
            }
            setScopes(Arrays.asList(scopes));
            return this;
        }

        /**
         * Specifies the set of case-sensitive scopes. Replaces any previously specified set of
         * scopes. Individual scope strings cannot be null or empty.
         *
         * Scopes specified here are used to obtain a "down-scoped" access token, where the
         * set of scopes specified _must_ be a subset of those already granted in
         * previous requests.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.3
         * <https://tools.ietf.org/html/rfc6749#section-3.3>"
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 6
         * <https://tools.ietf.org/html/rfc6749#section-6>"
         */
        @NonNull
        public Builder setScopes(@Nullable Iterable<String> scopes) {
            mScope = AsciiStringListUtil.iterableToString(scopes);
            return this;
        }

        /**
         * Specifies the authorization code for the request. If provided, the authorization code
         * must not be empty.
         *
         * Specifying an authorization code normally implies that this is a request to exchange
         * this authorization code for one or more tokens. If this is not intended, the grant type
         * should be explicitly set.
         */
        @NonNull
        public Builder setAuthorizationCode(@Nullable String authorizationCode) {
            checkNullOrNotEmpty(authorizationCode, "authorization code must not be empty");
            mAuthorizationCode = authorizationCode;
            return this;
        }

        /**
         * Specifies the refresh token for the request. If a non-null value is provided, it must
         * not be empty.
         *
         * Specifying a refresh token normally implies that this is a request to exchange the
         * refresh token for a new token. If this is not intended, the grant type should be
         * explicit set.
         */
        @NonNull
        public Builder setRefreshToken(@Nullable String refreshToken) {
            if (refreshToken != null) {
                checkNotEmpty(refreshToken, "refresh token cannot be empty if defined");
            }
            mRefreshToken = refreshToken;
            return this;
        }

        /**
         * Specifies the code verifier for an authorization code exchange request. This must match
         * the code verifier that was used to generate the challenge sent in the request that
         * produced the authorization code.
         */
        public Builder setCodeVerifier(@Nullable String codeVerifier) {
            if (codeVerifier != null) {
                CodeVerifierUtil.checkCodeVerifier(codeVerifier);
            }

            mCodeVerifier = codeVerifier;
            return this;
        }

        /**
         * Specifies an additional set of parameters to be sent as part of the request.
         */
        @NonNull
        public Builder setAdditionalParameters(@Nullable Map<String, String> additionalParameters) {
            mAdditionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS);
            return this;
        }

        /**
         * Produces a {@link TokenRequest} instance, if all necessary values have been provided.
         */
        @NonNull
        public TokenRequest build() {
            String grantType = inferGrantType();

            if (GrantTypeValues.AUTHORIZATION_CODE.equals(grantType)) {
                checkNotNull(mAuthorizationCode,
                        "authorization code must be specified for grant_type = "
                                + GrantTypeValues.AUTHORIZATION_CODE);
            }

            if (GrantTypeValues.REFRESH_TOKEN.equals(grantType)) {
                checkNotNull(mRefreshToken,
                        "refresh token must be specified for grant_type = "
                                + GrantTypeValues.REFRESH_TOKEN);
            }


            if (grantType.equals(GrantTypeValues.AUTHORIZATION_CODE) && mRedirectUri == null) {
                throw new IllegalStateException(
                        "no redirect URI specified on token request for code exchange");
            }

            return new TokenRequest(
                    mConfiguration,
                    mClientId,
                    mNonce,
                    grantType,
                    mRedirectUri,
                    mScope,
                    mAuthorizationCode,
                    mRefreshToken,
                    mCodeVerifier,
                    Collections.unmodifiableMap(mAdditionalParameters));
        }

        private String inferGrantType() {
            if (mGrantType != null) {
                return mGrantType;
            } else if (mAuthorizationCode != null) {
                return GrantTypeValues.AUTHORIZATION_CODE;
            } else if (mRefreshToken != null) {
                return GrantTypeValues.REFRESH_TOKEN;
            } else {
                throw new IllegalStateException("grant type not specified and cannot be inferred");
            }
        }
    }

    private TokenRequest(
            @NonNull AuthorizationServiceConfiguration configuration,
            @NonNull String clientId,
            @Nullable String nonce,
            @NonNull String grantType,
            @Nullable Uri redirectUri,
            @Nullable String scope,
            @Nullable String authorizationCode,
            @Nullable String refreshToken,
            @Nullable String codeVerifier,
            @NonNull Map<String, String> additionalParameters) {
        this.configuration = configuration;
        this.clientId = clientId;
        this.nonce = nonce;
        this.grantType = grantType;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.authorizationCode = authorizationCode;
        this.refreshToken = refreshToken;
        this.codeVerifier = codeVerifier;
        this.additionalParameters = additionalParameters;
    }

    /**
     * Derives the set of scopes from the consolidated, space-delimited scopes in the
     * {@link #scope} field. If no scopes were specified for this request, the method will
     * return `null`.
     */
    @Nullable
    public Set<String> getScopeSet() {
        return AsciiStringListUtil.stringToSet(scope);
    }

    /**
     * Produces the set of request parameters for this query, which can be further
     * processed into a request body.
     */
    @NonNull
    public Map<String, String> getRequestParameters() {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_GRANT_TYPE, grantType);
        putIfNotNull(params, PARAM_REDIRECT_URI, redirectUri);
        putIfNotNull(params, PARAM_CODE, authorizationCode);
        putIfNotNull(params, PARAM_REFRESH_TOKEN, refreshToken);
        putIfNotNull(params, PARAM_CODE_VERIFIER, codeVerifier);
        putIfNotNull(params, PARAM_SCOPE, scope);

        for (Entry<String, String> param : additionalParameters.entrySet()) {
            params.put(param.getKey(), param.getValue());
        }

        return params;
    }

    private void putIfNotNull(Map<String, String> map, String key, Object value) {
        if (value != null) {
            map.put(key, value.toString());
        }
    }

    /**
     * Produces a JSON string representation of the token request for persistent storage or
     * local transmission (e.g. between activities).
     */
    @NonNull
    public JSONObject jsonSerialize() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_CONFIGURATION, configuration.toJson());
        JsonUtil.put(json, KEY_CLIENT_ID, clientId);
        JsonUtil.putIfNotNull(json, KEY_NONCE, nonce);
        JsonUtil.put(json, KEY_GRANT_TYPE, grantType);
        JsonUtil.putIfNotNull(json, KEY_REDIRECT_URI, redirectUri);
        JsonUtil.putIfNotNull(json, KEY_SCOPE, scope);
        JsonUtil.putIfNotNull(json, KEY_AUTHORIZATION_CODE, authorizationCode);
        JsonUtil.putIfNotNull(json, KEY_REFRESH_TOKEN, refreshToken);
        JsonUtil.putIfNotNull(json, KEY_CODE_VERIFIER, codeVerifier);
        JsonUtil.put(json, KEY_ADDITIONAL_PARAMETERS,
                JsonUtil.mapToJsonObject(additionalParameters));
        return json;
    }

    /**
     * Produces a JSON string representation of the token request for persistent storage or
     * local transmission (e.g. between activities). This method is just a convenience wrapper
     * for {@link #jsonSerialize()}, converting the JSON object to its string form.
     */
    @NonNull
    public String jsonSerializeString() {
        return jsonSerialize().toString();
    }

    /**
     * Reads a token request from a JSON string representation produced by
     * {@link #jsonSerialize()}.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static TokenRequest jsonDeserialize(JSONObject json) throws JSONException {
        checkNotNull(json, "json object cannot be null");

        return new TokenRequest(
                AuthorizationServiceConfiguration.fromJson(json.getJSONObject(KEY_CONFIGURATION)),
                JsonUtil.getString(json, KEY_CLIENT_ID),
                JsonUtil.getStringIfDefined(json, KEY_NONCE),
                JsonUtil.getString(json, KEY_GRANT_TYPE),
                JsonUtil.getUriIfDefined(json, KEY_REDIRECT_URI),
                JsonUtil.getStringIfDefined(json, KEY_SCOPE),
                JsonUtil.getStringIfDefined(json, KEY_AUTHORIZATION_CODE),
                JsonUtil.getStringIfDefined(json, KEY_REFRESH_TOKEN),
                JsonUtil.getStringIfDefined(json, KEY_CODE_VERIFIER),
                JsonUtil.getStringMap(json, KEY_ADDITIONAL_PARAMETERS));
    }

    /**
     * Reads a token request from a JSON string representation produced by
     * {@link #jsonSerializeString()}. This method is just a convenience wrapper for
     * {@link #jsonDeserialize(JSONObject)}, converting the JSON string to its JSON object form.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static TokenRequest jsonDeserialize(@NonNull String json) throws JSONException {
        checkNotNull(json, "json string cannot be null");
        return jsonDeserialize(new JSONObject(json));
    }
}
