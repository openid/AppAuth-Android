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

import static net.openid.appauth.Preconditions.checkArgument;
import static net.openid.appauth.Preconditions.checkNotNull;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An OAuth2 authorization request.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4"> "The OAuth 2.0 Authorization
 * Framework" (RFC 6749), Section 4</a>
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.1"> "The OAuth 2.0 Authorization
 * Framework" (RFC 6749), Section 4.1.1</a>
 */
public class AuthorizationRequest {

    /**
     * Instructs the authorization server to send response parameters using
     * the query portion of the redirect URI.
     * @see <a href="http://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#ResponseModes">
     * "OAuth 2.0 Multiple Response Type Encoding Practices", Section 2.1</a>
     */
    public static final String RESPONSE_MODE_QUERY = "query";

    /**
     * Instructs the authorization server to send response parameters using
     * the fragment portion of the redirect URI.
     * @see <a href="http://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#ResponseModes">
     * "OAuth 2.0 Multiple Response Type Encoding Practices", Section 2.1</a>
     */
    public static final String RESPONSE_MODE_FRAGMENT = "fragment";

    /**
     * For requesting an authorization code.
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1.1"> "The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 3.1.1</a>
     */
    public static final String RESPONSE_TYPE_CODE = "code";

    /**
     * For requesting an access token via an implicit grant.
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1.1"> "The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 3.1.1</a>
     */
    public static final String RESPONSE_TYPE_TOKEN = "token";

    /**
     * A scope for OpenID based authorization.
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthRequest">"OpenID
     * Connect Core 1.0", Section 3.1.2.1</a>
     */
    public static final String SCOPE_OPENID = "openid";

    /**
     * A scope for the authenticated user's basic profile information.
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">"OpenID
     * Connect Core 1.0", Section 5.4</a>
     */
    public static final String SCOPE_PROFILE = "profile";

    /**
     * A scope for the authenticated user's email address.
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">"OpenID
     * Connect Core 1.0", Section 5.4</a>
     */
    public static final String SCOPE_EMAIL = "email";

    /**
     * A scope for the authenticated user's mailing address.
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">"OpenID
     * Connect Core 1.0", Section 5.4</a>
     */
    public static final String SCOPE_ADDRESS = "address";

    /**
     * A scope for the authenticated user's phone number.
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">"OpenID
     * Connect Core 1.0", Section 5.4</a>
     */
    public static final String SCOPE_PHONE = "phone";

    /**
     * SHA-256 based code verifier challenge method.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7636#section-4.3">"Proof Key for Code Exchange
     * by OAuth Public Clients" (RFC 7636), Section 4.4</a>
     */
    public static final String CODE_CHALLENGE_METHOD_S256 = "S256";

    /**
     * Plain-text code verifier challenge method. This is only used by AppAuth for Android if
     * SHA-256 is not supported on this platform.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7636#section-4.3">"Proof Key for Code Exchange
     * by OAuth Public Clients" (RFC 7636), Section 4.4</a>
     */
    public static final String CODE_CHALLENGE_METHOD_PLAIN = "plain";

    @VisibleForTesting
    static final String PARAM_CLIENT_ID = "client_id";

    @VisibleForTesting
    static final String PARAM_CODE_CHALLENGE = "code_challenge";

    @VisibleForTesting
    static final String PARAM_CODE_CHALLENGE_METHOD = "code_challenge_method";

    @VisibleForTesting
    static final String PARAM_REDIRECT_URI = "redirect_uri";

    @VisibleForTesting
    static final String PARAM_RESPONSE_MODE = "response_mode";

    @VisibleForTesting
    static final String PARAM_RESPONSE_TYPE = "response_type";

    @VisibleForTesting
    static final String PARAM_SCOPE = "scope";

    @VisibleForTesting
    static final String PARAM_STATE = "state";

    private static final String KEY_CONFIGURATION = "configuration";
    private static final String KEY_CLIENT_ID = "clientId";
    private static final String KEY_RESPONSE_TYPE = "responseType";
    private static final String KEY_REDIRECT_URI = "redirectUri";
    private static final String KEY_SCOPE = "scope";
    private static final String KEY_STATE = "state";
    private static final String KEY_CODE_VERIFIER = "codeVerifier";
    private static final String KEY_CODE_VERIFIER_CHALLENGE = "codeVerifierChallenge";
    private static final String KEY_CODE_VERIFIER_CHALLENGE_METHOD = "codeVerifierChallengeMethod";
    private static final String KEY_RESPONSE_MODE = "responseMode";
    private static final String KEY_ADDITIONAL_PARAMETERS = "additionalParameters";
    private static final int STATE_LENGTH = 16;

    /**
     * The service's {@link AuthorizationServiceConfiguration configuration}.
     * This configuration specifies how to connect to a particular OAuth provider.
     * Configurations may be
     * {@link AuthorizationServiceConfiguration#AuthorizationServiceConfiguration(Uri, Uri)}
     * created manually}, or {@link AuthorizationServiceConfiguration#fetchFromUrl(Uri,
     * AuthorizationServiceConfiguration.RetrieveConfigurationCallback)} via an OpenID Connect
     * Discovery Document}.
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
     * The expected response type.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1.1"> "The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 3.1.1</a>
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3"> "OpenID
     * Connect Core 1.0", Section 3</a>
     */
    @NonNull
    public final String responseType;

    /**
     * The client's redirect URI.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1.2"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 3.1.2</a>
     */
    @NonNull
    public final Uri redirectUri;

    /**
     * The optional set of scopes expressed as a space-delimited, case-sensitive string.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3"> "The OAuth 2.0 Authorization
     * Framework" (RFC 6749), Section 3.3</a>
     */
    @Nullable
    public final String scope;

    /**
     * An opaque value used by the client to maintain state between the request and callback. If
     * this value is not explicitly set, this library will automatically add state and perform
     * appropriate  validation of the state in the authorization response. It is recommended that
     * the default implementation of this parameter be used wherever possible. Typically used to
     * prevent CSRF attacks, as recommended in
     * <a href="https://tools.ietf.org/html/rfc6819#section-5.3.5">RFC6819 Section 5.3.5</a>.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.1"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 4.1.1</a>
     * @see <a href="https://tools.ietf.org/html/rfc6819#section-5.3.5"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 5.3.5</a>
     */
    @Nullable
    public final String state;

    /**
     * The proof key for code exchange. This is an opaque value used to associate an authorization
     * request with a subsequent code exchange, in order to prevent any eavesdropping party from
     * intercepting and using the code before the original requestor. If PKCE is disabled due to
     * a non-compliant authorization server which rejects requests with PKCE parameters present,
     * this value will be {@code null}.
     *
     * @see Builder#setCodeVerifier(String)
     * @see Builder#setCodeVerifier(String, String, String)
     * @see <a href="https://tools.ietf.org/html/rfc7636">"Proof Key for Code Exchange by OAuth
     * Public Clients" (RFC 7636)</a>
     */
    @Nullable
    public final String codeVerifier;

    /**
     * The challenge derived from the {@link #codeVerifier code verifier}, using the
     * {@link #codeVerifierChallengeMethod challenge method}. If a code verifier is not being
     * used for this request, this value will be {@code null}.
     *
     * @see Builder#setCodeVerifier(String)
     * @see Builder#setCodeVerifier(String, String, String)
     * @see <a href="https://tools.ietf.org/html/rfc7636">"Proof Key for Code Exchange by OAuth
     * Public Clients" (RFC 7636)</a>
     */
    @Nullable
    public final String codeVerifierChallenge;

    /**
     * The challenge method used to generate a {@link #codeVerifierChallenge challenge} from
     * the {@link #codeVerifier code verifier}. If a code verifier is not being used for this
     * request, this value will be {@code null}.
     *
     * @see Builder#setCodeVerifier(String)
     * @see Builder#setCodeVerifier(String, String, String)
     * @see <a href="https://tools.ietf.org/html/rfc7636">"Proof Key for Code Exchange by OAuth
     * Public Clients" (RFC 7636)</a>
     */
    @Nullable
    public final String codeVerifierChallengeMethod;

    /**
     * Instructs the authorization service on the mechanism to be used for returning
     * response parameters from the authorization endpoint. This use of this parameter is
     * <em>not recommended</em> when the response mode that would be requested is the default mode
     * specified for the response type.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthRequest">"OpenID
     * Connect Core 1.0", Section 3.1.2.1</a>
     */
    @Nullable
    public final String responseMode;

    /**
     * Additional parameters to be passed as part of the request.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1"> "The OAuth 2.0 Authorization
     * Framework" (RFC 6749), Section 3.1</a>
     */
    @NonNull
    public final Map<String, String> additionalParameters;

    /**
     * Creates instances of {@link AuthorizationRequest}.
     */
    public static final class Builder {

        @NonNull
        private AuthorizationServiceConfiguration mConfiguration;

        @NonNull
        private String mClientId;

        @NonNull
        private String mResponseType;

        @NonNull
        private Uri mRedirectUri;

        @Nullable
        private String mScope;

        @Nullable
        private String mState;

        @Nullable
        private String mCodeVerifier;

        @Nullable
        private String mCodeVerifierChallenge;

        @Nullable
        private String mCodeVerifierChallengeMethod;

        @Nullable
        private String mResponseMode;

        @NonNull
        private HashMap<String, String> mAdditionalParameters = new HashMap<>();

        /**
         * Creates an authorization request builder with the specified mandatory properties.
         */
        public Builder(
                @NonNull AuthorizationServiceConfiguration configuration,
                @NonNull String clientId,
                @NonNull String responseType,
                @NonNull Uri redirectUri) {
            setAuthorizationServiceConfiguration(configuration);
            setClientId(clientId);
            setResponseType(responseType);
            setRedirectUri(redirectUri);
            setState(AuthorizationRequest.generateRandomState());
            setCodeVerifier(CodeVerifierUtil.generateRandomCodeVerifier());
        }

        /**
         * Specifies the service configuration to be used in dispatching this request.
         */
        public Builder setAuthorizationServiceConfiguration(
                @NonNull AuthorizationServiceConfiguration configuration) {
            mConfiguration = Preconditions.checkNotNull(configuration,
                    "configuration cannot be null");
            return this;
        }

        /**
         * Specifies the client ID. Cannot be null or empty.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-4"> "The OAuth 2.0
         * Authorization
         * Framework" (RFC 6749), Section 4</a>
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.1"> "The OAuth 2.0
         * Authorization Framework" (RFC 6749), Section 4.1.1</a>
         */
        @NonNull
        public Builder setClientId(@NonNull String clientId) {
            checkArgument(!TextUtils.isEmpty(clientId), "client ID cannot be null or empty");
            mClientId = clientId;
            return this;
        }

        /**
         * Specifies the expected response type. Cannot be null or empty.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-2.2"> "The OAuth 2.0
         * Authorization
         * Framework" (RFC 6749), Section 2.2</a>
         * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3">
         * "OpenID
         * Connect Core 1.0", Section 3</a>
         */
        @NonNull
        public Builder setResponseType(@NonNull String responseType) {
            checkArgument(!TextUtils.isEmpty(responseType),
                    "expected response type cannot be null or empty");
            mResponseType = responseType;
            return this;
        }

        /**
         * Specifies the client's redirect URI. Cannot be null or empty.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1.2"> "The OAuth 2.0
         * Authorization Framework" (RFC 6749), Section 3.1.2</a>
         */
        @NonNull
        public Builder setRedirectUri(@NonNull Uri redirectUri) {
            mRedirectUri = checkNotNull(redirectUri, "redirect URI cannot be null or empty");
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
         * scopes. If no arguments are provided, the scope string will be set to {@code null}.
         * Individual scope strings cannot be null or empty.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3"> "The OAuth 2.0
         * Authorization
         * Framework" (RFC 6749), Section 3.3</a>
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
         * scopes. If the iterable is empty, the scope string will be set to {@code null}.
         * Individual scope strings cannot be null or empty.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3"> "The OAuth 2.0
         * Authorization
         * Framework" (RFC 6749), Section 3.3</a>
         */
        @NonNull
        public Builder setScopes(@Nullable Iterable<String> scopes) {
            mScope = ScopeUtil.scopeIterableToString(scopes);
            return this;
        }

        /**
         * Specifies the opaque value used by the client to maintain state between the request and
         * callback. If this value is not explicitly set, this library will automatically add state
         * and perform appropriate validation of the state in the authorization response. It is
         * recommended that the default implementation of this parameter be used wherever possible.
         * Typically used to prevent CSRF attacks, as recommended in
         * <a href="https://tools.ietf.org/html/rfc6819#section-5.3.5">RFC6819 Section 5.3.5</a>.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.1"> "The OAuth 2.0
         * Authorization Framework" (RFC 6749), Section 4.1.1</a>
         * @see <a href="https://tools.ietf.org/html/rfc6819#section-5.3.5"> "The OAuth 2.0
         * Authorization Framework" (RFC 6749), Section 5.3.5</a>
         */
        @NonNull
        public Builder setState(@Nullable String state) {
            if (state != null) {
                Preconditions.checkArgument(!TextUtils.isEmpty(state),
                        "state cannot be empty if defined");
            }
            mState = state;
            return this;
        }

        /**
         * Specifies the code verifier to use for this authorization request. The default challenge
         * method (typically {@link #CODE_CHALLENGE_METHOD_S256}) implemented by
         * {@link CodeVerifierUtil} will be used, and a challenge will be generated using this
         * method. If the use of a code verifier is not desired, set the code verifier
         * to {@code null}.
         */
        @NonNull
        public Builder setCodeVerifier(@Nullable String codeVerifier) {
            if (codeVerifier != null) {
                CodeVerifierUtil.checkCodeVerifier(codeVerifier);
                mCodeVerifier = codeVerifier;
                mCodeVerifierChallenge = CodeVerifierUtil.deriveCodeVerifierChallenge(codeVerifier);
                mCodeVerifierChallengeMethod = CodeVerifierUtil.getCodeVerifierChallengeMethod();
            } else {
                mCodeVerifier = null;
                mCodeVerifierChallenge = null;
                mCodeVerifierChallengeMethod = null;
            }

            return this;
        }

        /**
         * Specifies the code verifier, challenge and method strings to use for this authorization
         * request. If these values are not explicitly set, they will be automatically generated
         * and used. It is recommended that this default behavior be used wherever possible. If
         * a null code verifier is set (to indicate that a code verifier is not to be used), then
         * the challenge and method must also be null. If a non-null code verifier is set, the
         * code verifier challenge and method must also be set.
         *
         * @see <a href="https://tools.ietf.org/html/rfc7636">"Proof Key for Code Exchange by OAuth
         * Public Clients" (RFC 7636)</a>
         */
        @NonNull
        public Builder setCodeVerifier(
                @Nullable String codeVerifier,
                @Nullable String codeVerifierChallenge,
                @Nullable String codeVerifierChallengeMethod) {
            if (codeVerifier != null) {
                CodeVerifierUtil.checkCodeVerifier(codeVerifier);
                Preconditions.checkNotEmpty(codeVerifierChallenge,
                        "code verifier challenge cannot be null or empty if verifier is set");
                Preconditions.checkNotEmpty(codeVerifierChallengeMethod,
                        "code verifier challenge method cannot be null or empty if verifier "
                                + "is set");
            } else {
                Preconditions.checkArgument(codeVerifierChallenge == null,
                        "code verifier challenge must be null if verifier is null");
                Preconditions.checkArgument(codeVerifierChallengeMethod == null,
                        "code verifier challenge method must be null if verifier is null");
            }

            mCodeVerifier = codeVerifier;
            mCodeVerifierChallenge = codeVerifierChallenge;
            mCodeVerifierChallengeMethod = codeVerifierChallengeMethod;

            return this;
        }

        /**
         * Specifies the response mode to be used for returning authorization response parameters
         * from the authorization endpoint.
         * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthRequest">"OpenID
         * Connect Core 1.0", Section 3.1.2.1</a>
         * @see <a href="http://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#ResponseTypesAndModes">
         * "OAuth 2.0 Multiple Response Type Encoding Practices", Section 2</a>
         */
        @NonNull
        public Builder setResponseMode(@Nullable String responseMode) {
            Preconditions.checkNullOrNotEmpty(responseMode, "responseMode must not be empty");
            mResponseMode = responseMode;
            return this;
        }

        /**
         * Specifies additional parameters. Replaces any previously provided set of parameters.
         * Parameter keys and values cannot be null or empty.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1"> "The OAuth 2.0
         * Authorization
         * Framework" (RFC 6749), Section 3.1</a>
         */
        @NonNull
        public Builder setAdditionalParameters(@NonNull Map<String, String> additionalParameters) {
            checkNotNull(additionalParameters);
            mAdditionalParameters = new HashMap<>();
            for (Entry<String, String> entry : additionalParameters.entrySet()) {
                Preconditions.checkMapEntryFullyDefined(entry,
                        "additional parameters must have non-null keys and non-null values");
                // TODO: check that the key name does not conflict with any "core" field names.
                mAdditionalParameters.put(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * Constructs the authorization request. At a minimum the following fields must have been
         * set:
         * <ul> <li>The client ID</li> <li>The expected response type</li> <li>The redirect
         * URI</li>
         * </ul> Failure to specify any of these parameters will result in a runtime exception.
         */
        @NonNull
        public AuthorizationRequest build() {
            return new AuthorizationRequest(
                    mConfiguration,
                    mClientId,
                    mResponseType,
                    mRedirectUri,
                    mScope,
                    mState,
                    mCodeVerifier,
                    mCodeVerifierChallenge,
                    mCodeVerifierChallengeMethod,
                    mResponseMode,
                    Collections.unmodifiableMap(new HashMap<>(mAdditionalParameters)));
        }
    }

    private AuthorizationRequest(
            @NonNull AuthorizationServiceConfiguration configuration,
            @NonNull String clientId,
            @NonNull String responseType,
            @NonNull Uri redirectUri,
            @Nullable String scope,
            @Nullable String state,
            @Nullable String codeVerifier,
            @Nullable String codeVerifierChallenge,
            @Nullable String codeVerifierChallengeMethod,
            @Nullable String responseMode,
            @NonNull Map<String, String> additionalParameters) {
        this.configuration = configuration;
        this.clientId = clientId;
        this.responseType = responseType;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.state = state;
        this.codeVerifier = codeVerifier;
        this.codeVerifierChallenge = codeVerifierChallenge;
        this.codeVerifierChallengeMethod = codeVerifierChallengeMethod;
        this.responseMode = responseMode;
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
     * Produces a request URI, that can be used to dispath the authorization request.
     */
    @NonNull
    public Uri toUri() {
        Uri.Builder uriBuilder = configuration.authorizationEndpoint.buildUpon()
                .appendQueryParameter(PARAM_REDIRECT_URI, redirectUri.toString())
                .appendQueryParameter(PARAM_CLIENT_ID, clientId)
                .appendQueryParameter(PARAM_RESPONSE_TYPE, responseType);

        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_STATE, state);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_SCOPE, scope);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_RESPONSE_MODE, responseMode);

        if (codeVerifier != null) {
            uriBuilder.appendQueryParameter(PARAM_CODE_CHALLENGE, codeVerifierChallenge)
                    .appendQueryParameter(PARAM_CODE_CHALLENGE_METHOD, codeVerifierChallengeMethod);
        }

        for (Entry<String, String> entry : additionalParameters.entrySet()) {
            uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
        }

        return uriBuilder.build();
    }

    /**
     * Produces a JSON representation of the request for storage or transmission.
     */
    @NonNull
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_CONFIGURATION, configuration.toJson());
        JsonUtil.put(json, KEY_CLIENT_ID, clientId);
        JsonUtil.put(json, KEY_RESPONSE_TYPE, responseType);
        JsonUtil.put(json, KEY_REDIRECT_URI, redirectUri.toString());
        JsonUtil.putIfNotNull(json, KEY_SCOPE, scope);
        JsonUtil.putIfNotNull(json, KEY_STATE, state);
        JsonUtil.putIfNotNull(json, KEY_CODE_VERIFIER, codeVerifier);
        JsonUtil.putIfNotNull(json, KEY_CODE_VERIFIER_CHALLENGE, codeVerifierChallenge);
        JsonUtil.putIfNotNull(json, KEY_CODE_VERIFIER_CHALLENGE_METHOD,
                codeVerifierChallengeMethod);
        JsonUtil.putIfNotNull(json, KEY_RESPONSE_MODE, responseMode);
        JsonUtil.put(json, KEY_ADDITIONAL_PARAMETERS,
                JsonUtil.mapToJsonObject(additionalParameters));
        return json;
    }

    /**
     * Produces a JSON string representation of the request for storage or transmission.
     */
    public String toJsonString() {
        return toJson().toString();
    }

    /**
     * Reads an Authorization request from a JSON string representation produced by
     * {@link #toJsonString()}.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static AuthorizationRequest fromJson(@NonNull String jsonStr) throws JSONException {
        checkNotNull(jsonStr, "json string cannot be null");
        return fromJson(new JSONObject(jsonStr));
    }

    /**
     * Reads an Authorization request from a JSON representation produced by
     * {@link #toJson()}.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static AuthorizationRequest fromJson(@NonNull JSONObject json) throws JSONException {
        checkNotNull(json, "json cannot be null");
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                AuthorizationServiceConfiguration.fromJson(json.getJSONObject(KEY_CONFIGURATION)),
                JsonUtil.getString(json, KEY_CLIENT_ID),
                JsonUtil.getString(json, KEY_RESPONSE_TYPE),
                JsonUtil.getUri(json, KEY_REDIRECT_URI))
                .setState(JsonUtil.getStringIfDefined(json, KEY_STATE))
                .setCodeVerifier(
                        JsonUtil.getStringIfDefined(json, KEY_CODE_VERIFIER),
                        JsonUtil.getStringIfDefined(json, KEY_CODE_VERIFIER_CHALLENGE),
                        JsonUtil.getStringIfDefined(json, KEY_CODE_VERIFIER_CHALLENGE_METHOD))
                .setResponseMode(JsonUtil.getStringIfDefined(json, KEY_RESPONSE_MODE))
                .setAdditionalParameters(JsonUtil.getStringMap(json, KEY_ADDITIONAL_PARAMETERS));

        if (json.has(KEY_SCOPE)) {
            builder.setScopes(ScopeUtil.scopeStringToSet(JsonUtil.getString(json, KEY_SCOPE)));
        }
        return builder.build();
    }

    private static String generateRandomState() {
        SecureRandom sr = new SecureRandom();
        byte[] random = new byte[STATE_LENGTH];
        sr.nextBytes(random);
        return Base64.encodeToString(random, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
    }
}
