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
import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.TEST_CLIENT_SECRET;
import static net.openid.appauth.TestValues.TEST_CLIENT_SECRET_EXPIRES_AT;
import static net.openid.appauth.TestValues.TEST_IDP_REGISTRATION_ENDPOINT;
import static net.openid.appauth.TestValues.TEST_IDP_TOKEN_ENDPOINT;
import static net.openid.appauth.TestValues.TEST_ID_TOKEN;
import static net.openid.appauth.TestValues.TEST_REFRESH_TOKEN;
import static net.openid.appauth.TestValues.TEST_STATE;
import static net.openid.appauth.TestValues.getTestAuthCodeExchangeRequest;
import static net.openid.appauth.TestValues.getTestAuthRequestBuilder;
import static net.openid.appauth.TestValues.getTestRegistrationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;

import net.openid.appauth.AuthorizationException.GeneralErrors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class AuthorizationServiceTest {
    private static final int CALLBACK_TIMEOUT_MILLIS = 1000;

    private static final int TEST_EXPIRES_IN = 3600;
    private static final String TEST_BROWSER_PACKAGE = "com.browser.test";

    private static final String AUTH_CODE_EXCHANGE_RESPONSE_JSON = "{\n"
            + "  \"refresh_token\": \"" + TEST_REFRESH_TOKEN + "\",\n"
            + "  \"access_token\": \"" + TEST_ACCESS_TOKEN + "\",\n"
            + "  \"expires_in\": \"" + TEST_EXPIRES_IN + "\",\n"
            + "  \"id_token\": \"" + TEST_ID_TOKEN + "\",\n"
            + "  \"token_type\": \"" + AuthorizationResponse.TOKEN_TYPE_BEARER + "\"\n"
            + "}";

    private static final String REGISTRATION_RESPONSE_JSON = "{\n"
            + " \"client_id\": \"" + TEST_CLIENT_ID + "\",\n"
            + " \"client_secret\": \"" + TEST_CLIENT_SECRET + "\",\n"
            + " \"client_secret_expires_at\": \"" + TEST_CLIENT_SECRET_EXPIRES_AT + "\",\n"
            + " \"application_type\": " + RegistrationRequest.APPLICATION_TYPE_NATIVE + "\n"
            + "}";

    private URL mUrl;
    private AuthorizationCallback mAuthCallback;
    private RegistrationCallback mRegistrationCallback;
    private AuthorizationService mService;
    private InjectedUrlBuilder mBuilder;
    private OutputStream mOutputStream;
    @Mock HttpURLConnection mHttpConnection;
    @Mock PendingIntent mPendingIntent;
    @Mock Context mContext;
    @Mock CustomTabsClient mClient;
    @Mock BrowserHandler mBrowserHandler;

    @Before
    @SuppressWarnings("ResourceType")
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        PendingIntentStore.getInstance().clearPendingIntents();
        URLStreamHandler urlStreamHandler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) throws IOException {
                return mHttpConnection;
            }
        };
        mUrl = new URL("foo", "bar", -1, "/foobar", urlStreamHandler);
        mAuthCallback = new AuthorizationCallback();
        mRegistrationCallback = new RegistrationCallback();
        mBuilder = new InjectedUrlBuilder();
        mService = new AuthorizationService(mContext, mBuilder, mBrowserHandler);
        mOutputStream = new ByteArrayOutputStream();
        when(mHttpConnection.getOutputStream()).thenReturn(mOutputStream);
        when(mContext.bindService(serviceIntentEq(), any(CustomTabsServiceConnection.class),
                anyInt())).thenReturn(true);
        when(mBrowserHandler.createCustomTabsIntentBuilder())
                .thenReturn(new CustomTabsIntent.Builder());
        when(mBrowserHandler.getBrowserPackage()).thenReturn(TEST_BROWSER_PACKAGE);
    }

    @After
    public void tearDown() {
        PendingIntentStore.getInstance().clearPendingIntents();
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
        assertEquals(mPendingIntent, PendingIntentStore.getInstance().getPendingIntent(TEST_STATE));
    }

    @Test
    public void testAuthorizationRequest_withDefaultRandomState() throws Exception {
        AuthorizationRequest request = getTestAuthRequestBuilder().build();
        mService.performAuthorizationRequest(request, mPendingIntent);
        Intent intent = captureAuthRequestIntent();
        assertRequestIntent(intent, null);
        assertEquals(mPendingIntent,
                PendingIntentStore.getInstance().getPendingIntent(request.state));
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
    public void testTokenRequest() throws Exception {
        InputStream is = new ByteArrayInputStream(AUTH_CODE_EXCHANGE_RESPONSE_JSON.getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);
        TokenRequest request = getTestAuthCodeExchangeRequest();
        mService.performTokenRequest(request, mAuthCallback);
        mAuthCallback.waitForCallback();
        assertTokenResponse(mAuthCallback.response, request);
        String postBody = mOutputStream.toString();
        assertThat(postBody).isEqualTo(UriUtil.formUrlEncode(request.getRequestParameters()));
        assertEquals(TEST_IDP_TOKEN_ENDPOINT.toString(), mBuilder.mUri);
    }

    @Test
    public void testTokenRequest_withBasicAuth() throws Exception {
        ClientSecretBasic csb = new ClientSecretBasic(TEST_CLIENT_SECRET);
        InputStream is = new ByteArrayInputStream(AUTH_CODE_EXCHANGE_RESPONSE_JSON.getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);
        TokenRequest request = getTestAuthCodeExchangeRequest();
        mService.performTokenRequest(request, csb, mAuthCallback);
        mAuthCallback.waitForCallback();
        assertTokenResponse(mAuthCallback.response, request);
        String postBody = mOutputStream.toString();
        assertTokenRequestBody(postBody, request.getRequestParameters());
        assertEquals(TEST_IDP_TOKEN_ENDPOINT.toString(), mBuilder.mUri);
        verify(mHttpConnection).setRequestProperty("Authorization",
                csb.getRequestHeaders(TEST_CLIENT_ID).get("Authorization"));
    }

    @Test
    public void testTokenRequest_withPostAuth() throws Exception {
        ClientSecretPost csp = new ClientSecretPost(TEST_CLIENT_SECRET);
        InputStream is = new ByteArrayInputStream(AUTH_CODE_EXCHANGE_RESPONSE_JSON.getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);
        TokenRequest request = getTestAuthCodeExchangeRequest();
        mService.performTokenRequest(request, csp, mAuthCallback);
        mAuthCallback.waitForCallback();
        assertTokenResponse(mAuthCallback.response, request);

        String postBody = mOutputStream.toString();
        Map<String, String> expectedRequestBody = request.getRequestParameters();
        expectedRequestBody.putAll(csp.getRequestParameters(TEST_CLIENT_ID));
        assertTokenRequestBody(postBody, expectedRequestBody);
        assertEquals(TEST_IDP_TOKEN_ENDPOINT.toString(), mBuilder.mUri);
    }

    @Test
    public void testTokenRequest_IoException() throws Exception {
        Exception ex = new IOException();
        when(mHttpConnection.getInputStream()).thenThrow(ex);
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
        assertEquals(TEST_IDP_REGISTRATION_ENDPOINT.toString(), mBuilder.mUri);
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

    private Intent captureAuthRequestIntent() {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());
        return intentCaptor.getValue();
    }

    private void assertTokenResponse(TokenResponse response, TokenRequest expectedRequest) {
        assertNotNull(response);
        assertEquals(expectedRequest, response.request);
        assertEquals(TEST_ACCESS_TOKEN, response.accessToken);
        assertEquals(TEST_REFRESH_TOKEN, response.refreshToken);
        assertEquals(AuthorizationResponse.TOKEN_TYPE_BEARER, response.tokenType);
        assertEquals(TEST_ID_TOKEN, response.idToken);
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

    private class InjectedUrlBuilder implements AuthorizationService.UrlBuilder {
        public String mUri;

        public URL buildUrlFromString(String uri) throws IOException {
            mUri = uri;
            return mUrl;
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
        assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_NO_HISTORY) > 0);
        assertColorMatch(intent, color);
    }

    private void assertColorMatch(Intent intent, Integer expected) {
        int color = intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, Color.TRANSPARENT);
        assertTrue((expected == null) || ((expected == color) && (color != Color.TRANSPARENT)));
    }

    /**
     * Custom matcher for verifying the intent fired during token request.
     */
    private static class CustomTabsServiceMatcher extends ArgumentMatcher<Intent> {

        CustomTabsServiceMatcher() { }

        @Override
        public boolean matches(Object actual) {
            Intent intent = (Intent) actual;
            assertNotNull(intent);
            return TEST_BROWSER_PACKAGE.equals(intent.getPackage());
        }
    }

    static Intent serviceIntentEq() {
        return argThat(new CustomTabsServiceMatcher());
    }
}
