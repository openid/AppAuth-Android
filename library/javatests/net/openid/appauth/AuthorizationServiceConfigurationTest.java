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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.support.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import net.openid.appauth.AuthorizationException.GeneralErrors;
import net.openid.appauth.connectivity.ConnectionBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=16)
public class AuthorizationServiceConfigurationTest {
    private static final int CALLBACK_TIMEOUT_MILLIS = 1000;
    private static final String TEST_NAME = "test_name";
    private static final String TEST_ISSUER = "test_issuer";
    private static final String TEST_AUTH_ENDPOINT = "https://test.openid.com/o/oauth/auth";
    private static final String TEST_TOKEN_ENDPOINT = "https://test.openid.com/o/oauth/token";
    private static final String TEST_REGISTRATION_ENDPOINT = "https://test.openid.com/o/oauth/registration";
    private static final String TEST_USERINFO_ENDPOINT = "https://test.openid.com/o/oauth/userinfo";
    private static final String TEST_JWKS_URI = "https://test.openid.com/o/oauth/jwks";
    private static final List<String> TEST_RESPONSE_TYPE_SUPPORTED = Arrays.asList("code", "token");
    private static final List<String> TEST_SUBJECT_TYPES_SUPPORTED = Arrays.asList("public");
    private static final List<String> TEST_ID_TOKEN_SIGNING_ALG_VALUES = Arrays.asList("RS256");
    private static final List<String> TEST_SCOPES_SUPPORTED = Arrays.asList("openid", "profile");
    private static final List<String> TEST_TOKEN_ENDPOINT_AUTH_METHODS
            = Arrays.asList("client_secret_post", "client_secret_basic");
    private static final List<String> TEST_CLAIMS_SUPPORTED = Arrays.asList("aud", "exp");
    private static final Uri TEST_DISCOVERY_URI = Uri.parse(
            "https://test.openid.com/.well-known/openid-configuration");
    static final String TEST_JSON = "{\n"
            + " \"issuer\": \"" + TEST_ISSUER + "\",\n"
            + " \"authorization_endpoint\": \"" + TEST_AUTH_ENDPOINT + "\",\n"
            + " \"token_endpoint\": \"" + TEST_TOKEN_ENDPOINT + "\",\n"
            + " \"registration_endpoint\": \"" + TEST_REGISTRATION_ENDPOINT + "\",\n"
            + " \"userinfo_endpoint\": \"" + TEST_USERINFO_ENDPOINT + "\",\n"
            + " \"jwks_uri\": \"" + TEST_JWKS_URI + "\",\n"
            + " \"response_types_supported\": " + toJson(TEST_RESPONSE_TYPE_SUPPORTED) + ",\n"
            + " \"subject_types_supported\": " + toJson(TEST_SUBJECT_TYPES_SUPPORTED) + ",\n"
            + " \"id_token_signing_alg_values_supported\": "
            + toJson(TEST_ID_TOKEN_SIGNING_ALG_VALUES) + ",\n"
            + " \"scopes_supported\": " + toJson(TEST_SCOPES_SUPPORTED) + ",\n"
            + " \"token_endpoint_auth_methods_supported\": "
            + toJson(TEST_TOKEN_ENDPOINT_AUTH_METHODS) + ",\n"
            + " \"claims_supported\": " + toJson(TEST_CLAIMS_SUPPORTED) + "\n"
            + "}";

    private static final String TEST_JSON_MALFORMED = "{\n"
            + " \"issuer\": \"" + TEST_ISSUER + "\",\n"
            + " \"authorization_endpoint\": \"" + TEST_AUTH_ENDPOINT + "\",\n"
            + " \"token_endpoint\": \"" + TEST_TOKEN_ENDPOINT + "\",\n"
            + " \"userinfo_endpoint\": \"" + TEST_USERINFO_ENDPOINT + "\",\n"
            + " \"jwks_uri\": \"" + TEST_JWKS_URI + "\",\n"
            + " \"response_types_supported\": " + toJson(TEST_RESPONSE_TYPE_SUPPORTED) + ",\n"
            + " \"subject_types_supported\": " + toJson(TEST_SUBJECT_TYPES_SUPPORTED) + ",\n"
            + " \"id_token_signing_alg_values_supported\": "
            + toJson(TEST_ID_TOKEN_SIGNING_ALG_VALUES) + ",\n"
            + " \"scopes_supported\": " + toJson(TEST_SCOPES_SUPPORTED) + ",\n"
            + " \"token_endpoint_auth_methods_supported\": "
            + toJson(TEST_TOKEN_ENDPOINT_AUTH_METHODS) + ",\n"
            + " \"claims_supported\": " + toJson(TEST_CLAIMS_SUPPORTED) + ",\n"
            + "}";

    private static final String TEST_JSON_MISSING_ARGUMENT = "{\n"
            + " \"issuer\": \"" + TEST_ISSUER + "\",\n"
            + " \"authorization_endpoint\": \"" + TEST_AUTH_ENDPOINT + "\",\n"
            + " \"token_endpoint\": \"" + TEST_TOKEN_ENDPOINT + "\",\n"
            + " \"userinfo_endpoint\": \"" + TEST_USERINFO_ENDPOINT + "\"\n"
            + "}";

    private AuthorizationServiceConfiguration mConfig;
    private RetrievalCallback mCallback;
    @Mock HttpURLConnection mHttpConnection;
    @Mock ConnectionBuilder mConnectionBuilder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mCallback = new RetrievalCallback();
        mConfig = new AuthorizationServiceConfiguration(
                Uri.parse(TEST_AUTH_ENDPOINT),
                Uri.parse(TEST_TOKEN_ENDPOINT),
                Uri.parse(TEST_REGISTRATION_ENDPOINT));
        when(mConnectionBuilder.openConnection(any(Uri.class))).thenReturn(mHttpConnection);
    }

    @Test
    public void testDefaultConstructor() {
        assertMembers(mConfig);
    }

    @Test
    public void testSerialization() throws Exception {
        AuthorizationServiceConfiguration config = AuthorizationServiceConfiguration.fromJson(
                mConfig.toJson());
        assertMembers(config);
    }

    @Test
    public void testSerializationWithoutRegistrationEndpoint() throws Exception {
        AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(
                Uri.parse(TEST_AUTH_ENDPOINT),
                Uri.parse(TEST_TOKEN_ENDPOINT),
                null);
        AuthorizationServiceConfiguration deserialized = AuthorizationServiceConfiguration
                .fromJson(config.toJson());
        assertThat(deserialized.authorizationEndpoint).isEqualTo(config.authorizationEndpoint);
        assertThat(deserialized.tokenEndpoint).isEqualTo(config.tokenEndpoint);
        assertThat(deserialized.registrationEndpoint).isNull();
    }

    @Test
    public void testDiscoveryConstructorWithName() throws Exception {
        JSONObject json = new JSONObject(TEST_JSON);
        AuthorizationServiceDiscovery discovery = new AuthorizationServiceDiscovery(json);
        AuthorizationServiceConfiguration config
                = new AuthorizationServiceConfiguration(discovery);
        assertMembers(config);
    }

    @Test
    public void testDiscoveryConstructorWithoutName() throws Exception {
        JSONObject json = new JSONObject(TEST_JSON);
        AuthorizationServiceDiscovery discovery = new AuthorizationServiceDiscovery(json);
        AuthorizationServiceConfiguration config =
                new AuthorizationServiceConfiguration(discovery);
        assertMembers(config);
    }

    private void assertMembers(AuthorizationServiceConfiguration config) {
        assertEquals(TEST_AUTH_ENDPOINT, config.authorizationEndpoint.toString());
        assertEquals(TEST_TOKEN_ENDPOINT, config.tokenEndpoint.toString());
        assertEquals(TEST_REGISTRATION_ENDPOINT, config.registrationEndpoint.toString());
    }

    @Test
    public void testBuildConfigurationUriFromIssuer() {
        Uri issuerUri = Uri.parse("https://test.openid.com");
        assertThat(AuthorizationServiceConfiguration.buildConfigurationUriFromIssuer(issuerUri))
                .isEqualTo(TEST_DISCOVERY_URI);
    }

    @Test
    public void testBuildConfigurationUriFromIssuer_withRootPath() {
        Uri issuerUri = Uri.parse("https://test.openid.com/");
        assertThat(AuthorizationServiceConfiguration.buildConfigurationUriFromIssuer(issuerUri))
                .isEqualTo(TEST_DISCOVERY_URI);
    }

    @Test
    public void testBuildConfigurationUriFromIssuer_withExtendedPath() {
        Uri issuerUri = Uri.parse("https://test.openid.com/tenant1");
        assertThat(AuthorizationServiceConfiguration.buildConfigurationUriFromIssuer(issuerUri))
                .isEqualTo(Uri.parse(
                        "https://test.openid.com/tenant1/.well-known/openid-configuration"));
    }

    @Test
    public void testFetchFromUrl_success() throws Exception {
        InputStream is = new ByteArrayInputStream(TEST_JSON.getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);
        doFetch();
        mCallback.waitForCallback();
        AuthorizationServiceConfiguration result = mCallback.config;
        assertNotNull(result);
        assertEquals(TEST_AUTH_ENDPOINT, result.authorizationEndpoint.toString());
        assertEquals(TEST_TOKEN_ENDPOINT, result.tokenEndpoint.toString());
        verify(mHttpConnection).connect();
    }

    @Test
    public void testFetchFromUrlWithoutName() throws Exception {
        InputStream is = new ByteArrayInputStream(TEST_JSON.getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);
        doFetch();
        mCallback.waitForCallback();
        AuthorizationServiceConfiguration result = mCallback.config;
        assertNotNull(result);
        assertEquals(TEST_AUTH_ENDPOINT, result.authorizationEndpoint.toString());
        assertEquals(TEST_TOKEN_ENDPOINT, result.tokenEndpoint.toString());
        verify(mHttpConnection).connect();
    }

    @Test
    public void testServiceConfigurationRequest_withBadRequest() throws Exception {
        InputStream is = new ByteArrayInputStream(AuthorizationServiceTest.BAD_REQUEST_RESPONSE.getBytes());
        when(mHttpConnection.getErrorStream()).thenReturn(is);
        when(mHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        when(mHttpConnection.getResponseMessage()).thenReturn(AuthorizationServiceTest.BAD_REQUEST_ERROR_MESSAGE);
        doFetch();
        mCallback.waitForCallback();
        assertBadRequest(mCallback.error);
    }

    private void assertBadRequest(AuthorizationException error) {
        assertNotNull(error);
        assertEquals(AuthorizationException.TYPE_HTTP_ERROR, error.type);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, error.code);
        assertEquals(AuthorizationServiceTest.BAD_REQUEST_ERROR_MESSAGE, error.error);
        assertEquals(AuthorizationServiceTest.BAD_REQUEST_RESPONSE, error.errorDescription);
    }

    @Test
    public void testFetchFromUrl_missingArgument() throws Exception {
        InputStream is = new ByteArrayInputStream(TEST_JSON_MISSING_ARGUMENT.getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);
        doFetch();
        mCallback.waitForCallback();
        assertNotNull(mCallback.error);
        assertEquals(GeneralErrors.INVALID_DISCOVERY_DOCUMENT, mCallback.error);
    }

    @Test
    public void testFetchFromUrl_malformedJson() throws Exception {
        InputStream is = new ByteArrayInputStream(TEST_JSON_MALFORMED.getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);
        doFetch();
        mCallback.waitForCallback();
        assertNotNull(mCallback.error);
        assertEquals(GeneralErrors.JSON_DESERIALIZATION_ERROR, mCallback.error);
    }

    @Test
    public void testFetchFromUrl_IoException() throws Exception {
        IOException ex = new IOException();
        when(mHttpConnection.getInputStream()).thenThrow(ex);
        doFetch();
        mCallback.waitForCallback();
        assertNotNull(mCallback.error);
        assertEquals(GeneralErrors.NETWORK_ERROR, mCallback.error);
    }

    private void doFetch() {
        AuthorizationServiceConfiguration.fetchFromUrl(
                TEST_DISCOVERY_URI,
                mCallback,
                mConnectionBuilder);
    }

    private static class RetrievalCallback implements
            AuthorizationServiceConfiguration.RetrieveConfigurationCallback {
        private Semaphore mSemaphore = new Semaphore(0);
        public AuthorizationServiceConfiguration config;
        public AuthorizationException error;

        public void waitForCallback() throws Exception {
            assertTrue(mSemaphore.tryAcquire(CALLBACK_TIMEOUT_MILLIS,
                    TimeUnit.MILLISECONDS));
        }

        @Override
        public void onFetchConfigurationCompleted(
                @Nullable AuthorizationServiceConfiguration serviceConfiguration,
                @Nullable AuthorizationException ex) {
            assertTrue((serviceConfiguration == null) ^ (ex == null));
            this.config = serviceConfiguration;
            this.error = ex;
            mSemaphore.release();
        }
    }

    private static String toJson(List<String> strings) {
        return new JSONArray(strings).toString();
    }
}
