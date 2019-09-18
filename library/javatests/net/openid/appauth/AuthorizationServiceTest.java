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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;

import net.openid.appauth.AppAuthConfiguration.Builder;
import net.openid.appauth.AuthorizationException.GeneralErrors;
import net.openid.appauth.browser.BrowserDescriptor;
import net.openid.appauth.browser.Browsers;
import net.openid.appauth.browser.CustomTabManager;
import net.openid.appauth.connectivity.ConnectionBuilder;
import net.openid.appauth.internal.UriUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.support.customtabs.CustomTabsIntent.EXTRA_TITLE_VISIBILITY_STATE;
import static android.support.customtabs.CustomTabsIntent.EXTRA_TOOLBAR_COLOR;
import static net.openid.appauth.AuthorizationManagementActivity.KEY_AUTH_INTENT;
import static net.openid.appauth.AuthorizationManagementActivity.KEY_AUTH_REQUEST;
import static net.openid.appauth.AuthorizationManagementActivity.KEY_CANCEL_INTENT;
import static net.openid.appauth.AuthorizationManagementActivity.KEY_COMPLETE_INTENT;
import static net.openid.appauth.TestValues.TEST_ACCESS_TOKEN;
import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.TEST_CLIENT_SECRET;
import static net.openid.appauth.TestValues.TEST_CLIENT_SECRET_EXPIRES_AT;
import static net.openid.appauth.TestValues.TEST_ID_TOKEN;
import static net.openid.appauth.TestValues.TEST_NONCE;
import static net.openid.appauth.TestValues.TEST_REFRESH_TOKEN;
import static net.openid.appauth.TestValues.TEST_STATE;
import static net.openid.appauth.TestValues.getTestAuthCodeExchangeRequest;
import static net.openid.appauth.TestValues.getTestAuthCodeExchangeRequestBuilder;
import static net.openid.appauth.TestValues.getTestAuthRequestBuilder;
import static net.openid.appauth.TestValues.getTestIdTokenWithNonce;
import static net.openid.appauth.TestValues.getTestRegistrationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=16)
public class AuthorizationServiceTest {
    private static final int CALLBACK_TIMEOUT_MILLIS = 1000;

    private static final int TEST_EXPIRES_IN = 3600;
    private static final String TEST_BROWSER_PACKAGE = "com.browser.test";

    private static final String REGISTRATION_RESPONSE_JSON = "{\n"
            + " \"client_id\": \"" + TEST_CLIENT_ID + "\",\n"
            + " \"client_secret\": \"" + TEST_CLIENT_SECRET + "\",\n"
            + " \"client_secret_expires_at\": \"" + TEST_CLIENT_SECRET_EXPIRES_AT + "\",\n"
            + " \"application_type\": " + RegistrationRequest.APPLICATION_TYPE_NATIVE + "\n"
            + "}";

    public static final String BAD_REQUEST_RESPONSE = "<!DOCTYPE html>\n" +
        "<html lang=\"en\">\n" +
        "<head>\n" +
        "  <meta charset=\"utf-8\" />\n" +
        "  <title>Exception caught</title>\n" +
        "  <style>\n" +
        "    body {\n" +
        "      background-color: #FAFAFA;\n" +
        "      color: #333;\n" +
        "      margin: 0px;\n" +
        "    }\n" +
        "\n" +
        "  </style>\n" +
        "\n" +
        "\n" +
        "</head>\n" +
        "<body>\n" +
        "\n" +
        "<header>\n" +
        "  <h1>Network Error</h1>\n" +
        "</header>\n" +
        "<div id=\"container\">\n" +
        "  <h2>Bad Request</h2>\n" +
        "\n" +
        "</div>\n" +
        "\n" +
        "</body>\n" +
        "</html>";

    public static final String BAD_REQUEST_ERROR_MESSAGE = "Bad Request";

    private static final int TEST_INVALID_GRANT_CODE = 2002;

    private AuthorizationCallback mAuthCallback;
    private RegistrationCallback mRegistrationCallback;
    private AuthorizationService mService;
    private OutputStream mOutputStream;
    private BrowserDescriptor mBrowserDescriptor;
    @Mock ConnectionBuilder mConnectionBuilder;
    @Mock HttpURLConnection mHttpConnection;
    @Mock PendingIntent mPendingIntent;
    @Mock Context mContext;
    @Mock CustomTabsClient mClient;
    @Mock CustomTabManager mCustomTabManager;

    @Before
    @SuppressWarnings("ResourceType")
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mAuthCallback = new AuthorizationCallback();
        mRegistrationCallback = new RegistrationCallback();
        mBrowserDescriptor = Browsers.Chrome.customTab("46");
        mService = new AuthorizationService(
                mContext,
                new Builder()
                        .setConnectionBuilder(mConnectionBuilder)
                        .build(),
                mBrowserDescriptor,
                mCustomTabManager);
        mOutputStream = new ByteArrayOutputStream();
        when(mConnectionBuilder.openConnection(any(Uri.class))).thenReturn(mHttpConnection);
        when(mHttpConnection.getOutputStream()).thenReturn(mOutputStream);
        when(mContext.bindService(serviceIntentEq(), any(CustomTabsServiceConnection.class),
                anyInt())).thenReturn(true);
        when(mCustomTabManager.createTabBuilder())
                .thenReturn(new CustomTabsIntent.Builder());
    }

    @Test
    public void testAuthorizationRequest_withSpecifiedState() throws Exception {
        AuthorizationRequest request = getTestAuthRequestBuilder()
                .setState(TEST_STATE)
                .build();
        mService.performAuthorizationRequest(request, mPendingIntent);
        Intent intent = captureAuthRequestIntent();
        assertRequestIntent(intent, null);
        assertEquals(request.toUri().toString(), intent.getData().toString());
    }

    @Test
    public void testAuthorizationRequest_withSpecifiedNonce() throws Exception {
        AuthorizationRequest request = getTestAuthRequestBuilder()
            .setNonce(TEST_NONCE)
            .build();
        mService.performAuthorizationRequest(request, mPendingIntent);
        Intent intent = captureAuthRequestIntent();
        assertRequestIntent(intent, null);
        assertEquals(request.toUri().toString(), intent.getData().toString());
    }

    @Test
    public void testAuthorizationRequest_withDefaultRandomStateAndNonce() throws Exception {
        AuthorizationRequest request = getTestAuthRequestBuilder().build();
        mService.performAuthorizationRequest(request, mPendingIntent);
        Intent intent = captureAuthRequestIntent();
        assertRequestIntent(intent, null);
    }

    @Test
    public void testAuthorizationRequest_customization() throws Exception {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setToolbarColor(Color.GREEN)
                .build();
        mService.performAuthorizationRequest(
                getTestAuthRequestBuilder().build(),
                mPendingIntent,
                customTabsIntent);
        Intent intent = captureAuthRequestIntent();
        assertColorMatch(intent, Color.GREEN);
    }

    @Test(expected = IllegalStateException.class)
    public void testAuthorizationRequest_afterDispose() throws Exception {
        mService.dispose();
        mService.performAuthorizationRequest(getTestAuthRequestBuilder().build(), mPendingIntent);
    }

    @Test
    public void testGetAuthorizationRequestIntent_preservesRequest() {
        AuthorizationRequest request = getTestAuthRequestBuilder().build();
        Intent intent = mService.getAuthorizationRequestIntent(request);
        assertThat(intent.hasExtra(KEY_AUTH_INTENT)).isTrue();
        assertThat(intent.getStringExtra(KEY_AUTH_REQUEST))
                .isEqualTo(request.jsonSerializeString());
    }

    @Test
    public void testGetAuthorizationRequestIntent_doesNotInitPendingIntents() {
        AuthorizationRequest request = getTestAuthRequestBuilder().build();
        Intent intent = mService.getAuthorizationRequestIntent(request);
        Intent actualAuthIntent = intent.getParcelableExtra(KEY_AUTH_INTENT);
        assertThat(actualAuthIntent.<Intent>getParcelableExtra(KEY_COMPLETE_INTENT)).isNull();
        assertThat(actualAuthIntent.<Intent>getParcelableExtra(KEY_CANCEL_INTENT)).isNull();
    }

    @Test
    public void testGetAuthorizationRequestIntent_withCustomTabs_preservesTabSettings() {
        AuthorizationRequest request = getTestAuthRequestBuilder().build();
        @ColorInt int toolbarColor = Color.GREEN;
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setToolbarColor(toolbarColor)
                .setShowTitle(true)
                .build();

        Intent intent = mService.getAuthorizationRequestIntent(request, customTabsIntent);
        Intent actualAuthIntent = intent.getParcelableExtra(KEY_AUTH_INTENT);
        assertThat(actualAuthIntent.getIntExtra(EXTRA_TOOLBAR_COLOR, 0)).isEqualTo(toolbarColor);
        assertThat(actualAuthIntent.getIntExtra(EXTRA_TITLE_VISIBILITY_STATE, 0))
            .isEqualTo(CustomTabsIntent.SHOW_PAGE_TITLE);
    }

    @Test
    public void testTokenRequest() throws Exception {
        InputStream is = new ByteArrayInputStream(getAuthCodeExchangeResponseJson().getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);
        when(mHttpConnection.getRequestProperty("Accept")).thenReturn(null);
        when(mHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        TokenRequest request = getTestAuthCodeExchangeRequest();
        mService.performTokenRequest(request, mAuthCallback);
        mAuthCallback.waitForCallback();
        assertTokenResponse(mAuthCallback.response, request);
        String postBody = mOutputStream.toString();

        // by default, we set application/json as an acceptable response type if a value was not
        // already set
        verify(mHttpConnection).setRequestProperty("Accept", "application/json");

        Map<String, String> params = UriUtil.formUrlDecodeUnique(postBody);

        for (Map.Entry<String, String> requestParam : request.getRequestParameters().entrySet()) {
            assertThat(params).containsEntry(requestParam.getKey(), requestParam.getValue());
        }

        assertThat(params).containsEntry(TokenRequest.PARAM_CLIENT_ID, request.clientId);
    }

    @Test
    public void testTokenRequest_withNonceValidation() throws Exception {
        String idToken = getTestIdTokenWithNonce(TEST_NONCE);
        InputStream is = new ByteArrayInputStream(
            getAuthCodeExchangeResponseJson(idToken).getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);
        when(mHttpConnection.getRequestProperty("Accept")).thenReturn(null);
        when(mHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        TokenRequest request = getTestAuthCodeExchangeRequestBuilder()
                .setNonce(TEST_NONCE)
                .build();
        mService.performTokenRequest(request, mAuthCallback);
        mAuthCallback.waitForCallback();
        assertTokenResponse(mAuthCallback.response, request, idToken);
    }

    @Test
    public void testTokenRequest_clientSecretBasicAuth() throws Exception {
        InputStream is = new ByteArrayInputStream(getAuthCodeExchangeResponseJson().getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);
        when(mHttpConnection.getRequestProperty("Accept")).thenReturn(null);
        when(mHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        TokenRequest request = getTestAuthCodeExchangeRequest();

        ClientSecretBasic clientAuth = new ClientSecretBasic("SUPER_SECRET");
        mService.performTokenRequest(request, clientAuth, mAuthCallback);
        mAuthCallback.waitForCallback();
        assertTokenResponse(mAuthCallback.response, request);
        String postBody = mOutputStream.toString();


        // client secret basic does not send the client ID in the body - explicitly check for
        // this as a possible regression, as this can break integration with IDPs if present.
        Map<String, String> params = UriUtil.formUrlDecodeUnique(postBody);
        assertThat(params).doesNotContainKey(TokenRequest.PARAM_CLIENT_ID);
    }

    @Test
    public void testTokenRequest_leaveExistingAcceptUntouched() throws Exception {
        InputStream is = new ByteArrayInputStream(getAuthCodeExchangeResponseJson().getBytes());

        // emulate some content types having already been set as an Accept value
        when(mHttpConnection.getRequestProperty("Accept"))
                .thenReturn("text/plain");

        when(mHttpConnection.getInputStream()).thenReturn(is);
        when(mHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        TokenRequest request = getTestAuthCodeExchangeRequest();
        mService.performTokenRequest(request, mAuthCallback);

        // application/json should be added after the existing string
        verify(mHttpConnection, never()).setRequestProperty(eq("Accept"), any(String.class));
    }

    @Test
    public void testTokenRequest_withBasicAuth() throws Exception {
        ClientSecretBasic csb = new ClientSecretBasic(TEST_CLIENT_SECRET);
        InputStream is = new ByteArrayInputStream(getAuthCodeExchangeResponseJson().getBytes());
        when(mHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(mHttpConnection.getInputStream()).thenReturn(is);
        TokenRequest request = getTestAuthCodeExchangeRequest();
        mService.performTokenRequest(request, csb, mAuthCallback);
        mAuthCallback.waitForCallback();
        assertTokenResponse(mAuthCallback.response, request);
        String postBody = mOutputStream.toString();
        assertTokenRequestBody(postBody, request.getRequestParameters());
        verify(mHttpConnection).setRequestProperty("Authorization",
                csb.getRequestHeaders(TEST_CLIENT_ID).get("Authorization"));
    }

    @Test
    public void testTokenRequest_withPostAuth() throws Exception {
        ClientSecretPost csp = new ClientSecretPost(TEST_CLIENT_SECRET);
        InputStream is = new ByteArrayInputStream(getAuthCodeExchangeResponseJson().getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);
        when(mHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        TokenRequest request = getTestAuthCodeExchangeRequest();
        mService.performTokenRequest(request, csp, mAuthCallback);
        mAuthCallback.waitForCallback();
        assertTokenResponse(mAuthCallback.response, request);

        String postBody = mOutputStream.toString();
        Map<String, String> expectedRequestBody = request.getRequestParameters();
        expectedRequestBody.putAll(csp.getRequestParameters(TEST_CLIENT_ID));
        assertTokenRequestBody(postBody, expectedRequestBody);
    }

    @Test
    public void testTokenRequest_withBadRequest() throws Exception {
        ClientSecretPost csp = new ClientSecretPost(TEST_CLIENT_SECRET);
        InputStream is = new ByteArrayInputStream(BAD_REQUEST_RESPONSE.getBytes());
        when(mHttpConnection.getErrorStream()).thenReturn(is);
        when(mHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(mHttpConnection.getResponseMessage()).thenReturn(BAD_REQUEST_ERROR_MESSAGE);
        TokenRequest request = getTestAuthCodeExchangeRequest();
        mService.performTokenRequest(request, csp, mAuthCallback);
        mAuthCallback.waitForCallback();
        assertBadRequest(mAuthCallback.error);
    }

    @Test
    public void testTokenRequest_IoException() throws Exception {
        Exception ex = new IOException();
        when(mHttpConnection.getInputStream()).thenThrow(ex);
        when(mHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        mService.performTokenRequest(getTestAuthCodeExchangeRequest(), mAuthCallback);
        mAuthCallback.waitForCallback();
        assertNotNull(mAuthCallback.error);
        assertEquals(GeneralErrors.NETWORK_ERROR, mAuthCallback.error);
    }

    @Test
    public void testRegistrationRequest() throws Exception {
        InputStream is = new ByteArrayInputStream(REGISTRATION_RESPONSE_JSON.getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);
        RegistrationRequest request = getTestRegistrationRequest();
        mService.performRegistrationRequest(request, mRegistrationCallback);
        mRegistrationCallback.waitForCallback();
        assertRegistrationResponse(mRegistrationCallback.response, request);
        String postBody = mOutputStream.toString();
        assertThat(postBody).isEqualTo(request.toJsonString());
    }

    @Test
    public void testRegistrationRequest_withBadRequest() throws Exception {
        InputStream is = new ByteArrayInputStream(BAD_REQUEST_RESPONSE.getBytes());
        when(mHttpConnection.getErrorStream()).thenReturn(is);
        when(mHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(mHttpConnection.getResponseMessage()).thenReturn(AuthorizationServiceTest.BAD_REQUEST_ERROR_MESSAGE);
        RegistrationRequest request = getTestRegistrationRequest();
        mService.performRegistrationRequest(request, mRegistrationCallback);
        mRegistrationCallback.waitForCallback();
        assertBadRequest(mRegistrationCallback.error);
    }

    @Test
    public void testRegistrationRequest_IoException() throws Exception {
        Exception ex = new IOException();
        when(mHttpConnection.getInputStream()).thenThrow(ex);
        mService.performRegistrationRequest(getTestRegistrationRequest(), mRegistrationCallback);
        mRegistrationCallback.waitForCallback();
        assertNotNull(mRegistrationCallback.error);
        assertEquals(GeneralErrors.NETWORK_ERROR, mRegistrationCallback.error);
    }

    @Test(expected = IllegalStateException.class)
    public void testTokenRequest_afterDispose() throws Exception {
        mService.dispose();
        mService.performTokenRequest(getTestAuthCodeExchangeRequest(), mAuthCallback);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateCustomTabsIntentBuilder_afterDispose() throws Exception {
        mService.dispose();
        mService.createCustomTabsIntentBuilder();
    }

    @Test
    public void testGetBrowserDescriptor_browserAvailable() {
        assertEquals(mService.getBrowserDescriptor(), mBrowserDescriptor);
    }

    private Intent captureAuthRequestIntent() {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());

        // the real auth intent is wrapped in the intent by AuthorizationManagementActivity
        return intentCaptor
                .getValue()
                .getParcelableExtra(KEY_AUTH_INTENT);
    }

    private void assertTokenResponse(TokenResponse response, TokenRequest expectedRequest) {
        assertTokenResponse(response, expectedRequest, TEST_ID_TOKEN);
    }

    private void assertTokenResponse(
            TokenResponse response,
            TokenRequest expectedRequest,
            String idToken) {
        assertNotNull(response);
        assertEquals(expectedRequest, response.request);
        assertEquals(TEST_ACCESS_TOKEN, response.accessToken);
        assertEquals(TEST_REFRESH_TOKEN, response.refreshToken);
        assertEquals(AuthorizationResponse.TOKEN_TYPE_BEARER, response.tokenType);
        assertEquals(idToken, response.idToken);
    }

    private void assertBadRequest(AuthorizationException error) {
        assertNotNull(error);
        assertEquals(AuthorizationException.TYPE_HTTP_ERROR, error.type);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, error.code);
        assertEquals(BAD_REQUEST_ERROR_MESSAGE, error.error);
        assertEquals(BAD_REQUEST_RESPONSE, error.errorDescription);
    }

    private void assertRegistrationResponse(RegistrationResponse response,
                                            RegistrationRequest expectedRequest) {
        assertThat(response).isNotNull();
        assertThat(response.request).isEqualTo(expectedRequest);
        assertThat(response.clientId).isEqualTo(TEST_CLIENT_ID);
        assertThat(response.clientSecret).isEqualTo(TEST_CLIENT_SECRET);
        assertThat(response.clientSecretExpiresAt).isEqualTo(TEST_CLIENT_SECRET_EXPIRES_AT);
    }

    private void assertTokenRequestBody(
            String requestBody, Map<String, String> expectedParameters) {
        Uri postBody = new Uri.Builder().encodedQuery(requestBody).build();
        for (Map.Entry<String, String> param : expectedParameters.entrySet()) {
            assertThat(postBody.getQueryParameter(param.getKey())).isEqualTo(param.getValue());
        }
    }

    private static class AuthorizationCallback implements
            AuthorizationService.TokenResponseCallback {
        private Semaphore mSemaphore = new Semaphore(0);
        public TokenResponse response;
        public AuthorizationException error;

        @Override
        public void onTokenRequestCompleted(
                @Nullable TokenResponse tokenResponse,
                @Nullable AuthorizationException ex) {
            assertTrue((tokenResponse == null) ^ (ex == null));
            this.response = tokenResponse;
            this.error = ex;
            mSemaphore.release();
        }

        public void waitForCallback() throws Exception {
            assertTrue(mSemaphore.tryAcquire(CALLBACK_TIMEOUT_MILLIS,
                    TimeUnit.MILLISECONDS));
        }
    }

    private static class RegistrationCallback implements
            AuthorizationService.RegistrationResponseCallback {
        private Semaphore mSemaphore = new Semaphore(0);
        public RegistrationResponse response;
        public AuthorizationException error;

        @Override
        public void onRegistrationRequestCompleted(
                @Nullable RegistrationResponse registrationResponse,
                @Nullable AuthorizationException ex) {
            assertTrue((registrationResponse == null) ^ (ex == null));
            this.response = registrationResponse;
            this.error = ex;
            mSemaphore.release();
        }

        public void waitForCallback() throws Exception {
            assertTrue(mSemaphore.tryAcquire(CALLBACK_TIMEOUT_MILLIS,
                    TimeUnit.MILLISECONDS));
        }
    }

    private void assertRequestIntent(Intent intent, Integer color) {
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertColorMatch(intent, color);
    }

    private void assertColorMatch(Intent intent, Integer expected) {
        int color = intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, Color.TRANSPARENT);
        assertTrue((expected == null) || ((expected == color) && (color != Color.TRANSPARENT)));
    }

    /**
     * Custom matcher for verifying the intent fired during token request.
     */
    private static class CustomTabsServiceMatcher implements ArgumentMatcher<Intent> {

        CustomTabsServiceMatcher() { }

        @Override
        public boolean matches(Intent intent) {
            assertNotNull(intent);
            return TEST_BROWSER_PACKAGE.equals(intent.getPackage());
        }
    }

    static Intent serviceIntentEq() {
        return argThat(new CustomTabsServiceMatcher());
    }

    String getAuthCodeExchangeResponseJson() {
        return getAuthCodeExchangeResponseJson(null);
    }

    String getAuthCodeExchangeResponseJson(@Nullable String idToken) {
        if (idToken == null) {
            idToken = TEST_ID_TOKEN;
        }
        return "{\n"
                + "  \"refresh_token\": \"" + TEST_REFRESH_TOKEN + "\",\n"
                + "  \"access_token\": \"" + TEST_ACCESS_TOKEN + "\",\n"
                + "  \"expires_in\": \"" + TEST_EXPIRES_IN + "\",\n"
                + "  \"id_token\": \"" + idToken + "\",\n"
                + "  \"token_type\": \"" + AuthorizationResponse.TOKEN_TYPE_BEARER + "\"\n"
                + "}";
    }
}
