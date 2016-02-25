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

import static net.openid.appauth.TestValues.TEST_ACCESS_TOKEN;
import static net.openid.appauth.TestValues.TEST_APP_REDIRECT_URI;
import static net.openid.appauth.TestValues.TEST_AUTH_CODE;
import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.TEST_IDP_TOKEN_ENDPOINT;
import static net.openid.appauth.TestValues.TEST_ID_TOKEN;
import static net.openid.appauth.TestValues.TEST_REFRESH_TOKEN;
import static net.openid.appauth.TestValues.TEST_SCOPE;
import static net.openid.appauth.TestValues.TEST_STATE;
import static net.openid.appauth.TestValues.getTestAuthCodeExchangeRequest;
import static net.openid.appauth.TestValues.getTestAuthRequestBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.ComponentName;
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
import org.robolectric.RobolectricTestRunner;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AuthorizationServiceTest {
    private static final int CALLBACK_TIMEOUT_MILLIS = 1000;

    private static final int TEST_EXPIRES_IN = 3600;
    private static final String TEST_BROWSER_PACKAGE = "com.browser.test";
    private static final ComponentName TEST_CUSTOM_TABS_COMPONENT =
            new ComponentName(TEST_BROWSER_PACKAGE, ".CustomTabsService");

    private static final Map<String, String> TEST_ADDITIONAL_PARAMS;

    static {
        TEST_ADDITIONAL_PARAMS = new HashMap<>();
        TEST_ADDITIONAL_PARAMS.put("test_key1", "test_value1");
        TEST_ADDITIONAL_PARAMS.put("test_key2", "test_value2");
    }

    private static final String AUTH_CODE_EXCHANGE_RESPONSE_JSON = "{\n"
            + "  \"refresh_token\": \"" + TEST_REFRESH_TOKEN + "\",\n"
            + "  \"access_token\": \"" + TEST_ACCESS_TOKEN + "\",\n"
            + "  \"expires_in\": \"" + TEST_EXPIRES_IN + "\",\n"
            + "  \"id_token\": \"" + TEST_ID_TOKEN + "\",\n"
            + "  \"token_type\": \"" + AuthorizationResponse.TOKEN_TYPE_BEARER + "\"\n"
            + "}";

    private URL mUrl;
    private AuthorizationCallback mAuthCallback;
    private AuthorizationService mService;
    private InjectedUrlBuilder mBuilder;
    private OutputStream mOutputStream;
    private CustomTabsIntent.Builder mCustomTabsIntent;
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
        mBuilder = new InjectedUrlBuilder();
        mService = new AuthorizationService(mContext, mBuilder, mBrowserHandler);
        mOutputStream = new ByteArrayOutputStream();
        when(mHttpConnection.getOutputStream()).thenReturn(mOutputStream);
        when(mContext.bindService(serviceIntentEq(), any(CustomTabsServiceConnection.class),
                anyInt())).thenReturn(true);
        mCustomTabsIntent = new CustomTabsIntent.Builder(null);
        when(mBrowserHandler.createCustomTabsIntentBuilder()).thenReturn(mCustomTabsIntent);
        when(mBrowserHandler.getBrowserPackage()).thenReturn(TEST_BROWSER_PACKAGE);
    }

    @After
    public void tearDown() {
        PendingIntentStore.getInstance().clearPendingIntents();
    }

    @Test
    public void testAuthorizationRequest() throws Exception {
        AuthorizationRequest request = getTestAuthRequestBuilder()
                .setScope(TEST_SCOPE)
                .setState(TEST_STATE)
                .build();
        mService.performAuthorizationRequest(request, mPendingIntent);
        Intent intent = captureAuthRequestIntent();
        assertRequestIntent(intent, null, TEST_SCOPE, TEST_STATE);
        assertEquals(mPendingIntent, PendingIntentStore.getInstance().getPendingIntent(TEST_STATE));
    }

    @Test
    public void testAuthorizationRequest_withDefaultRandomState() throws Exception {
        AuthorizationRequest request = getTestAuthRequestBuilder().build();
        mService.performAuthorizationRequest(request, mPendingIntent);
        Intent intent = captureAuthRequestIntent();
        assertRequestIntent(intent, null, TEST_SCOPE, request.state);
        assertEquals(mPendingIntent,
                PendingIntentStore.getInstance().getPendingIntent(request.state));
    }

    @Test
    public void testAuthorizationRequest_withNoState() throws Exception {
        AuthorizationRequest request = getTestAuthRequestBuilder()
                .setState(null)
                .build();
        mService.performAuthorizationRequest(request, mPendingIntent);
        assertFalse(captureAuthRequestUri()
                .getQueryParameterNames()
                .contains(AuthorizationService.STATE));
        assertEquals(mPendingIntent, PendingIntentStore.getInstance().getPendingIntent(null));
    }

    @Test
    public void testAuthorizationRequest_withNoResponseModeSpecified() throws Exception {
        AuthorizationRequest request = getTestAuthRequestBuilder().build();
        mService.performAuthorizationRequest(request, mPendingIntent);
        assertFalse(captureAuthRequestUri()
                .getQueryParameterNames()
                .contains(AuthorizationService.RESPONSE_MODE));
    }

    @Test
    public void testAuthorizationRequest_withResponseMode() throws Exception {
        AuthorizationRequest request = getTestAuthRequestBuilder()
                .setResponseMode(AuthorizationRequest.RESPONSE_MODE_QUERY)
                .build();
        mService.performAuthorizationRequest(request, mPendingIntent);
        assertEquals(AuthorizationRequest.RESPONSE_MODE_QUERY,
                captureAuthRequestUri().getQueryParameter(AuthorizationService.RESPONSE_MODE));
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

    @Test
    public void testAuthorizationRequest_scopeEmpty() throws Exception {
        AuthorizationRequest request = getTestAuthRequestBuilder()
                .setScopes()
                .build();
        mService.performAuthorizationRequest(request, mPendingIntent);
        Uri requestUri = captureAuthRequestUri();
        assertFalse(requestUri.getQueryParameterNames().contains(AuthorizationService.SCOPE));
    }

    @Test
    public void testAuthorizationRequest_scopeNull() throws Exception {
        AuthorizationRequest request = getTestAuthRequestBuilder()
                .setScope(null)
                .build();
        mService.performAuthorizationRequest(request, mPendingIntent);
        assertFalse(captureAuthRequestUri()
                .getQueryParameterNames()
                .contains(AuthorizationService.SCOPE));
    }

    @Test
    public void testAuthorizationRequest_additionalParams() throws Exception {
        AuthorizationRequest request = getTestAuthRequestBuilder()
                .setState(TEST_STATE)
                .setAdditionalParameters(TEST_ADDITIONAL_PARAMS)
                .build();
        mService.performAuthorizationRequest(request, mPendingIntent);
        Uri requestUri = captureAuthRequestUri();
        for (String key : TEST_ADDITIONAL_PARAMS.keySet()) {
            assertEquals(TEST_ADDITIONAL_PARAMS.get(key), requestUri.getQueryParameter(key));
        }
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
        // mOutputStream contains the encoded query obtained via Uri.getEncodedQuery.
        // Use an empty helper uri to easily query the parameters.
        Uri uri = new Uri.Builder().encodedQuery(mOutputStream.toString()).build();
        assertCodeForTokenUri(uri);
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

    private Uri captureAuthRequestUri() {
        return captureAuthRequestIntent().getData();
    }

    private void assertTokenResponse(TokenResponse response, TokenRequest expectedRequest) {
        assertNotNull(response);
        assertEquals(expectedRequest, response.request);
        assertEquals(TEST_ACCESS_TOKEN, response.accessToken);
        assertEquals(TEST_REFRESH_TOKEN, response.refreshToken);
        assertEquals(AuthorizationResponse.TOKEN_TYPE_BEARER, response.tokenType);
        assertEquals(TEST_ID_TOKEN, response.idToken);
    }

    private void assertCodeForTokenUri(Uri uri) {
        assertEquals(TEST_CLIENT_ID, uri.getQueryParameter(AuthorizationService.CLIENT_ID));
        assertEquals(TEST_APP_REDIRECT_URI.toString(),
                uri.getQueryParameter(AuthorizationService.REDIRECT_URI));
        assertEquals(TEST_AUTH_CODE, uri.getQueryParameter(AuthorizationService.CODE));
        assertEquals(AuthorizationService.GRANT_TYPE_AUTH_CODE,
                uri.getQueryParameter(AuthorizationService.GRANT_TYPE));
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

    private void assertRequestIntent(Intent intent, Integer color, String scope, String state) {
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_NO_HISTORY) > 0);
        assertRequestUri(intent.getData(), scope, state);
        assertColorMatch(intent, color);
    }

    private void assertColorMatch(Intent intent, Integer expected) {
        int color = intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, Color.TRANSPARENT);
        assertTrue((expected == null) || ((expected == color) && (color != Color.TRANSPARENT)));
    }

    private void assertRequestUri(Uri uri, String scope, String state) {
        assertEquals(TEST_CLIENT_ID, uri.getQueryParameter(AuthorizationService.CLIENT_ID));
        assertEquals(TEST_APP_REDIRECT_URI.toString(),
                uri.getQueryParameter(AuthorizationService.REDIRECT_URI));
        assertEquals(AuthorizationRequest.RESPONSE_TYPE_CODE,
                uri.getQueryParameter(AuthorizationService.RESPONSE_TYPE));

        if (scope == null) {
            assertFalse("scope parameter should not be defined",
                        uri.getQueryParameterNames().contains(AuthorizationService.SCOPE));
        } else {
            assertEquals(scope, uri.getQueryParameter(AuthorizationService.SCOPE));
        }

        if (state == null) {
            assertFalse("state parameter should not be defined",
                        uri.getQueryParameterNames().contains(AuthorizationService.STATE));
        } else {
            assertEquals(state, uri.getQueryParameter(AuthorizationService.STATE));
        }
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
