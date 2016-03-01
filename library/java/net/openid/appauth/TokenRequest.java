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

import static net.openid.appauth.Preconditions.checkMapEntryFullyDefined;
import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;
import static net.openid.appauth.Preconditions.checkNullOrNotEmpty;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An OAuth2 token request. These are used to exchange codes for tokens, or exchange a refresh
 * token for updated tokens.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.3"> "The OAuth 2.0
 * Authorization
 * Framework" (RFC 6749), Section 4.1.3</a>
 */
public class TokenRequest {

    @VisibleForTesting
    static final String KEY_CONFIGURATION = "configuration";
    @VisibleForTesting
    static final String KEY_CLIENT_ID = "clientId";
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
    static final String KEY_ADDITIONAL_PARAMETERS = "additionalParameters";

    @VisibleForTesting
    static final String PARAM_CLIENT_ID = "client_id";

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

    /**
     * The grant type used for exchanging an authorization code for one or more tokens.
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.3"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 4.1.3</a>
     */
    public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

    /**
     * The grant type used when exchanging a refresh token for a new token.
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-6"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 6</a>
     */
    public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    /**
     * The grant type used when requesting an access token using a username and password.
     * This grant type is not directly supported by this library.
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.3.2"> "The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 4.3.2</a>
     */
    public static final String GRANT_TYPE_PASSWORD = "password";

    /**
     * The grant type used when requesting an access token using client credentials, typically
     * TLS client certificates. This grant type is not directly supported by this library.
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.4.2"> "The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 4.4.2</a>
     */
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    /**
     * The service's {@link AuthorizationServiceConfiguration configuration}.
     * This configuration specifies how to connect to a particular OAuth provider.
     * Configurations may be
     * {@link AuthorizationServiceConfiguration#AuthorizationServiceConfiguration(Uri,
     * Uri) created manually}, or
     * {@link AuthorizationServiceConfiguration#fetchFromUrl(Uri,
     * AuthorizationServiceConfiguration.RetrieveConfigurationCallback)
     * via an OpenID Connect Discovery Document}.
     */
    @NonNull
    public final AuthorizationServiceConfiguration configuration;

    /**
     * The client identifier.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4"> "The OAuth 2.0 Authorization
     * Framework" (RFC 6749), Section 4</a>
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.1"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 4.1.1</a>
     */
    @NonNull
    public final String clientId;

    /**
     * The type of token being sent to the token endpoint.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.3">"The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 4.1.3</a>.
     */
    @NonNull
    public final String grantType;

    /**
     * The client's redirect URI. Required if this token request is to exchange an authorization
     * code for one or more tokens, and must be identical to the value specified in the original
     * authorization request.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1.2"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 3.1.2</a>
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.3"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 4.1.3</a>
     */
    @Nullable
    public final Uri redirectUri;

    /**
     * An authorization code to be exchanged for one or more tokens.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.3"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 4.1.3</a>
     */
    @Nullable
    public final String authorizationCode;

    /**
     * A space-delimited set of scopes used to determine the scope of any returned tokens.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3"> "The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 3.3</a>
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-6"> "The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 6</a>
     */
    @Nullable
    public final String scope;

    /**
     * A refresh token to be exchanged for a new token.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-6"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 6</a>
     */
    @Nullable
    public final String refreshToken;

    /**
     * The code verifier that was used to generate the challenge in the original authorization
     * request, if one was used.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7636#section-4.5">"Proof Key for Code Exchange
     * by OAuth Public Clients" (RFC 7636), Section 4.5</a>
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
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3"> "The OAuth 2.0
         * Authorization
         * Framework" (RFC 6749), Section 3.3</a>
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
         * <p>Scopes specified here are used to obtain a "down-scoped" access token, where the
         * set of scopes specified <em>must</em> be a subset of those already granted in
         * previous requests.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3"> "The OAuth 2.0
         * Authorization Framework" (RFC 6749), Section 3.3</a>
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-6"> "The OAuth 2.0
         * Authorization Framework" (RFC 6749), Section 6</a>
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
         * <p>Scopes specified here are used to obtain a "down-scoped" access token, where the
         * set of scopes specified <em>must</em> be a subset of those already granted in
         * previous requests.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3"> "The OAuth 2.0
         * Authorization
         * Framework" (RFC 6749), Section 3.3</a>
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-6"> "The OAuth 2.0
         * Authorization Framework" (RFC 6749), Section 6</a>
         */
        @NonNull
        public Builder setScopes(@Nullable Iterable<String> scopes) {
            mScope = ScopeUtil.scopeIterableToString(scopes);
            return this;
        }

        /**
         * Specifies the authorization code for the request. If provided, the authorization code
         * must not be empty.
         *
         * <p>Specifying an authorization code normally implies that this is a request to exchange
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
         * <p>Specifying a refresh token normally implies that this is a request to exchange the
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
            mAdditionalParameters = new LinkedHashMap<>();
            if (additionalParameters == null) {
                return this;
            }

            for (Entry<String, String> entry : additionalParameters.entrySet()) {
                checkMapEntryFullyDefined(entry,
                        "extra parameters must have non-null keys and non-null values");
                // TODO: check that the key name does not conflict with any "core" field names.
                mAdditionalParameters.put(entry.getKey(), entry.getValue());
            }

            return this;
        }

        /**
         * Produces a {@link TokenRequest} instance, if all necessary values have been provided.
         */
        @NonNull
        public TokenRequest build() {
            String grantType = inferGrantType();

            if (GRANT_TYPE_AUTHORIZATION_CODE.equals(grantType)) {
                checkNotNull(mAuthorizationCode,
                        "authorization code must be specified for grant_type = "
                                + GRANT_TYPE_AUTHORIZATION_CODE);
            }

            if (GRANT_TYPE_REFRESH_TOKEN.equals(grantType)) {
                checkNotNull(mRefreshToken,
                        "refresh token must be specified for grant_type = "
                                + GRANT_TYPE_REFRESH_TOKEN);
            }


            if (grantType.equals(GRANT_TYPE_AUTHORIZATION_CODE) && mRedirectUri == null) {
                throw new IllegalStateException(
                        "no redirect URI specified on token request for code exchange");
            }

            return new TokenRequest(
                    mConfiguration,
                    mClientId,
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
                return GRANT_TYPE_AUTHORIZATION_CODE;
            } else if (mRefreshToken != null) {
                return GRANT_TYPE_REFRESH_TOKEN;
            } else {
                throw new IllegalStateException("grant type not specified and cannot be inferred");
            }
        }
    }

    private TokenRequest(
            @NonNull AuthorizationServiceConfiguration configuration,
            @NonNull String clientId,
            @NonNull String grantType,
            @Nullable Uri redirectUri,
            @Nullable String scope,
            @Nullable String authorizationCode,
            @Nullable String refreshToken,
            @Nullable String codeVerifier,
            @NonNull Map<String, String> additionalParameters) {
        this.configuration = configuration;
        this.clientId = clientId;
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
     * return {@code null}.
     */
    @Nullable
    public Set<String> getScopeSet() {
        return ScopeUtil.scopeStringToSet(scope);
    }

    /**
     * Produces a request URI, that can be used to dispatch the token request.
     */
    @NonNull
    public Uri toUri() {
        Uri.Builder uriBuilder = configuration.tokenEndpoint.buildUpon()
                .appendQueryParameter(PARAM_GRANT_TYPE, grantType)
                .appendQueryParameter(PARAM_CLIENT_ID, clientId);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_REDIRECT_URI, redirectUri);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_CODE, authorizationCode);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_REFRESH_TOKEN, refreshToken);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_CODE_VERIFIER, codeVerifier);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_SCOPE, scope);

        for (Entry<String, String> param : additionalParameters.entrySet()) {
            uriBuilder.appendQueryParameter(param.getKey(), param.getValue());
        }

        return uriBuilder.build();
    }

    /**
     * Converts the token request to JSON for storage or transmission.
     */
    @NonNull
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_CONFIGURATION, configuration.toJson());
        JsonUtil.put(json, KEY_CLIENT_ID, clientId);
        JsonUtil.put(json, KEY_GRANT_TYPE, grantType);
        JsonUtil.putIfNotNull(json, KEY_REDIRECT_URI, redirectUri);
        JsonUtil.putIfNotNull(json, KEY_SCOPE, scope);
        JsonUtil.putIfNotNull(json, KEY_AUTHORIZATION_CODE, authorizationCode);
        JsonUtil.putIfNotNull(json, KEY_REFRESH_TOKEN, refreshToken);
        JsonUtil.put(json, KEY_ADDITIONAL_PARAMETERS,
                JsonUtil.mapToJsonObject(additionalParameters));
        return json;
    }

    /**
     * Converts the authorization request to a JSON string for storage or transmission.
     */
    @NonNull
    public String toJsonString() {
        return toJson().toString();
    }

    /**
     * Reads a token request from a JSON string representation produced by the
     * {@link #toJson()} method or some other equivalent producer.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static TokenRequest fromJson(@NonNull String json) throws JSONException {
        checkNotNull(json, "json string cannot be null");
        return fromJson(new JSONObject(json));
    }

    /**
     * Reads a token request from a JSON representation produced by the
     * {@link #toJson()} method or some other equivalent producer.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static TokenRequest fromJson(JSONObject json) throws JSONException {
        checkNotNull(json, "json object cannot be null");

        TokenRequest.Builder builder = new TokenRequest.Builder(
                AuthorizationServiceConfiguration.fromJson(json.getJSONObject(KEY_CONFIGURATION)),
                JsonUtil.getString(json, KEY_CLIENT_ID))
                .setRedirectUri(JsonUtil.getUriIfDefined(json, KEY_REDIRECT_URI))
                .setGrantType(JsonUtil.getString(json, KEY_GRANT_TYPE))
                .setRefreshToken(JsonUtil.getStringIfDefined(json, KEY_REFRESH_TOKEN))
                .setAuthorizationCode(JsonUtil.getStringIfDefined(json, KEY_AUTHORIZATION_CODE))
                .setAdditionalParameters(JsonUtil.getStringMap(json, KEY_ADDITIONAL_PARAMETERS));

        if (json.has(KEY_SCOPE)) {
            builder.setScopes(ScopeUtil.scopeStringToSet(JsonUtil.getString(json, KEY_SCOPE)));
        }

        return builder.build();
    }
}
