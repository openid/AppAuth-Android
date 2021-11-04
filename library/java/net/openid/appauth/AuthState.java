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

import static net.openid.appauth.AuthorizationException.AuthorizationRequestErrors;
import static net.openid.appauth.Preconditions.checkArgument;
import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import net.openid.appauth.internal.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects authorization state from authorization requests and responses. This facilitates
 * the creation of subsequent requests based on this state, and allows for this state to be
 * persisted easily.
 */
public class AuthState {

    /**
     * Tokens which have less time than this value left before expiry will be considered to be
     * expired for the purposes of calls to
     * {@link #performActionWithFreshTokens(AuthorizationService, AuthStateAction)
     * performActionWithFreshTokens}.
     */
    public static final int EXPIRY_TIME_TOLERANCE_MS = 60000;

    private static final String KEY_CONFIG = "config";
    private static final String KEY_REFRESH_TOKEN = "refreshToken";
    private static final String KEY_SCOPE = "scope";
    private static final String KEY_LAST_AUTHORIZATION_RESPONSE = "lastAuthorizationResponse";
    private static final String KEY_LAST_TOKEN_RESPONSE = "mLastTokenResponse";
    private static final String KEY_AUTHORIZATION_EXCEPTION = "mAuthorizationException";
    private static final String KEY_LAST_REGISTRATION_RESPONSE = "lastRegistrationResponse";

    @Nullable
    private String mRefreshToken;

    @Nullable
    private String mScope;

    @Nullable
    private AuthorizationServiceConfiguration mConfig;

    @Nullable
    private AuthorizationResponse mLastAuthorizationResponse;

    @Nullable
    private TokenResponse mLastTokenResponse;

    @Nullable
    private RegistrationResponse mLastRegistrationResponse;

    @Nullable
    private AuthorizationException mAuthorizationException;

    private final Object mPendingActionsSyncObject = new Object();
    private List<AuthStateAction> mPendingActions;
    private boolean mNeedsTokenRefreshOverride;

    /**
     * Creates an empty, unauthenticated {@link AuthState}.
     */
    public AuthState() {}

    /**
     * Creates an unauthenticated {@link AuthState}, with the service configuration retained
     * for convenience.
     */
    public AuthState(@NonNull AuthorizationServiceConfiguration config) {
        mConfig = config;
    }

    /**
     * Creates an {@link AuthState} based on an authorization exchange.
     */
    public AuthState(@Nullable AuthorizationResponse authResponse,
            @Nullable AuthorizationException authError) {
        checkArgument(authResponse != null ^ authError != null,
                "exactly one of authResponse or authError should be non-null");
        mPendingActions = null;
        update(authResponse, authError);
    }

    /**
     * Creates an {@link AuthState} based on a dynamic registration client registration request.
     */
    public AuthState(@NonNull RegistrationResponse regResponse) {
        update(regResponse);
    }

    /**
     * Creates an {@link AuthState} based on an authorization exchange and subsequent token
     * exchange.
     */
    public AuthState(
            @NonNull AuthorizationResponse authResponse,
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        this(authResponse, null);
        update(tokenResponse, authException);
    }

    /**
     * The most recent refresh token received from the server, if available. Rather than using
     * this property directly as part of any request depending on authorization state, it is
     * recommended to call {@link #performActionWithFreshTokens(AuthorizationService,
     * AuthStateAction) performActionWithFreshTokens} to ensure that fresh tokens are available.
     */
    @Nullable
    public String getRefreshToken() {
        return mRefreshToken;
    }

    /**
     * The scope of the current authorization grant. This represents the latest scope returned by
     * the server and may be a subset of the scope that was initially granted.
     */
    @Nullable
    public String getScope() {
        return mScope;
    }

    /**
     * A set representation of {@link #getScope()}, for convenience.
     */
    @Nullable
    public Set<String> getScopeSet() {
        return AsciiStringListUtil.stringToSet(mScope);
    }

    /**
     * The most recent authorization response used to update the authorization state. For the
     * implicit flow, this will contain the latest access token. It is rarely necessary to
     * directly use the response; instead convenience methods are provided to retrieve the
     * {@link #getAccessToken() access token},
     * {@link #getAccessTokenExpirationTime() access token expiration},
     * {@link #getIdToken() ID token}
     * and {@link #getScopeSet() scope} regardless of the flow used to retrieve them.
     */
    @Nullable
    public AuthorizationResponse getLastAuthorizationResponse() {
        return mLastAuthorizationResponse;
    }

    /**
     * The most recent token response used to update this authorization state. For the
     * authorization code flow, this will contain the latest access token. It is rarely necessary
     * to directly use the response; instead convenience methods are provided to retrieve the
     * {@link #getAccessToken() access token},
     * {@link #getAccessTokenExpirationTime() access token expiration},
     * {@link #getIdToken() ID token}
     * and {@link #getScopeSet() scope} regardless of the flow used to retrieve them.
     */
    @Nullable
    public TokenResponse getLastTokenResponse() {
        return mLastTokenResponse;
    }

    /**
     * The most recent client registration response used to update this authorization state.
     *
     * <p>
     * It is rarely necessary to directly use the response; instead convenience methods are provided
     * to retrieve the {@link #getClientSecret() client secret} and
     * {@link #getClientSecretExpirationTime() client secret expiration}.
     * </p>
     */
    @Nullable
    public RegistrationResponse getLastRegistrationResponse() {
        return mLastRegistrationResponse;
    }

    /**
     * The configuration of the authorization service associated with this authorization state.
     */
    @Nullable
    public AuthorizationServiceConfiguration getAuthorizationServiceConfiguration() {
        if (mLastAuthorizationResponse != null) {
            return mLastAuthorizationResponse.request.configuration;
        }

        return mConfig;
    }

    /**
     * The current access token, if available. Rather than using
     * this property directly as part of any request depending on authorization state, it s
     * recommended to call {@link #performActionWithFreshTokens(AuthorizationService,
     * AuthStateAction) performActionWithFreshTokens} to ensure that fresh tokens are available.
     */
    @Nullable
    public String getAccessToken() {
        if (mAuthorizationException != null) {
            return null;
        }

        if (mLastTokenResponse != null && mLastTokenResponse.accessToken != null) {
            return mLastTokenResponse.accessToken;
        }

        if (mLastAuthorizationResponse != null) {
            return mLastAuthorizationResponse.accessToken;
        }

        return null;
    }

    /**
     * The expiration time of the current access token (if available), as milliseconds from the
     * UNIX epoch (consistent with {@link System#currentTimeMillis()}).
     */
    @Nullable
    public Long getAccessTokenExpirationTime() {
        if (mAuthorizationException != null) {
            return null;
        }

        if (mLastTokenResponse != null && mLastTokenResponse.accessToken != null) {
            return mLastTokenResponse.accessTokenExpirationTime;
        }

        if (mLastAuthorizationResponse != null && mLastAuthorizationResponse.accessToken != null) {
            return mLastAuthorizationResponse.accessTokenExpirationTime;
        }

        return null;
    }

    /**
     * The current ID token, if available.
     */
    @Nullable
    public String getIdToken() {
        if (mAuthorizationException != null) {
            return null;
        }

        if (mLastTokenResponse != null && mLastTokenResponse.idToken != null) {
            return mLastTokenResponse.idToken;
        }

        if (mLastAuthorizationResponse != null) {
            return mLastAuthorizationResponse.idToken;
        }

        return null;
    }

    /**
     * The current parsed ID token, if available.
     */
    @Nullable
    public IdToken getParsedIdToken() {
        String stringToken = getIdToken();
        IdToken token;

        if (stringToken != null) {
            try {
                token = IdToken.from(stringToken);
            } catch (JSONException | IdToken.IdTokenException ex) {
                token = null;
            }
        } else {
            token = null;
        }

        return token;
    }

    /**
     * The current client secret, if available.
     */
    public String getClientSecret() {
        if (mLastRegistrationResponse != null) {
            return mLastRegistrationResponse.clientSecret;
        }

        return null;
    }

    /**
     * The expiration time of the current client credentials (if available), as milliseconds from
     * the UNIX epoch (consistent with {@link System#currentTimeMillis()}). If the value is 0, the
     * client credentials will not expire.
     */
    @Nullable
    public Long getClientSecretExpirationTime() {
        if (mLastRegistrationResponse != null) {
            return mLastRegistrationResponse.clientSecretExpiresAt;
        }

        return null;
    }

    /**
     * Determines whether the current state represents a successful authorization,
     * from which at least either an access token or an ID token have been retrieved.
     */
    public boolean isAuthorized() {
        return mAuthorizationException == null
                && (getAccessToken() != null || getIdToken() != null);
    }

    /**
     * If the last response was an OAuth related failure, this returns the exception describing
     * the failure.
     */
    @Nullable
    public AuthorizationException getAuthorizationException() {
        return mAuthorizationException;
    }

    /**
     * Determines whether the access token is considered to have expired. If no refresh token
     * has been acquired, then this method will always return `false`. A token refresh
     * can be forced, regardless of the validity of any currently acquired access token, by
     * calling {@link #setNeedsTokenRefresh(boolean) setNeedsTokenRefresh(true)}.
     */
    public boolean getNeedsTokenRefresh() {
        return getNeedsTokenRefresh(SystemClock.INSTANCE);
    }

    @VisibleForTesting
    boolean getNeedsTokenRefresh(Clock clock) {
        if (mNeedsTokenRefreshOverride) {
            return true;
        }

        if (getAccessTokenExpirationTime() == null) {
            // if there is no expiration but we have an access token, it is assumed
            // to never expire.
            return getAccessToken() == null;
        }

        return getAccessTokenExpirationTime()
                <= clock.getCurrentTimeMillis() + EXPIRY_TIME_TOLERANCE_MS;
    }

    /**
     * Sets whether to force an access token refresh, regardless of the current access token's
     * expiration time.
     */
    public void setNeedsTokenRefresh(boolean needsTokenRefresh) {
        mNeedsTokenRefreshOverride = needsTokenRefresh;
    }

    /**
    * Determines whether the client credentials is considered to have expired. If no client
    * credentials have been acquired, then this method will always return `false`
    */
    public boolean hasClientSecretExpired() {
        return hasClientSecretExpired(SystemClock.INSTANCE);
    }

    @VisibleForTesting
    boolean hasClientSecretExpired(Clock clock) {
        if (getClientSecretExpirationTime() == null || getClientSecretExpirationTime() == 0) {
            // no explicit expiration time, and 0 means it will not expire
            return false;
        }

        return getClientSecretExpirationTime() <= clock.getCurrentTimeMillis();
    }

    /**
     * Updates the authorization state based on a new authorization response.
     */
    public void update(
            @Nullable AuthorizationResponse authResponse,
            @Nullable AuthorizationException authException) {
        checkArgument(authResponse != null ^ authException != null,
                "exactly one of authResponse or authException should be non-null");
        if (authException != null) {
            if (authException.type == AuthorizationException.TYPE_OAUTH_AUTHORIZATION_ERROR) {
                mAuthorizationException = authException;
            }
            return;
        }

        // the last token response and refresh token are now stale, as they are associated with
        // any previous authorization response
        mLastAuthorizationResponse = authResponse;
        mConfig = null;
        mLastTokenResponse = null;
        mRefreshToken = null;
        mAuthorizationException = null;

        // if the response's mScope is null, it means that it equals that of the request
        // see: https://tools.ietf.org/html/rfc6749#section-5.1
        mScope = (authResponse.scope != null) ? authResponse.scope : authResponse.request.scope;
    }

    /**
     * Updates the authorization state based on a new token response.
     */
    public void update(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        checkArgument(tokenResponse != null ^ authException != null,
                "exactly one of tokenResponse or authException should be non-null");

        if (mAuthorizationException != null) {
            // Calling updateFromTokenResponse while in an error state probably means the developer
            // obtained a new token and did the exchange without also calling
            // updateFromAuthorizationResponse. Attempt to handle this gracefully, but warn the
            // developer that this is unexpected.
            Logger.warn(
                    "AuthState.update should not be called in an error state (%s), call update"
                            + "with the result of the fresh authorization response first",
                    mAuthorizationException);
            mAuthorizationException = null;
        }

        if (authException != null) {
            if (authException.type == AuthorizationException.TYPE_OAUTH_TOKEN_ERROR) {
                mAuthorizationException = authException;
            }
            return;
        }

        mLastTokenResponse = tokenResponse;
        if (tokenResponse.scope != null) {
            mScope = tokenResponse.scope;
        }
        if (tokenResponse.refreshToken != null) {
            mRefreshToken = tokenResponse.refreshToken;
        }
    }

    /**
     * Updates the authorization state based on a new client registration response.
     */
    public void update(@Nullable RegistrationResponse regResponse) {
        mLastRegistrationResponse = regResponse;

        // a new client registration will have a new client id, so invalidate the current session.
        // Note however that we do not discard the configuration; this is likely still applicable.
        mConfig = getAuthorizationServiceConfiguration();

        mRefreshToken = null;
        mScope = null;
        mLastAuthorizationResponse = null;
        mLastTokenResponse = null;
        mAuthorizationException = null;
    }

    /**
     * Ensures that a non-expired access token is available before invoking the provided action.
     */
    public void performActionWithFreshTokens(
            @NonNull AuthorizationService service,
            @NonNull AuthStateAction action) {
        performActionWithFreshTokens(
                service,
                NoClientAuthentication.INSTANCE,
                Collections.<String, String>emptyMap(),
                SystemClock.INSTANCE,
                action);
    }

    /**
     * Ensures that a non-expired access token is available before invoking the provided action.
     */
    public void performActionWithFreshTokens(
            @NonNull AuthorizationService service,
            @NonNull ClientAuthentication clientAuth,
            @NonNull AuthStateAction action) {
        performActionWithFreshTokens(
                service,
                clientAuth,
                Collections.<String, String>emptyMap(),
                SystemClock.INSTANCE,
                action);
    }

    /**
     * Ensures that a non-expired access token is available before invoking the provided action.
     * If a token refresh is required, the provided additional parameters will be included in this
     * refresh request.
     */
    public void performActionWithFreshTokens(
            @NonNull AuthorizationService service,
            @NonNull Map<String, String> refreshTokenAdditionalParams,
            @NonNull AuthStateAction action) {
        try {
            performActionWithFreshTokens(
                    service,
                    getClientAuthentication(),
                    refreshTokenAdditionalParams,
                    SystemClock.INSTANCE,
                    action);
        } catch (ClientAuthentication.UnsupportedAuthenticationMethod ex) {
            action.execute(null, null,
                    AuthorizationException.fromTemplate(
                            AuthorizationException.TokenRequestErrors.CLIENT_ERROR, ex));
        }
    }

    /**
     * Ensures that a non-expired access token is available before invoking the provided action.
     * If a token refresh is required, the provided additional parameters will be included in this
     * refresh request.
     */
    public void performActionWithFreshTokens(
            @NonNull AuthorizationService service,
            @NonNull ClientAuthentication clientAuth,
            @NonNull Map<String, String> refreshTokenAdditionalParams,
            @NonNull AuthStateAction action) {
        performActionWithFreshTokens(
                service,
                clientAuth,
                refreshTokenAdditionalParams,
                SystemClock.INSTANCE,
                action);
    }

    @VisibleForTesting
    void performActionWithFreshTokens(
            @NonNull final AuthorizationService service,
            @NonNull final ClientAuthentication clientAuth,
            @NonNull final Map<String, String> refreshTokenAdditionalParams,
            @NonNull final Clock clock,
            @NonNull final AuthStateAction action) {
        checkNotNull(service, "service cannot be null");
        checkNotNull(clientAuth, "client authentication cannot be null");
        checkNotNull(refreshTokenAdditionalParams,
                "additional params cannot be null");
        checkNotNull(clock, "clock cannot be null");
        checkNotNull(action, "action cannot be null");

        if (!getNeedsTokenRefresh(clock)) {
            action.execute(getAccessToken(), getIdToken(), null);
            return;
        }

        if (mRefreshToken == null) {
            AuthorizationException ex = AuthorizationException.fromTemplate(
                    AuthorizationRequestErrors.CLIENT_ERROR,
                    new IllegalStateException("No refresh token available and token have expired"));
            action.execute(null, null, ex);
            return;
        }

        checkNotNull(mPendingActionsSyncObject, "pending actions sync object cannot be null");
        synchronized (mPendingActionsSyncObject) {
            //if a token request is currently executing, queue the actions instead
            if (mPendingActions != null) {
                mPendingActions.add(action);
                return;
            }

            //creates a list of pending actions, starting with the current action
            mPendingActions = new ArrayList<>();
            mPendingActions.add(action);
        }

        service.performTokenRequest(
                createTokenRefreshRequest(refreshTokenAdditionalParams),
                clientAuth,
                new AuthorizationService.TokenResponseCallback() {
                    @Override
                    public void onTokenRequestCompleted(
                            @Nullable TokenResponse response,
                            @Nullable AuthorizationException ex) {
                        update(response, ex);

                        String accessToken = null;
                        String idToken = null;
                        AuthorizationException exception = null;

                        if (ex == null) {
                            mNeedsTokenRefreshOverride = false;
                            accessToken = getAccessToken();
                            idToken = getIdToken();
                        } else {
                            exception = ex;
                        }

                        //sets pending queue to null and processes all actions in the queue
                        List<AuthStateAction> actionsToProcess;
                        synchronized (mPendingActionsSyncObject) {
                            actionsToProcess = mPendingActions;
                            mPendingActions = null;
                        }
                        for (AuthStateAction action : actionsToProcess) {
                            action.execute(accessToken, idToken, exception);
                        }
                    }
                });
    }

    /**
     * Creates a token request for new tokens using the current refresh token.
     */
    @NonNull
    public TokenRequest createTokenRefreshRequest() {
        return createTokenRefreshRequest(Collections.<String, String>emptyMap());
    }

    /**
     * Creates a token request for new tokens using the current refresh token, adding the
     * specified additional parameters.
     */
    @NonNull
    public TokenRequest createTokenRefreshRequest(
            @NonNull Map<String, String> additionalParameters) {
        if (mRefreshToken == null) {
            throw new IllegalStateException("No refresh token available for refresh request");
        }
        if (mLastAuthorizationResponse == null) {
            throw new IllegalStateException(
                    "No authorization configuration available for refresh request");
        }

        return new TokenRequest.Builder(
                mLastAuthorizationResponse.request.configuration,
                mLastAuthorizationResponse.request.clientId)
                .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                .setScope(null)
                .setRefreshToken(mRefreshToken)
                .setAdditionalParameters(additionalParameters)
                .build();
    }

    /**
     * Produces a JSON representation of the authorization state for persistent storage or local
     * transmission (e.g. between activities).
     */
    public JSONObject jsonSerialize() {
        JSONObject json = new JSONObject();
        JsonUtil.putIfNotNull(json, KEY_REFRESH_TOKEN, mRefreshToken);
        JsonUtil.putIfNotNull(json, KEY_SCOPE, mScope);

        if (mConfig != null) {
            JsonUtil.put(json, KEY_CONFIG, mConfig.toJson());
        }

        if (mAuthorizationException != null) {
            JsonUtil.put(json, KEY_AUTHORIZATION_EXCEPTION, mAuthorizationException.toJson());
        }

        if (mLastAuthorizationResponse != null) {
            JsonUtil.put(
                    json,
                    KEY_LAST_AUTHORIZATION_RESPONSE,
                    mLastAuthorizationResponse.jsonSerialize());
        }

        if (mLastTokenResponse != null) {
            JsonUtil.put(
                    json,
                    KEY_LAST_TOKEN_RESPONSE,
                    mLastTokenResponse.jsonSerialize());
        }

        if (mLastRegistrationResponse != null) {
            JsonUtil.put(
                    json,
                    KEY_LAST_REGISTRATION_RESPONSE,
                    mLastRegistrationResponse.jsonSerialize());
        }

        return json;
    }

    /**
     * Produces a JSON string representation of the authorization state for persistent storage or
     * local transmission (e.g. between activities). This method is just a convenience wrapper
     * for {@link #jsonSerialize()}, converting the JSON object to its string form.
     */
    public String jsonSerializeString() {
        return jsonSerialize().toString();
    }

    /**
     * Reads an authorization state instance from a JSON string representation produced by
     * {@link #jsonSerialize()}.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    public static AuthState jsonDeserialize(@NonNull JSONObject json) throws JSONException {
        checkNotNull(json, "json cannot be null");

        AuthState state = new AuthState();
        state.mRefreshToken = JsonUtil.getStringIfDefined(json, KEY_REFRESH_TOKEN);
        state.mScope = JsonUtil.getStringIfDefined(json, KEY_SCOPE);

        if (json.has(KEY_CONFIG)) {
            state.mConfig = AuthorizationServiceConfiguration.fromJson(
                    json.getJSONObject(KEY_CONFIG));
        }

        if (json.has(KEY_AUTHORIZATION_EXCEPTION)) {
            state.mAuthorizationException = AuthorizationException.fromJson(
                    json.getJSONObject(KEY_AUTHORIZATION_EXCEPTION));
        }

        if (json.has(KEY_LAST_AUTHORIZATION_RESPONSE)) {
            state.mLastAuthorizationResponse = AuthorizationResponse.jsonDeserialize(
                    json.getJSONObject(KEY_LAST_AUTHORIZATION_RESPONSE));
        }

        if (json.has(KEY_LAST_TOKEN_RESPONSE)) {
            state.mLastTokenResponse = TokenResponse.jsonDeserialize(
                    json.getJSONObject(KEY_LAST_TOKEN_RESPONSE));
        }

        if (json.has(KEY_LAST_REGISTRATION_RESPONSE)) {
            state.mLastRegistrationResponse = RegistrationResponse.jsonDeserialize(
                    json.getJSONObject(KEY_LAST_REGISTRATION_RESPONSE));
        }

        return state;
    }

    /**
     * Reads an authorization state instance from a JSON string representation produced by
     * {@link #jsonSerializeString()}. This method is just a convenience wrapper for
     * {@link #jsonDeserialize(JSONObject)}, converting the JSON string to its JSON object form.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    public static AuthState jsonDeserialize(@NonNull String jsonStr) throws JSONException {
        checkNotEmpty(jsonStr, "jsonStr cannot be null or empty");
        return jsonDeserialize(new JSONObject(jsonStr));
    }

    /**
     * Interface for actions executed in the context of fresh (non-expired) tokens.
     * @see #performActionWithFreshTokens(AuthorizationService, AuthStateAction)
     */
    public interface AuthStateAction {
        /**
         * Executed in the context of fresh (non-expired) tokens. If new tokens were
         * required to execute the action and could not be acquired, an authorization
         * exception is provided instead. One or both of the access token and ID token will be
         * provided, dependent upon the token types previously negotiated.
         */
        void execute(
                @Nullable String accessToken,
                @Nullable String idToken,
                @Nullable AuthorizationException ex);
    }

    /**
     * Creates the required client authentication for the token endpoint based on information
     * in the most recent registration response (if it is set).
     *
     * @throws ClientAuthentication.UnsupportedAuthenticationMethod if the expected client
     *     authentication method is unsupported by this client library.
     */
    public ClientAuthentication getClientAuthentication() throws
            ClientAuthentication.UnsupportedAuthenticationMethod {
        if (getClientSecret() == null) {
            /* Without client credentials, or unspecified 'token_endpoint_auth_method',
             * we can never authenticate */
            return NoClientAuthentication.INSTANCE;
        } else if (mLastRegistrationResponse.tokenEndpointAuthMethod == null) {
            /* 'token_endpoint_auth_method': "If omitted, the default is client_secret_basic",
             * "OpenID Connect Dynamic Client Registration 1.0", Section 2 */
            return new ClientSecretBasic(getClientSecret());
        }

        switch (mLastRegistrationResponse.tokenEndpointAuthMethod) {
            case ClientSecretBasic.NAME:
                return new ClientSecretBasic(getClientSecret());
            case ClientSecretPost.NAME:
                return new ClientSecretPost(getClientSecret());
            case "none":
                return NoClientAuthentication.INSTANCE;
            default:
                throw new ClientAuthentication.UnsupportedAuthenticationMethod(
                        mLastRegistrationResponse.tokenEndpointAuthMethod);

        }
    }
}
