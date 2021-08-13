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

import static net.openid.appauth.AdditionalParamsProcessor.builtInParams;
import static net.openid.appauth.AdditionalParamsProcessor.checkAdditionalParams;
import static net.openid.appauth.Preconditions.checkArgument;
import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;
import static net.openid.appauth.Preconditions.checkNullOrNotEmpty;

import android.net.Uri;
import android.text.TextUtils;
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
import java.util.Map.Entry;
import java.util.Set;

/**
 * An OAuth2 authorization request.
 *
 * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4
 * <https://tools.ietf.org/html/rfc6749#section-4>"
 * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.1
 * <https://tools.ietf.org/html/rfc6749#section-4.1.1>"
 */
public class AuthorizationRequest implements AuthorizationManagementRequest {

    /**
     * SHA-256 based code verifier challenge method.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4.3
     * <https://tools.ietf.org/html/rfc7636#section-4.3>"
     */
    public static final String CODE_CHALLENGE_METHOD_S256 = "S256";

    /**
     * Plain-text code verifier challenge method. This is only used by AppAuth for Android if
     * SHA-256 is not supported on this platform.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4.4
     * <https://tools.ietf.org/html/rfc7636#section-4.4>"
     */
    public static final String CODE_CHALLENGE_METHOD_PLAIN = "plain";

    /**
     * All spec-defined values for the OpenID Connect 1.0 `display` parameter.
     *
     * @see Builder#setDisplay(String)
     * @see "OpenID Connect Core 1.0, Section 3.1.2.1
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
     */
    // SuppressWarnings justification: the constants defined are not directly used by the library,
    // existing only for convenience of the developer.
    @SuppressWarnings("unused")
    public static final class Display {

        /**
         * The Authorization Server _SHOULD_ display the authentication and consent UI
         * consistent with a full User Agent page view. If the display parameter is not specified,
         * this is the default display mode.
         */
        public static final String PAGE = "page";

        /**
         * The Authorization Server _SHOULD_ display the authentication and consent UI
         * consistent with a popup User Agent window. The popup User Agent window should be of an
         * appropriate size for a login-focused dialog and should not obscure the entire window that
         * it is popping up over.
         */
        public static final String POPUP = "popup";

        /**
         * The Authorization Server _SHOULD_ display the authentication and consent UI
         * consistent with a device that leverages a touch interface.
         */
        public static final String TOUCH = "touch";

        /**
         * The Authorization Server _SHOULD_ display the authentication and consent UI
         * consistent with a "feature phone" type display.
         */
        public static final String WAP = "wap";
    }

    /**
     * All spec-defined values for the OpenID Connect 1.0 `prompt` parameter.
     *
     * @see Builder#setPrompt(String)
     * @see "OpenID Connect Core 1.0, Section 3.1.2.1
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
     */
    // SuppressWarnings justification: the constants defined are not directly used by the library,
    // existing only for convenience of the developer.
    @SuppressWarnings("unused")
    public static final class Prompt {

        /**
         * The Authorization Server _MUST NOT_ display any authentication or consent user
         * interface pages. An error is returned if an End-User is not already authenticated or the
         * Client does not have pre-configured consent for the requested Claims or does not fulfill
         * other conditions for processing the request. The error code will typically be
         * `login_required`, `interaction_required`, or another code defined in
         * [OpenID Connect Core 1.0, Section 3.1.2.6](
         * https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.6). This can be
         * used as a method to check for existing authentication and/or consent.
         *
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         * @see "OpenID Connect Core 1.0, Section 3.1.2.6
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.6>"
         */
        public static final String NONE = "none";

        /**
         * The Authorization Server _SHOULD_ prompt the End-User for re-authentication. If
         * it cannot re-authenticate the End-User, it _MUST_ return an error, typically
         * `login_required`.
         *
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         * @see "OpenID Connect Core 1.0, Section 3.1.2.6
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.6>"
         */
        public static final String LOGIN = "login";

        /**
         * The Authorization Server _SHOULD_ prompt the End-User for consent before
         * returning information to the Client. If it cannot obtain consent, it _MUST_
         * return an error, typically `consent_required`.
         *
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         * @see "OpenID Connect Core 1.0, Section 3.1.2.6
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.6>"
         */
        public static final String CONSENT = "consent";

        /**
         * The Authorization Server _SHOULD_ prompt the End-User to select a user account.
         * This enables an End-User who has multiple accounts at the Authorization Server to select
         * amongst the multiple accounts that they might have current sessions for. If it cannot
         * obtain an account selection choice made by the End-User, it MUST return an error,
         * typically `account_selection_required`.
         *
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         * @see "OpenID Connect Core 1.0, Section 3.1.2.6
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.6>"
         */
        public static final String SELECT_ACCOUNT = "select_account";
    }

    /**
     * All spec-defined values for the OAuth2 / OpenID Connect 1.0 `scope` parameter.
     *
     * @see Builder#setScope(String)
     * @see "OpenID Connect Core 1.0, Section 5.4
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.5.4>"
     */
    // SuppressWarnings justification: the constants defined are not directly used by the library,
    // existing only for convenience of the developer.
    @SuppressWarnings("unused")
    public static final class Scope {
        /**
         * A scope for the authenticated user's mailing address.
         *
         * @see "OpenID Connect Core 1.0, Section 5.4
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.5.4>"
         */
        public static final String ADDRESS = "address";

        /**
         * A scope for the authenticated user's email address.
         *
         * @see "OpenID Connect Core 1.0, Section 5.4
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.5.4>"
         */
        public static final String EMAIL = "email";

        /**
         * A scope for requesting an OAuth 2.0 refresh token to be issued, that can be used to
         * obtain an Access Token that grants access to the End-User's UserInfo Endpoint even
         * when the End-User is not present (not logged in).
         *
         * @see "OpenID Connect Core 1.0, Section 11
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.11>"
         */
        public static final String OFFLINE_ACCESS = "offline_access";

        /**
         * A scope for OpenID based authorization.
         *
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         */
        public static final String OPENID = "openid";

        /**
         * A scope for the authenticated user's phone number.
         *
         * @see "OpenID Connect Core 1.0, Section 5.4
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.5.4>"
         */
        public static final String PHONE = "phone";

        /**
         * A scope for the authenticated user's basic profile information.
         *
         * @see "OpenID Connect Core 1.0, Section 5.4
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.5.4>"
         */
        public static final String PROFILE = "profile";
    }

    /**
     * All spec-defined values for the OAuth2 / OpenID Connect `response_mode` parameter.
     *
     * @see Builder#setResponseMode(String)
     * @see "OAuth 2.0 Multiple Response Type Encoding Practices, Section 2.1
     * <http://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#rfc.section.2.1>"
     * @see "OpenID Connect Core 1.0, Section 3.1.2.1
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
     */
    // SuppressWarnings justification: the constants defined are not directly used by the library,
    // existing only for convenience of the developer.
    @SuppressWarnings("unused")
    public static final class ResponseMode {
        /**
         * Instructs the authorization server to send response parameters using
         * the query portion of the redirect URI.
         *
         * @see "OAuth 2.0 Multiple Response Type Encoding Practices, Section 2.1
         * <http://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#rfc.section.2.1>"
         */
        public static final String QUERY = "query";

        /**
         * Instructs the authorization server to send response parameters using
         * the fragment portion of the redirect URI.
         * @see "OAuth 2.0 Multiple Response Type Encoding Practices, Section 2.1
         * <http://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#rfc.section.2.1>"
         */
        public static final String FRAGMENT = "fragment";
    }

    @VisibleForTesting
    static final String PARAM_CLIENT_ID = "client_id";

    @VisibleForTesting
    static final String PARAM_CODE_CHALLENGE = "code_challenge";

    @VisibleForTesting
    static final String PARAM_CODE_CHALLENGE_METHOD = "code_challenge_method";

    @VisibleForTesting
    static final String PARAM_DISPLAY = "display";

    @VisibleForTesting
    static final String PARAM_LOGIN_HINT = "login_hint";

    @VisibleForTesting
    static final String PARAM_PROMPT = "prompt";

    @VisibleForTesting
    static final String PARAM_UI_LOCALES = "ui_locales";

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

    @VisibleForTesting
    static final String PARAM_NONCE = "nonce";

    @VisibleForTesting
    static final String PARAM_CLAIMS = "claims";

    @VisibleForTesting
    static final String PARAM_CLAIMS_LOCALES = "claims_locales";

    private static final Set<String> BUILT_IN_PARAMS = builtInParams(
            PARAM_CLIENT_ID,
            PARAM_CODE_CHALLENGE,
            PARAM_CODE_CHALLENGE_METHOD,
            PARAM_DISPLAY,
            PARAM_LOGIN_HINT,
            PARAM_PROMPT,
            PARAM_UI_LOCALES,
            PARAM_REDIRECT_URI,
            PARAM_RESPONSE_MODE,
            PARAM_RESPONSE_TYPE,
            PARAM_SCOPE,
            PARAM_STATE,
            PARAM_CLAIMS,
            PARAM_CLAIMS_LOCALES);

    private static final String KEY_CONFIGURATION = "configuration";
    private static final String KEY_CLIENT_ID = "clientId";
    private static final String KEY_DISPLAY = "display";
    private static final String KEY_LOGIN_HINT = "login_hint";
    private static final String KEY_PROMPT = "prompt";
    private static final String KEY_UI_LOCALES = "ui_locales";
    private static final String KEY_RESPONSE_TYPE = "responseType";
    private static final String KEY_REDIRECT_URI = "redirectUri";
    private static final String KEY_SCOPE = "scope";
    private static final String KEY_STATE = "state";
    private static final String KEY_NONCE = "nonce";
    private static final String KEY_CODE_VERIFIER = "codeVerifier";
    private static final String KEY_CODE_VERIFIER_CHALLENGE = "codeVerifierChallenge";
    private static final String KEY_CODE_VERIFIER_CHALLENGE_METHOD = "codeVerifierChallengeMethod";
    private static final String KEY_RESPONSE_MODE = "responseMode";
    private static final String KEY_CLAIMS = "claims";
    private static final String KEY_CLAIMS_LOCALES = "claimsLocales";
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
     * The OpenID Connect 1.0 `display` parameter. This is a string that specifies how the
     * Authorization Server displays the authentication and consent user interface pages to the
     * End-User.
     *
     * @see "OpenID Connect Core 1.0, Section 3.1.2.1
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
     */
    @Nullable
    public final String display;


    /**
     * The OpenID Connect 1.0 `login_hint` parameter. This is a string hint to the
     * Authorization Server about the login identifier the End-User might use to log in, typically
     * collected directly from the user in an identifier-first authentication flow.
     *
     * @see "OpenID Connect Core 1.0, Section 3.1.2.1
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
     */
    @Nullable
    public final String loginHint;

    /**
     * The OpenID Connect 1.0 `prompt` parameter. This is a space delimited, case sensitive
     * list of ASCII strings that specifies whether the Authorization Server prompts the End-User
     * for re-authentication and consent.
     *
     * @see Prompt
     * @see "OpenID Connect Core 1.0, Section 3.1.2.1
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
     */
    @Nullable
    public final String prompt;

    /**
     * The OpenID Connect 1.0 `ui_locales` parameter. This is a space-separated list of
     * BCP47 [RFC5646] language tag values, ordered by preference. It represents End-User's
     * preferred languages and scripts for the user interface.
     *
     * @see "OpenID Connect Core 1.0, Section 3.1.2.1
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
     */
    @Nullable
    public final String uiLocales;

    /**
     * The expected response type.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.1
     * <https://tools.ietf.org/html/rfc6749#section-3.1.1>"
     * @see "OpenID Connect Core 1.0, Section 3
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3>"
     */
    @NonNull
    public final String responseType;

    /**
     * The client's redirect URI.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.2
     * <https://tools.ietf.org/html/rfc6749#section-3.1.2>"
     */
    @NonNull
    public final Uri redirectUri;

    /**
     * The optional set of scopes expressed as a space-delimited, case-sensitive string.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.2
     * <https://tools.ietf.org/html/rfc6749#section-3.1.2>"
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.3
     * <https://tools.ietf.org/html/rfc6749#section-3.3>"
     */
    @Nullable
    public final String scope;

    /**
     * An opaque value used by the client to maintain state between the request and callback. If
     * this value is not explicitly set, this library will automatically add state and perform
     * appropriate  validation of the state in the authorization response. It is recommended that
     * the default implementation of this parameter be used wherever possible. Typically used to
     * prevent CSRF attacks, as recommended in
     * [RFC6819 Section 5.3.5](https://tools.ietf.org/html/rfc6819#section-5.3.5).
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.1
     * <https://tools.ietf.org/html/rfc6749#section-4.1.1>"
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 5.3.5
     * <https://tools.ietf.org/html/rfc6749#section-5.3.5>"
     */
    @Nullable
    public final String state;

    /**
     * String value used to associate a Client session with an ID Token, and to mitigate replay
     * attacks. The value is passed through unmodified from the Authentication Request to the ID
     * Token. If this value is not explicitly set, this library will automatically add nonce and
     * perform appropriate validation of the ID Token. It is recommended that the default
     * implementation of this parameter be used wherever possible.
     *
     * @see "OpenID Connect Core 1.0, Section 3.1.2.1
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
     */
    @Nullable
    public final String nonce;

    /**
     * The proof key for code exchange. This is an opaque value used to associate an authorization
     * request with a subsequent code exchange, in order to prevent any eavesdropping party from
     * intercepting and using the code before the original requestor. If PKCE is disabled due to
     * a non-compliant authorization server which rejects requests with PKCE parameters present,
     * this value will be `null`.
     *
     * @see Builder#setCodeVerifier(String)
     * @see Builder#setCodeVerifier(String, String, String)
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636)
     * <https://tools.ietf.org/html/rfc7636>"
     */
    @Nullable
    public final String codeVerifier;

    /**
     * The challenge derived from the {@link #codeVerifier code verifier}, using the
     * {@link #codeVerifierChallengeMethod challenge method}. If a code verifier is not being
     * used for this request, this value will be `null`.
     *
     * @see Builder#setCodeVerifier(String)
     * @see Builder#setCodeVerifier(String, String, String)
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636)
     * <https://tools.ietf.org/html/rfc7636>"
     */
    @Nullable
    public final String codeVerifierChallenge;

    /**
     * The challenge method used to generate a {@link #codeVerifierChallenge challenge} from
     * the {@link #codeVerifier code verifier}. If a code verifier is not being used for this
     * request, this value will be `null`.
     *
     * @see Builder#setCodeVerifier(String)
     * @see Builder#setCodeVerifier(String, String, String)
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636)
     * <https://tools.ietf.org/html/rfc7636>"
     */
    @Nullable
    public final String codeVerifierChallengeMethod;

    /**
     * Instructs the authorization service on the mechanism to be used for returning
     * response parameters from the authorization endpoint. This use of this parameter is
     * _not recommended_ when the response mode that would be requested is the default mode
     * specified for the response type.
     *
     * @see "OpenID Connect Core 1.0, Section 3.1.2.1
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
     */
    @Nullable
    public final String responseMode;

    /**
     * Requests that specific Claims be returned.
     * The value is a JSON object listing the requested Claims.
     *
     * @see "OpenID Connect Core 1.0, Section 5.5
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.5.5>"
     */
    @Nullable
    public final JSONObject claims;

    /**
     * End-User's preferred languages and scripts for Claims being returned, represented as a
     * space-separated list of BCP47 [RFC5646] language tag values, ordered by preference.
     *
     * @see "OpenID Connect Core 1.0, Section 5.2
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.5.2>"
     */
    @Nullable
    public final String claimsLocales;

    /**
     * Additional parameters to be passed as part of the request.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1
     * <https://tools.ietf.org/html/rfc6749#section-3.1>"
     */
    @NonNull
    public final Map<String, String> additionalParameters;

    /**
     * Creates instances of {@link AuthorizationRequest}.
     */
    public static final class Builder {

        // SuppressWarnings justification: static analysis incorrectly determines that this field
        // is not initialized, as it is indirectly initialized by setConfiguration
        @NonNull
        @SuppressWarnings("NullableProblems")
        private AuthorizationServiceConfiguration mConfiguration;

        // SuppressWarnings justification: static analysis incorrectly determines that this field
        // is not initialized, as it is indirectly initialized by setClientId
        @NonNull
        @SuppressWarnings("NullableProblems")
        private String mClientId;

        @Nullable
        private String mDisplay;

        @Nullable
        private String mLoginHint;

        @Nullable
        private String mPrompt;

        @Nullable
        private String mUiLocales;

        // SuppressWarnings justification: static analysis incorrectly determines that this field
        // is not initialized, as it is indirectly initialized by setResponseType
        @NonNull
        @SuppressWarnings("NullableProblems")
        private String mResponseType;

        // SuppressWarnings justification: static analysis incorrectly determines that this field
        // is not initialized, as it is indirectly initialized by setRedirectUri
        @NonNull
        @SuppressWarnings("NullableProblems")
        private Uri mRedirectUri;

        @Nullable
        private String mScope;

        @Nullable
        private String mState;

        @Nullable
        private String mNonce;

        @Nullable
        private String mCodeVerifier;

        @Nullable
        private String mCodeVerifierChallenge;

        @Nullable
        private String mCodeVerifierChallengeMethod;

        @Nullable
        private String mResponseMode;

        @Nullable
        private JSONObject mClaims;

        @Nullable
        private String mClaimsLocales;

        @NonNull
        private Map<String, String> mAdditionalParameters = new HashMap<>();

        /**
         * Creates an authorization request builder with the specified mandatory properties,
         * and preset values for {@link AuthorizationRequest#state},
         * {@link AuthorizationRequest#nonce} and {@link AuthorizationRequest#codeVerifier}.
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
            setState(AuthorizationManagementUtil.generateRandomState());
            setNonce(AuthorizationManagementUtil.generateRandomState());
            setCodeVerifier(CodeVerifierUtil.generateRandomCodeVerifier());
        }

        /**
         * Specifies the service configuration to be used in dispatching this request.
         */
        public Builder setAuthorizationServiceConfiguration(
                @NonNull AuthorizationServiceConfiguration configuration) {
            mConfiguration = checkNotNull(configuration,
                    "configuration cannot be null");
            return this;
        }

        /**
         * Specifies the client ID. Cannot be null or empty.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4
         * <https://tools.ietf.org/html/rfc6749#section-4>"
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.1
         * <https://tools.ietf.org/html/rfc6749#section-4.1.1>"
         */
        @NonNull
        public Builder setClientId(@NonNull String clientId) {
            mClientId = checkNotEmpty(clientId, "client ID cannot be null or empty");
            return this;
        }

        /**
         * Specifies the OpenID Connect 1.0 `display` parameter.
         *
         * @see Display
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         */
        public Builder setDisplay(@Nullable String display) {
            mDisplay = checkNullOrNotEmpty(display, "display must be null or not empty");
            return this;
        }

        /**
         * Specifies the OpenID Connect 1.0 `login_hint` parameter.
         *
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         */
        public Builder setLoginHint(@Nullable String loginHint) {
            mLoginHint = checkNullOrNotEmpty(loginHint, "login hint must be null or not empty");
            return this;
        }

        /**
         * Specifies the encoded OpenID Connect 1.0 `prompt` parameter, which is a
         * space-delimited set of case sensitive ASCII prompt values. Replaces any previously
         * specified prompt values.
         *
         * @see Prompt
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         */
        @NonNull
        public Builder setPrompt(@Nullable String prompt) {
            mPrompt = checkNullOrNotEmpty(prompt, "prompt must be null or non-empty");
            return this;
        }

        /**
         * Specifies the set of OpenID Connect 1.0 `prompt` parameter values, which are
         * space-delimited, case sensitive ASCII prompt values. Replaces any previously
         * specified prompt values.
         *
         * @see Prompt
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         */
        @NonNull
        public Builder setPromptValues(@Nullable String... promptValues) {
            if (promptValues == null) {
                mPrompt = null;
                return this;
            }

            return setPromptValues(Arrays.asList(promptValues));
        }

        /**
         * Specifies the set of OpenID Connect 1.0 `prompt` parameter values, which are
         * space-delimited, case sensitive ASCII prompt values. Replaces any previously
         * specified prompt values.
         *
         * @see Prompt
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         */
        @NonNull
        public Builder setPromptValues(@Nullable Iterable<String> promptValues) {
            mPrompt = AsciiStringListUtil.iterableToString(promptValues);
            return this;
        }

        /**
         * Specifies the OpenID Connect 1.0 `ui_locales` parameter, which is a space-separated list
         * of BCP47 [RFC5646] language tag values, ordered by preference. It represents End-User's
         * preferred languages and scripts for the user interface. Replaces any previously
         * specified ui_locales values.
         *
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         */
        public Builder setUiLocales(@Nullable String uiLocales) {
            mUiLocales = checkNullOrNotEmpty(uiLocales, "uiLocales must be null or not empty");
            return this;
        }

        /**
         * Specifies the OpenID Connect 1.0 `ui_locales` parameter, which is a space-separated list
         * of BCP47 [RFC5646] language tag values, ordered by preference. It represents End-User's
         * preferred languages and scripts for the user interface. Replaces any previously
         * specified ui_locales values.
         *
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         */
        @NonNull
        public Builder setUiLocalesValues(@Nullable String... uiLocalesValues) {
            if (uiLocalesValues == null) {
                mUiLocales = null;
                return this;
            }

            return setUiLocalesValues(Arrays.asList(uiLocalesValues));
        }

        /**
         * Specifies the OpenID Connect 1.0 `ui_locales` parameter, which is a space-separated list
         * of BCP47 [RFC5646] language tag values, ordered by preference. It represents End-User's
         * preferred languages and scripts for the user interface. Replaces any previously
         * specified ui_locales values.
         *
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         */
        @NonNull
        public Builder setUiLocalesValues(@Nullable Iterable<String> uiLocalesValues) {
            mUiLocales = AsciiStringListUtil.iterableToString(uiLocalesValues);
            return this;
        }

        /**
         * Specifies the expected response type. Cannot be null or empty.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 2.2
         * <https://tools.ietf.org/html/rfc6749#section-2.2>"
         * @see "OpenID Connect Core 1.0, Section 3
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3>"
         */
        @NonNull
        public Builder setResponseType(@NonNull String responseType) {
            mResponseType = checkNotEmpty(responseType,
                    "expected response type cannot be null or empty");
            return this;
        }

        /**
         * Specifies the client's redirect URI. Cannot be null or empty.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.2
         * <https://tools.ietf.org/html/rfc6749#section-3.1.2>"
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
         * scopes. If no arguments are provided, the scope string will be set to `null`.
         * Individual scope strings cannot be null or empty.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.3
         * <https://tools.ietf.org/html/rfc6749#section-3.3>"
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
         * scopes. If the iterable is empty, the scope string will be set to `null`.
         * Individual scope strings cannot be null or empty.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.3
         * <https://tools.ietf.org/html/rfc6749#section-3.3>"
         */
        @NonNull
        public Builder setScopes(@Nullable Iterable<String> scopes) {
            mScope = AsciiStringListUtil.iterableToString(scopes);
            return this;
        }

        /**
         * Specifies the opaque value used by the client to maintain state between the request and
         * callback. If this value is not explicitly set, this library will automatically add state
         * and perform appropriate validation of the state in the authorization response. It is
         * recommended that the default implementation of this parameter be used wherever possible.
         * Typically used to prevent CSRF attacks, as recommended in
         * [RFC6819 Section 5.3.5](https://tools.ietf.org/html/rfc6819#section-5.3.5).
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.1
         * <https://tools.ietf.org/html/rfc6749#section-4.1.1>"
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 5.3.5
         * <https://tools.ietf.org/html/rfc6749#section-5.3.5>"
         */
        @NonNull
        public Builder setState(@Nullable String state) {
            mState = checkNullOrNotEmpty(state, "state cannot be empty if defined");
            return this;
        }

        /**
         * Specifies the String value used to associate a Client session with an ID Token, and to
         * mitigate replay attacks. The value is passed through unmodified from the Authentication
         * Request to the ID Token. If this value is not explicitly set, this library will
         * automatically add nonce and perform appropriate validation of the ID Token. It is
         * recommended that the default implementation of this parameter be used wherever possible.
         *
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         */
        @NonNull
        public Builder setNonce(@Nullable String nonce) {
            mNonce = checkNullOrNotEmpty(nonce, "nonce cannot be empty if defined");
            return this;
        }

        /**
         * Specifies the code verifier to use for this authorization request. The default challenge
         * method (typically {@link #CODE_CHALLENGE_METHOD_S256}) implemented by
         * {@link CodeVerifierUtil} will be used, and a challenge will be generated using this
         * method. If the use of a code verifier is not desired, set the code verifier
         * to `null`.
         *
         * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4.3
         * <https://tools.ietf.org/html/rfc7636#section-4.3>"
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
         * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4.3
         * <https://tools.ietf.org/html/rfc7636#section-4.3>"
         */
        @NonNull
        public Builder setCodeVerifier(
                @Nullable String codeVerifier,
                @Nullable String codeVerifierChallenge,
                @Nullable String codeVerifierChallengeMethod) {
            if (codeVerifier != null) {
                CodeVerifierUtil.checkCodeVerifier(codeVerifier);
                checkNotEmpty(codeVerifierChallenge,
                        "code verifier challenge cannot be null or empty if verifier is set");
                checkNotEmpty(codeVerifierChallengeMethod,
                        "code verifier challenge method cannot be null or empty if verifier "
                                + "is set");
            } else {
                checkArgument(codeVerifierChallenge == null,
                        "code verifier challenge must be null if verifier is null");
                checkArgument(codeVerifierChallengeMethod == null,
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
         *
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         * @see "OAuth 2.0 Multiple Response Type Encoding Practices, Section 2
         * <http://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#rfc.section.2>"
         */
        @NonNull
        public Builder setResponseMode(@Nullable String responseMode) {
            checkNullOrNotEmpty(responseMode, "responseMode must not be empty");
            mResponseMode = responseMode;
            return this;
        }

        /**
         * Requests that specific Claims be returned.
         * The value is a JSON object listing the requested Claims.
         *
         * @see "OpenID Connect Core 1.0, Section 5.5
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.5.5>"
         */
        @NonNull
        public Builder setClaims(@Nullable JSONObject claims) {
            mClaims = claims;
            return this;
        }

        /**
         * End-User's preferred languages and scripts for Claims being returned, represented as a
         * space-separated list of BCP47 [RFC5646] language tag values, ordered by preference.
         *
         * @see "OpenID Connect Core 1.0, Section 5.2
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.5.2>"
         */
        public Builder setClaimsLocales(@Nullable String claimsLocales) {
            mClaimsLocales = checkNullOrNotEmpty(
                    claimsLocales,
                    "claimsLocales must be null or not empty");
            return this;
        }

        /**
         * End-User's preferred languages and scripts for Claims being returned, represented as a
         * space-separated list of BCP47 [RFC5646] language tag values, ordered by preference.
         *
         * @see "OpenID Connect Core 1.0, Section 5.2
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.5.2>"
         */
        @NonNull
        public Builder setClaimsLocalesValues(@Nullable String... claimsLocalesValues) {
            if (claimsLocalesValues == null) {
                mClaimsLocales = null;
                return this;
            }

            return setClaimsLocalesValues(Arrays.asList(claimsLocalesValues));
        }

        /**
         * End-User's preferred languages and scripts for Claims being returned, represented as a
         * space-separated list of BCP47 [RFC5646] language tag values, ordered by preference.
         *
         * @see "OpenID Connect Core 1.0, Section 5.2
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.5.2>"
         */
        @NonNull
        public Builder setClaimsLocalesValues(@Nullable Iterable<String> claimsLocalesValues) {
            mClaimsLocales = AsciiStringListUtil.iterableToString(claimsLocalesValues);
            return this;
        }

        /**
         * Specifies additional parameters. Replaces any previously provided set of parameters.
         * Parameter keys and values cannot be null or empty.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1
         * <https://tools.ietf.org/html/rfc6749#section-3.1>"
         */
        @NonNull
        public Builder setAdditionalParameters(@Nullable Map<String, String> additionalParameters) {
            mAdditionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS);
            return this;
        }

        /**
         * Constructs the authorization request. At a minimum the following fields must have been
         * set:
         *
         * - The client ID
         * - The expected response type
         * - The redirect URI
         *
         * Failure to specify any of these parameters will result in a runtime exception.
         */
        @NonNull
        public AuthorizationRequest build() {
            return new AuthorizationRequest(
                    mConfiguration,
                    mClientId,
                    mResponseType,
                    mRedirectUri,
                    mDisplay,
                    mLoginHint,
                    mPrompt,
                    mUiLocales,
                    mScope,
                    mState,
                    mNonce,
                    mCodeVerifier,
                    mCodeVerifierChallenge,
                    mCodeVerifierChallengeMethod,
                    mResponseMode,
                    mClaims,
                    mClaimsLocales,
                    Collections.unmodifiableMap(new HashMap<>(mAdditionalParameters)));
        }
    }

    private AuthorizationRequest(
            @NonNull AuthorizationServiceConfiguration configuration,
            @NonNull String clientId,
            @NonNull String responseType,
            @NonNull Uri redirectUri,
            @Nullable String display,
            @Nullable String loginHint,
            @Nullable String prompt,
            @Nullable String uiLocales,
            @Nullable String scope,
            @Nullable String state,
            @Nullable String nonce,
            @Nullable String codeVerifier,
            @Nullable String codeVerifierChallenge,
            @Nullable String codeVerifierChallengeMethod,
            @Nullable String responseMode,
            @Nullable JSONObject claims,
            @Nullable String claimsLocales,
            @NonNull Map<String, String> additionalParameters) {
        // mandatory fields
        this.configuration = configuration;
        this.clientId = clientId;
        this.responseType = responseType;
        this.redirectUri = redirectUri;
        this.additionalParameters = additionalParameters;

        // optional fields
        this.display = display;
        this.loginHint = loginHint;
        this.prompt = prompt;
        this.uiLocales = uiLocales;
        this.scope = scope;
        this.state = state;
        this.nonce = nonce;
        this.codeVerifier = codeVerifier;
        this.codeVerifierChallenge = codeVerifierChallenge;
        this.codeVerifierChallengeMethod = codeVerifierChallengeMethod;
        this.responseMode = responseMode;
        this.claims = claims;
        this.claimsLocales = claimsLocales;
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
     * Derives the set of prompt values from the consolidated, space-delimited prompt values in
     * the {@link #prompt} field. If no prompt values were specified for this request, the method
     * will return `null`.
     */
    public Set<String> getPromptValues() {
        return AsciiStringListUtil.stringToSet(prompt);
    }

    /**
     * Derives the set of ui_locales values from the consolidated, space-separated list of
     * BCP47 [RFC5646] language tag values in the {@link #uiLocales} field. If no ui_locales values
     * were specified for this request, the method will return `null`.
     */
    public Set<String> getUiLocales() {
        return AsciiStringListUtil.stringToSet(uiLocales);
    }

    @Override
    @Nullable
    public String getState() {
        return state;
    }

    /**
     * Derives the set of claims_locales values from the consolidated, space-separated list of
     * BCP47 [RFC5646] language tag values in the {@link #claimsLocales} field. If no claims_locales
     * values were specified for this request, the method will return `null`.
     */
    public Set<String> getClaimsLocales() {
        return AsciiStringListUtil.stringToSet(claimsLocales);
    }

    /**
     * Produces a request URI, that can be used to dispatch the authorization request.
     */
    @Override
    @NonNull
    public Uri toUri() {
        Uri.Builder uriBuilder = configuration.authorizationEndpoint.buildUpon()
                .appendQueryParameter(PARAM_REDIRECT_URI, redirectUri.toString())
                .appendQueryParameter(PARAM_CLIENT_ID, clientId)
                .appendQueryParameter(PARAM_RESPONSE_TYPE, responseType);

        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_DISPLAY, display);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_LOGIN_HINT, loginHint);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_PROMPT, prompt);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_UI_LOCALES, uiLocales);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_STATE, state);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_NONCE, nonce);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_SCOPE, scope);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_RESPONSE_MODE, responseMode);

        if (codeVerifier != null) {
            uriBuilder.appendQueryParameter(PARAM_CODE_CHALLENGE, codeVerifierChallenge)
                    .appendQueryParameter(PARAM_CODE_CHALLENGE_METHOD, codeVerifierChallengeMethod);
        }

        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_CLAIMS, claims);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_CLAIMS_LOCALES, claimsLocales);

        for (Entry<String, String> entry : additionalParameters.entrySet()) {
            uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
        }

        return uriBuilder.build();
    }

    /**
     * Produces a JSON representation of the authorization request for persistent storage or local
     * transmission (e.g. between activities).
     */
    @Override
    @NonNull
    public JSONObject jsonSerialize() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_CONFIGURATION, configuration.toJson());
        JsonUtil.put(json, KEY_CLIENT_ID, clientId);
        JsonUtil.put(json, KEY_RESPONSE_TYPE, responseType);
        JsonUtil.put(json, KEY_REDIRECT_URI, redirectUri.toString());
        JsonUtil.putIfNotNull(json, KEY_DISPLAY, display);
        JsonUtil.putIfNotNull(json, KEY_LOGIN_HINT, loginHint);
        JsonUtil.putIfNotNull(json, KEY_SCOPE, scope);
        JsonUtil.putIfNotNull(json, KEY_PROMPT, prompt);
        JsonUtil.putIfNotNull(json, KEY_UI_LOCALES, uiLocales);
        JsonUtil.putIfNotNull(json, KEY_STATE, state);
        JsonUtil.putIfNotNull(json, KEY_NONCE, nonce);
        JsonUtil.putIfNotNull(json, KEY_CODE_VERIFIER, codeVerifier);
        JsonUtil.putIfNotNull(json, KEY_CODE_VERIFIER_CHALLENGE, codeVerifierChallenge);
        JsonUtil.putIfNotNull(json, KEY_CODE_VERIFIER_CHALLENGE_METHOD,
                codeVerifierChallengeMethod);
        JsonUtil.putIfNotNull(json, KEY_RESPONSE_MODE, responseMode);
        JsonUtil.putIfNotNull(json, KEY_CLAIMS, claims);
        JsonUtil.putIfNotNull(json, KEY_CLAIMS_LOCALES, claimsLocales);
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
    @NonNull
    public static AuthorizationRequest jsonDeserialize(@NonNull JSONObject json)
            throws JSONException {
        checkNotNull(json, "json cannot be null");
        return new AuthorizationRequest(
                AuthorizationServiceConfiguration.fromJson(json.getJSONObject(KEY_CONFIGURATION)),
                JsonUtil.getString(json, KEY_CLIENT_ID),
                JsonUtil.getString(json, KEY_RESPONSE_TYPE),
                JsonUtil.getUri(json, KEY_REDIRECT_URI),
                JsonUtil.getStringIfDefined(json, KEY_DISPLAY),
                JsonUtil.getStringIfDefined(json, KEY_LOGIN_HINT),
                JsonUtil.getStringIfDefined(json, KEY_PROMPT),
                JsonUtil.getStringIfDefined(json, KEY_UI_LOCALES),
                JsonUtil.getStringIfDefined(json, KEY_SCOPE),
                JsonUtil.getStringIfDefined(json, KEY_STATE),
                JsonUtil.getStringIfDefined(json, KEY_NONCE),
                JsonUtil.getStringIfDefined(json, KEY_CODE_VERIFIER),
                JsonUtil.getStringIfDefined(json, KEY_CODE_VERIFIER_CHALLENGE),
                JsonUtil.getStringIfDefined(json, KEY_CODE_VERIFIER_CHALLENGE_METHOD),
                JsonUtil.getStringIfDefined(json, KEY_RESPONSE_MODE),
                JsonUtil.getJsonObjectIfDefined(json, KEY_CLAIMS),
                JsonUtil.getStringIfDefined(json, KEY_CLAIMS_LOCALES),
                JsonUtil.getStringMap(json, KEY_ADDITIONAL_PARAMETERS));
    }

    /**
     * Reads an authorization request from a JSON string representation produced by
     * {@link #jsonSerializeString()}. This method is just a convenience wrapper for
     * {@link #jsonDeserialize(JSONObject)}, converting the JSON string to its JSON object form.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static AuthorizationRequest jsonDeserialize(@NonNull String jsonStr)
            throws JSONException {
        checkNotNull(jsonStr, "json string cannot be null");
        return jsonDeserialize(new JSONObject(jsonStr));
    }
}
