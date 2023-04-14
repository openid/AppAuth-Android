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

import static net.openid.appauth.TestValues.TEST_ACCESS_TOKEN;
import static net.openid.appauth.TestValues.TEST_AUTH_CODE;
import static net.openid.appauth.TestValues.TEST_CLIENT_SECRET;
import static net.openid.appauth.TestValues.TEST_ID_TOKEN;
import static net.openid.appauth.TestValues.TEST_REFRESH_TOKEN;
import static net.openid.appauth.TestValues.getMinimalAuthRequestBuilder;
import static net.openid.appauth.TestValues.getTestAuthCodeExchangeResponse;
import static net.openid.appauth.TestValues.getTestAuthCodeExchangeResponseBuilder;
import static net.openid.appauth.TestValues.getTestAuthRequest;
import static net.openid.appauth.TestValues.getTestAuthResponse;
import static net.openid.appauth.TestValues.getTestAuthResponseBuilder;
import static net.openid.appauth.TestValues.getTestRegistrationResponse;
import static net.openid.appauth.TestValues.getTestRegistrationResponseBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=16)
public class AuthStateTest {

    private static final Long ONE_SECOND = 1000L;
    private static final Long TWO_MINUTES = 120000L;

    private TestClock mClock;

    @Before
    public void setUp() {
        mClock = new TestClock(0L);
    }

    @Test
    public void testInitialState() {
        AuthState state = new AuthState();
        assertThat(state.isAuthorized()).isFalse();

        assertThat(state.getAccessToken()).isNull();
        assertThat(state.getAccessTokenExpirationTime()).isNull();
        assertThat(state.getIdToken()).isNull();
        assertThat(state.getRefreshToken()).isNull();

        assertThat(state.getLastAuthorizationResponse()).isNull();
        assertThat(state.getLastTokenResponse()).isNull();
        assertThat(state.getLastRegistrationResponse()).isNull();

        assertThat(state.getScope()).isNull();
        assertThat(state.getScopeSet()).isNull();

        assertThat(state.getNeedsTokenRefresh(mClock)).isTrue();
    }

    @Test
    public void testInitialState_fromAuthorizationResponse() {
        AuthorizationRequest authCodeRequest = getTestAuthRequest();
        AuthorizationResponse resp = getTestAuthResponse();
        AuthState state = new AuthState(resp, null);

        assertThat(state.isAuthorized()).isFalse();
        assertThat(state.getAccessToken()).isNull();
        assertThat(state.getAccessTokenExpirationTime()).isNull();
        assertThat(state.getIdToken()).isNull();
        assertThat(state.getRefreshToken()).isNull();

        assertThat(state.getAuthorizationException()).isNull();
        assertThat(state.getLastAuthorizationResponse()).isSameAs(resp);
        assertThat(state.getLastTokenResponse()).isNull();

        assertThat(state.getScope()).isEqualTo(authCodeRequest.scope);
        assertThat(state.getScopeSet()).isEqualTo(authCodeRequest.getScopeSet());

        assertThat(state.getNeedsTokenRefresh(mClock)).isTrue();
    }

    @Test
    public void testInitialState_fromAuthorizationException() {
        AuthState state = new AuthState(
                null,
                AuthorizationException.AuthorizationRequestErrors.ACCESS_DENIED);

        assertThat(state.isAuthorized()).isFalse();
        assertThat(state.getAccessToken()).isNull();
        assertThat(state.getAccessTokenExpirationTime()).isNull();
        assertThat(state.getIdToken()).isNull();
        assertThat(state.getRefreshToken()).isNull();

        assertThat(state.getAuthorizationException()).isEqualTo(
                AuthorizationException.AuthorizationRequestErrors.ACCESS_DENIED);
        assertThat(state.getLastAuthorizationResponse()).isNull();
        assertThat(state.getLastTokenResponse()).isNull();

        assertThat(state.getScope()).isNull();
        assertThat(state.getScopeSet()).isNull();
        assertThat(state.getNeedsTokenRefresh(mClock)).isTrue();
    }

    @Test
    public void testInitialState_fromAuthorizationResponse_withModifiedScope() {
        // simulate a situation in which the response grants a subset of the requested scopes,
        // perhaps due to policy or user preference
        AuthorizationResponse resp = getTestAuthResponseBuilder()
                .setScopes(AuthorizationRequest.Scope.OPENID)
                .build();
        AuthState state = new AuthState(resp, null);

        assertThat(state.getScope()).isEqualTo(resp.scope);
        assertThat(state.getScopeSet()).isEqualTo(resp.getScopeSet());
    }

    @Test
    public void testInitialState_fromAuthorizationResponseAndTokenResponse() {
        AuthorizationResponse authResp = getTestAuthResponse();
        TokenResponse tokenResp = getTestAuthCodeExchangeResponse();
        AuthState state = new AuthState(authResp, tokenResp, null);

        assertThat(state.getAccessToken()).isNull();
        assertThat(state.getAccessTokenExpirationTime()).isNull();
        assertThat(state.getIdToken()).isNull();
        assertThat(state.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);

        assertThat(state.getAuthorizationException()).isNull();
        assertThat(state.getLastAuthorizationResponse()).isSameAs(authResp);
        assertThat(state.getLastTokenResponse()).isSameAs(tokenResp);

        assertThat(state.getScope()).isEqualTo(authResp.request.scope);
        assertThat(state.getScopeSet()).isEqualTo(authResp.request.getScopeSet());

        // no access token or ID token have yet been retrieved
        assertThat(state.isAuthorized()).isFalse();

        // the refresh token has been acquired, but has not yet been used to fetch an access token
        assertThat(state.getNeedsTokenRefresh(mClock)).isTrue();
    }

    @Test
    public void testInitialState_fromRegistrationResponse() {
        RegistrationResponse regResp = getTestRegistrationResponse();
        AuthState state = new AuthState(regResp);

        assertThat(state.isAuthorized()).isFalse();
        assertThat(state.getAccessToken()).isNull();
        assertThat(state.getAccessTokenExpirationTime()).isNull();
        assertThat(state.getIdToken()).isNull();
        assertThat(state.getRefreshToken()).isNull();

        assertThat(state.getAuthorizationException()).isNull();
        assertThat(state.getLastAuthorizationResponse()).isNull();
        assertThat(state.getLastTokenResponse()).isNull();
        assertThat(state.getLastRegistrationResponse()).isSameAs(regResp);

        assertThat(state.getScope()).isNull();
        assertThat(state.getScopeSet()).isNull();
        assertThat(state.getNeedsTokenRefresh(mClock)).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_withAuthResponseAndException() {
        new AuthState(getTestAuthResponse(),
                AuthorizationException.AuthorizationRequestErrors.ACCESS_DENIED);
    }

    @Test
    public void testUpdate_authResponseWithException_authErrorType() {
        AuthState state = new AuthState();
        state.update((AuthorizationResponse) null,
                AuthorizationException.AuthorizationRequestErrors.ACCESS_DENIED);

        assertThat(state.getAuthorizationException())
                .isSameAs(AuthorizationException.AuthorizationRequestErrors.ACCESS_DENIED);
    }

    @Test
    public void testUpdate_authResponseWithException_ignoredErrorType() {
        AuthState state = new AuthState();
        state.update((AuthorizationResponse) null,
                AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW);

        assertThat(state.getAuthorizationException()).isNull();
    }

    @Test
    public void testUpdate_tokenResponseWithException_tokenErrorType() {
        AuthState state = new AuthState();
        state.update((TokenResponse) null,
                AuthorizationException.TokenRequestErrors.UNAUTHORIZED_CLIENT);

        assertThat(state.getAuthorizationException())
                .isSameAs(AuthorizationException.TokenRequestErrors.UNAUTHORIZED_CLIENT);
    }

    @Test
    public void testUpdate_tokenResponseWithException_ignoredErrorType() {
        AuthState state = new AuthState();
        state.update((TokenResponse) null,
                AuthorizationException.GeneralErrors.NETWORK_ERROR);

        assertThat(state.getAuthorizationException()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdate_withAuthResponseAndException() {
        AuthState state = new AuthState();
        state.update(getTestAuthResponse(),
                AuthorizationException.AuthorizationRequestErrors.ACCESS_DENIED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdate_withTokenResponseAndException() {
        AuthState state = new AuthState(getTestAuthResponse(), null);
        state.update(getTestAuthCodeExchangeResponse(),
                AuthorizationException.AuthorizationRequestErrors.ACCESS_DENIED);
    }

    @Test
    public void testGetAccessToken_fromAuthResponse() {
        AuthorizationRequest authReq = getMinimalAuthRequestBuilder("code token")
                .setScope(AuthorizationRequest.Scope.EMAIL)
                .build();
        AuthorizationResponse authResp = new AuthorizationResponse.Builder(authReq)
                .setAuthorizationCode(TEST_AUTH_CODE)
                .setAccessToken(TEST_ACCESS_TOKEN)
                .setState(authReq.state)
                .build();

        TokenResponse tokenResp = getTestAuthCodeExchangeResponse();
        AuthState state = new AuthState(authResp, tokenResp, null);

        // in this scenario, we have an access token in the authorization response but not
        // in the token response. We expect the access token from the authorization response
        // to be returned.
        assertThat(state.getAccessToken()).isEqualTo(authResp.accessToken);
    }

    @Test
    public void testGetAccessToken_fromTokenResponse() {
        AuthorizationRequest authReq = getMinimalAuthRequestBuilder("code token")
                .setScope(AuthorizationRequest.Scope.EMAIL)
                .build();
        AuthorizationResponse authResp = new AuthorizationResponse.Builder(authReq)
                .setAuthorizationCode(TEST_AUTH_CODE)
                .setAccessToken("older_token")
                .setState(authReq.state)
                .build();
        TokenResponse tokenResp = getTestAuthCodeExchangeResponseBuilder()
                .setAccessToken("newer_token")
                .build();
        AuthState state = new AuthState(authResp, tokenResp, null);

        // in this scenario, we have an access token on both the authorization response and the
        // token response. The value on the token response takes precedence.
        assertThat(state.getAccessToken()).isEqualTo(tokenResp.accessToken);
    }

    @Test
    public void testGetIdToken_fromAuthResponse() {
        AuthorizationRequest authReq = getMinimalAuthRequestBuilder("code id_token")
                .setScope(AuthorizationRequest.Scope.EMAIL)
                .build();
        AuthorizationResponse authResp = new AuthorizationResponse.Builder(authReq)
                .setAuthorizationCode(TEST_AUTH_CODE)
                .setIdToken(TEST_ID_TOKEN)
                .setState(authReq.state)
                .build();

        TokenResponse tokenResp = getTestAuthCodeExchangeResponse();
        AuthState state = new AuthState(authResp, tokenResp, null);

        // in this scenario, we have an ID token in the authorization response but not
        // in the token response. We expect the ID token from the authorization response
        // to be returned.
        assertThat(state.getIdToken()).isEqualTo(authResp.idToken);
    }

    @Test
    public void testGetIdToken_fromTokenResponse() {
        AuthorizationRequest authReq = getMinimalAuthRequestBuilder("code id_token")
                .setScope(AuthorizationRequest.Scope.EMAIL)
                .build();
        AuthorizationResponse authResp = new AuthorizationResponse.Builder(authReq)
                .setAuthorizationCode(TEST_AUTH_CODE)
                .setIdToken("older.token.value")
                .setState(authReq.state)
                .build();
        TokenResponse tokenResp = getTestAuthCodeExchangeResponseBuilder()
                .setIdToken("newer.token.value")
                .build();
        AuthState state = new AuthState(authResp, tokenResp, null);

        // in this scenario, we have an ID token on both the authorization response and the
        // token response. The value on the token response takes precedence.
        assertThat(state.getIdToken()).isEqualTo(tokenResp.idToken);
    }

    @Test
    public void testCreateTokenRefreshRequest() {
        AuthorizationResponse authResp = getTestAuthResponse();
        TokenResponse tokenResp = getTestAuthCodeExchangeResponse();
        AuthState state = new AuthState(authResp, tokenResp, null);

        TokenRequest request = state.createTokenRefreshRequest();
        assertThat(request.configuration.tokenEndpoint)
                .isEqualTo(state.getAuthorizationServiceConfiguration().tokenEndpoint);
        assertThat(request.clientId).isEqualTo(authResp.request.clientId);
        assertThat(request.grantType).isEqualTo(GrantTypeValues.REFRESH_TOKEN);
        assertThat(request.refreshToken).isEqualTo(state.getRefreshToken());
    }

    @Test
    public void testGetNeedsTokenRefresh() {
        AuthorizationRequest authReq = getMinimalAuthRequestBuilder("token code")
                .setScope("my_scope")
                .build();

        AuthorizationResponse authResp = new AuthorizationResponse.Builder(authReq)
                .setAccessToken(TEST_ACCESS_TOKEN)
                .setAccessTokenExpirationTime(TWO_MINUTES)
                .setAuthorizationCode(TEST_AUTH_CODE)
                .setState(authReq.state)
                .build();
        TokenResponse tokenResp = getTestAuthCodeExchangeResponse();
        AuthState state = new AuthState(authResp, tokenResp, null);

        // before the expiration time
        mClock.currentTime.set(ONE_SECOND);
        assertThat(state.getNeedsTokenRefresh(mClock)).isFalse();

        // 1ms before the tolerance threshold
        mClock.currentTime.set(TWO_MINUTES - AuthState.EXPIRY_TIME_TOLERANCE_MS - 1);
        assertThat(state.getNeedsTokenRefresh(mClock)).isFalse();

        // on the tolerance threshold
        mClock.currentTime.set(TWO_MINUTES - AuthState.EXPIRY_TIME_TOLERANCE_MS);
        assertThat(state.getNeedsTokenRefresh(mClock)).isTrue();

        // past tolerance threshold
        mClock.currentTime.set(TWO_MINUTES - AuthState.EXPIRY_TIME_TOLERANCE_MS + ONE_SECOND);
        assertThat(state.getNeedsTokenRefresh(mClock)).isTrue();

        // on token's actual expiration
        mClock.currentTime.set(TWO_MINUTES);
        assertThat(state.getNeedsTokenRefresh(mClock)).isTrue();

        // past token's actual expiration
        mClock.currentTime.set(TWO_MINUTES + ONE_SECOND);
        assertThat(state.getNeedsTokenRefresh(mClock)).isTrue();
    }

    @Test
    public void testSetNeedsTokenRefresh() {
        AuthorizationRequest authReq = getMinimalAuthRequestBuilder("token code")
                .setScope("my_scope")
                .build();

        AuthorizationResponse authResp = new AuthorizationResponse.Builder(authReq)
                .setAccessToken(TEST_ACCESS_TOKEN)
                .setAccessTokenExpirationTime(TWO_MINUTES)
                .setAuthorizationCode(TEST_AUTH_CODE)
                .setState(authReq.state)
                .build();
        TokenResponse tokenResp = getTestAuthCodeExchangeResponse();
        AuthState state = new AuthState(authResp, tokenResp, null);

        // before the expiration time...
        mClock.currentTime.set(ONE_SECOND);
        assertThat(state.getNeedsTokenRefresh(mClock)).isFalse();

        // ... force a refresh
        state.setNeedsTokenRefresh(true);
        assertThat(state.getNeedsTokenRefresh(mClock)).isTrue();
    }

    @Test
    public void testPerformActionWithFreshTokens() {
        AuthorizationRequest authReq = getMinimalAuthRequestBuilder("id_token token code")
                .setScope("my_scope")
                .build();

        AuthorizationResponse authResp = new AuthorizationResponse.Builder(authReq)
                .setAccessToken(TEST_ACCESS_TOKEN)
                .setAccessTokenExpirationTime(TWO_MINUTES)
                .setIdToken(TEST_ID_TOKEN)
                .setAuthorizationCode(TEST_AUTH_CODE)
                .setState(authReq.state)
                .build();
        TokenResponse tokenResp = getTestAuthCodeExchangeResponse();
        AuthState state = new AuthState(authResp, tokenResp, null);

        AuthorizationService service = mock(AuthorizationService.class);
        AuthState.AuthStateAction action = mock(AuthState.AuthStateAction.class);

        // at this point in time, the access token will not be considered to be expired
        mClock.currentTime.set(ONE_SECOND);
        state.performActionWithFreshTokens(
                service,
                NoClientAuthentication.INSTANCE,
                Collections.<String, String>emptyMap(),
                mClock,
                action);

        // as the token has not expired, the service will not be used to refresh it
        verifyNoInteractions(service);

        // the action should have been directly invoked
        verify(action, times(1)).execute(
                eq(TEST_ACCESS_TOKEN),
                eq(TEST_ID_TOKEN),
                ArgumentMatchers.<AuthorizationException>isNull());
    }

    @Test
    public void testPerformActionWithFreshTokens_afterTokenExpiration() {
        AuthorizationRequest authReq = getMinimalAuthRequestBuilder("id_token token code")
                .setScope("my_scope")
                .build();

        AuthorizationResponse authResp = new AuthorizationResponse.Builder(authReq)
                .setAccessToken(TEST_ACCESS_TOKEN)
                .setAccessTokenExpirationTime(TWO_MINUTES)
                .setIdToken(TEST_ID_TOKEN)
                .setAuthorizationCode(TEST_AUTH_CODE)
                .setState(authReq.state)
                .build();
        TokenResponse tokenResp = getTestAuthCodeExchangeResponse();
        AuthState state = new AuthState(authResp, tokenResp, null);

        AuthorizationService service = mock(AuthorizationService.class);
        AuthState.AuthStateAction action = mock(AuthState.AuthStateAction.class);

        // at this point in time, the access token will be considered to be expired
        mClock.currentTime.set(TWO_MINUTES - AuthState.EXPIRY_TIME_TOLERANCE_MS + ONE_SECOND);
        state.performActionWithFreshTokens(
                service,
                NoClientAuthentication.INSTANCE,
                Collections.<String, String>emptyMap(),
                mClock,
                action);

        // as the access token has expired, we expect a token refresh request
        ArgumentCaptor<TokenRequest> requestCaptor = ArgumentCaptor.forClass(TokenRequest.class);
        ArgumentCaptor<AuthorizationService.TokenResponseCallback> callbackCaptor =
                ArgumentCaptor.forClass(AuthorizationService.TokenResponseCallback.class);
        verify(service, times(1)).performTokenRequest(
                requestCaptor.capture(),
                any(ClientAuthentication.class),
                callbackCaptor.capture());

        assertThat(requestCaptor.getValue().refreshToken).isEqualTo(tokenResp.refreshToken);

        // the action should not be executed until after the token refresh completes
        verifyNoInteractions(action);

        String freshAccessToken = "fresh_access_token";
        Long freshExpirationTime = mClock.currentTime.get() + TWO_MINUTES;
        String freshIdToken = "fresh.id.token";

        // simulate success on the token request, with fresh tokens in the result
        TokenResponse freshResponse = new TokenResponse.Builder(requestCaptor.getValue())
                .setTokenType(TokenResponse.TOKEN_TYPE_BEARER)
                .setAccessToken(freshAccessToken)
                .setAccessTokenExpirationTime(freshExpirationTime)
                .setIdToken(freshIdToken)
                .build();

        callbackCaptor.getValue().onTokenRequestCompleted(freshResponse, null);

        // the action should be invoked in response to the token request completion
        verify(action, times(1)).execute(
                eq(freshAccessToken),
                eq(freshIdToken),
                ArgumentMatchers.<AuthorizationException>isNull());

        // additionally, the auth state should be updated with the new token values
        assertThat(state.getAccessToken()).isEqualTo(freshAccessToken);
        assertThat(state.getAccessTokenExpirationTime()).isEqualTo(freshExpirationTime);
        assertThat(state.getIdToken()).isEqualTo(freshIdToken);
    }

    @Test
    public void testPerformActionWithFreshToken_afterTokenExpiration_multipleActions() {
        AuthorizationRequest authReq = getMinimalAuthRequestBuilder("id_token token code")
            .setScope("my_scope")
            .build();

        AuthorizationResponse authResp = new AuthorizationResponse.Builder(authReq)
            .setAccessToken(TEST_ACCESS_TOKEN)
            .setAccessTokenExpirationTime(TWO_MINUTES)
            .setIdToken(TEST_ID_TOKEN)
            .setAuthorizationCode(TEST_AUTH_CODE)
            .setState(authReq.state)
            .build();
        TokenResponse tokenResp = getTestAuthCodeExchangeResponse();
        AuthState state = new AuthState(authResp, tokenResp, null);

        AuthorizationService service = mock(AuthorizationService.class);
        AuthState.AuthStateAction action = mock(AuthState.AuthStateAction.class);

        // at this point in time, the access token will be considered to be expired
        mClock.currentTime.set(TWO_MINUTES - AuthState.EXPIRY_TIME_TOLERANCE_MS + ONE_SECOND);
        state.performActionWithFreshTokens(
            service,
            NoClientAuthentication.INSTANCE,
            Collections.<String, String>emptyMap(),
            mClock,
            action);
        state.performActionWithFreshTokens(
            service,
            NoClientAuthentication.INSTANCE,
            Collections.<String, String>emptyMap(),
            mClock,
            action);

        // as the access token has expired, we expect a token refresh request
        ArgumentCaptor<TokenRequest> requestCaptor = ArgumentCaptor.forClass(TokenRequest.class);
        ArgumentCaptor<AuthorizationService.TokenResponseCallback> callbackCaptor =
            ArgumentCaptor.forClass(AuthorizationService.TokenResponseCallback.class);

        verify(service, times(1)).performTokenRequest(
            requestCaptor.capture(),
            any(ClientAuthentication.class),
            callbackCaptor.capture());

        assertThat(requestCaptor.getValue().refreshToken).isEqualTo(tokenResp.refreshToken);

        // the action should not be executed until after the token refresh completes
        verifyNoInteractions(action);

        String freshRefreshToken = "fresh_refresh_token";
        String freshAccessToken = "fresh_access_token";
        String freshIdToken = "fresh.id.token";
        Long freshExpirationTime = mClock.currentTime.get() + TWO_MINUTES;

        // simulate success on the token request, with fresh tokens in the result
        TokenResponse freshResponse = new TokenResponse.Builder(requestCaptor.getValue())
            .setTokenType(TokenResponse.TOKEN_TYPE_BEARER)
            .setAccessToken(freshAccessToken)
            .setAccessTokenExpirationTime(freshExpirationTime)
            .setIdToken(freshIdToken)
            .setRefreshToken(freshRefreshToken)
            .build();

        callbackCaptor.getValue().onTokenRequestCompleted(freshResponse, null);
        // the action should be invoked in response to the token request completion
        verify(action, times(2)).execute(
            eq(freshAccessToken),
            eq(freshIdToken),
            ArgumentMatchers.<AuthorizationException>isNull());

        // additionally, the auth state should be updated with the new token values
        assertThat(state.getRefreshToken()).isEqualTo(freshRefreshToken);
        assertThat(state.getAccessToken()).isEqualTo(freshAccessToken);
        assertThat(state.getAccessTokenExpirationTime()).isEqualTo(freshExpirationTime);
        assertThat(state.getIdToken()).isEqualTo(freshIdToken);
    }

    @Test
    public void testJsonSerialization() throws Exception {
        AuthorizationRequest authReq = getMinimalAuthRequestBuilder("id_token token code")
                .setScopes(
                        AuthorizationRequest.Scope.OPENID,
                        AuthorizationRequest.Scope.EMAIL,
                        AuthorizationRequest.Scope.PROFILE)
                .build();
        AuthorizationResponse authResp = new AuthorizationResponse.Builder(authReq)
                .setAccessToken(TEST_ACCESS_TOKEN)
                .setIdToken(TEST_ID_TOKEN)
                .setAuthorizationCode(TEST_AUTH_CODE)
                .setState(authReq.state)
                .build();

        TokenResponse tokenResp = getTestAuthCodeExchangeResponse();
        RegistrationResponse regResp = getTestRegistrationResponse();
        AuthState state = new AuthState(authResp, tokenResp, null);
        state.update(regResp);

        String json = state.jsonSerializeString();
        AuthState restoredState = AuthState.jsonDeserialize(json);

        assertThat(restoredState.isAuthorized()).isEqualTo(state.isAuthorized());

        assertThat(restoredState.getAccessToken()).isEqualTo(state.getAccessToken());
        assertThat(restoredState.getAccessTokenExpirationTime())
                .isEqualTo(state.getAccessTokenExpirationTime());
        assertThat(restoredState.getIdToken()).isEqualTo(state.getIdToken());
        assertThat(restoredState.getRefreshToken()).isEqualTo(state.getRefreshToken());
        assertThat(restoredState.getScope()).isEqualTo(state.getScope());
        assertThat(restoredState.getNeedsTokenRefresh(mClock))
                .isEqualTo(state.getNeedsTokenRefresh(mClock));
        assertThat(restoredState.getClientSecret()).isEqualTo(state.getClientSecret());
        assertThat(restoredState.hasClientSecretExpired(mClock))
                .isEqualTo(state.hasClientSecretExpired(mClock));
    }

    @Test
    public void testJsonSerialization_doesNotChange() throws Exception {
        AuthorizationRequest authReq = getMinimalAuthRequestBuilder("id_token token code")
            .setScopes(
                AuthorizationRequest.Scope.OPENID,
                AuthorizationRequest.Scope.EMAIL,
                AuthorizationRequest.Scope.PROFILE)
            .build();
        AuthorizationResponse authResp = new AuthorizationResponse.Builder(authReq)
            .setAccessToken(TEST_ACCESS_TOKEN)
            .setIdToken(TEST_ID_TOKEN)
            .setAuthorizationCode(TEST_AUTH_CODE)
            .setState(authReq.state)
            .build();

        TokenResponse tokenResp = getTestAuthCodeExchangeResponse();
        RegistrationResponse regResp = getTestRegistrationResponse();
        AuthState state = new AuthState(authResp, tokenResp, null);
        state.update(regResp);

        String firstOutput = state.jsonSerializeString();
        String secondOutput = AuthState.jsonDeserialize(firstOutput).jsonSerializeString();

        assertThat(secondOutput).isEqualTo(firstOutput);
    }

    @Test
    public void testJsonSerialization_withException() throws Exception {
        AuthState state = new AuthState(
                null,
                AuthorizationException.AuthorizationRequestErrors.INVALID_REQUEST);

        AuthState restored = AuthState.jsonDeserialize(state.jsonSerializeString());
        assertThat(restored.getAuthorizationException())
                .isEqualTo(state.getAuthorizationException());
    }

    @Test
    public void testHasClientSecretExpired() {
        RegistrationResponse regResp = getTestRegistrationResponseBuilder()
                .setClientSecret(TEST_CLIENT_SECRET)
                .setClientSecretExpiresAt(TWO_MINUTES)
                .build();
        AuthState state = new AuthState(regResp);

        // before the expiration time
        mClock.currentTime.set(ONE_SECOND);
        assertThat(state.hasClientSecretExpired(mClock)).isFalse();

        // on client_secret's actual expiration
        mClock.currentTime.set(TWO_MINUTES);
        assertThat(state.hasClientSecretExpired(mClock)).isTrue();

        // past client_secrets's actual expiration
        mClock.currentTime.set(TWO_MINUTES + ONE_SECOND);
        assertThat(state.hasClientSecretExpired(mClock)).isTrue();
    }

    @Test
    public void testCreateRequiredClientAuthentication_withoutClientCredentials() throws
            Exception {
        RegistrationResponse regResp = getTestRegistrationResponseBuilder().build();
        AuthState state = new AuthState(regResp);
        assertThat(state.getClientAuthentication())
                .isInstanceOf(NoClientAuthentication.class);
    }

    @Test
    public void testCreateRequiredClientAuthentication_withoutTokenEndpointAuthMethod() throws
            Exception {
        RegistrationResponse regResp = getTestRegistrationResponseBuilder()
                .setClientSecret(TEST_CLIENT_SECRET)
                .build();
        AuthState state = new AuthState(regResp);
        assertThat(state.getClientAuthentication())
                .isInstanceOf(ClientSecretBasic.class);
    }

    @Test
    public void testCreateRequiredClientAuthentication_withTokenEndpointAuthMethodNone() throws
            Exception {
        RegistrationResponse regResp = getTestRegistrationResponseBuilder()
                .setClientSecret(TEST_CLIENT_SECRET)
                .setTokenEndpointAuthMethod("none")
                .build();
        AuthState state = new AuthState(regResp);
        assertThat(state.getClientAuthentication())
                .isInstanceOf(NoClientAuthentication.class);
    }

    @Test
    public void testCreateRequiredClientAuthentication_withTokenEndpointAuthMethodBasic() throws
            Exception {
        RegistrationResponse regResp = getTestRegistrationResponseBuilder()
                .setClientSecret(TEST_CLIENT_SECRET)
                .setTokenEndpointAuthMethod(ClientSecretBasic.NAME)
                .build();
        AuthState state = new AuthState(regResp);
        assertThat(state.getClientAuthentication())
                .isInstanceOf(ClientSecretBasic.class);
    }

    @Test
    public void testCreateRequiredClientAuthentication_withTokenEndpointAuthMethodPost() throws
            Exception {
        RegistrationResponse regResp = getTestRegistrationResponseBuilder()
                .setClientSecret(TEST_CLIENT_SECRET)
                .setTokenEndpointAuthMethod(ClientSecretPost.NAME)
                .build();
        AuthState state = new AuthState(regResp);
        assertThat(state.getClientAuthentication())
                .isInstanceOf(ClientSecretPost.class);
    }

    @Test(expected = ClientAuthentication.UnsupportedAuthenticationMethod.class)
    public void testCreateRequiredClientAuthentication_withUnknownTokenEndpointAuthMethod()
            throws Exception {
        RegistrationResponse regResp = getTestRegistrationResponseBuilder()
                .setClientSecret(TEST_CLIENT_SECRET)
                .setTokenEndpointAuthMethod("unknown")
                .build();
        AuthState state = new AuthState(regResp);
        state.getClientAuthentication();
    }
}
