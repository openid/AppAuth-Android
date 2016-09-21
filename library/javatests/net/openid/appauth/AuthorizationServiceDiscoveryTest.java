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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=16)
public class AuthorizationServiceDiscoveryTest {
    // ToDo: add more tests for remaining getters
    private static final String TEST_ISSUER = "test_issuer";
    private static final String TEST_AUTHORIZATION_ENDPOINT = "http://test.openid.com/o/oauth/auth";
    private static final String TEST_TOKEN_ENDPOINT = "http://test.openid.com/o/oauth/token";
    private static final String TEST_USERINFO_ENDPOINT = "http://test.openid.com/o/oauth/userinfo";
    private static final String TEST_JWKS_URI = "http://test.openid.com/o/oauth/jwks";
    private static final List<String> TEST_RESPONSE_TYPE_SUPPORTED = Arrays.asList("code", "token");
    private static final List<String> TEST_SUBJECT_TYPES_SUPPORTED = Arrays.asList("public");
    private static final List<String> TEST_ID_TOKEN_SIGNING_ALG_VALUES = Arrays.asList("RS256");
    private static final List<String> TEST_SCOPES_SUPPORTED = Arrays.asList("openid", "profile");
    private static final List<String> TEST_TOKEN_ENDPOINT_AUTH_METHODS
            = Arrays.asList("client_secret_post", "client_secret_basic");
    private static final List<String> TEST_CLAIMS_SUPPORTED = Arrays.asList("aud", "exp");

    private static final String TEST_JSON = "{\n"
            + " \"issuer\": \"" + TEST_ISSUER + "\",\n"
            + " \"authorization_endpoint\": \"" + TEST_AUTHORIZATION_ENDPOINT + "\",\n"
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
            + " \"claims_supported\": " + toJson(TEST_CLAIMS_SUPPORTED) + "\n"
            + "}";

    JSONObject mJson;
    AuthorizationServiceDiscovery mDiscovery;

    @Before
    public void setUp() throws Exception {
        mJson = new JSONObject(TEST_JSON);
        mDiscovery = new AuthorizationServiceDiscovery(mJson);
    }

    @Test
    public void testMissingAuthorizationEndpoint() throws Exception {
        mJson.remove(AuthorizationServiceDiscovery.AUTHORIZATION_ENDPOINT.key);
        try {
            new AuthorizationServiceDiscovery(mJson);
            fail("Expected MissingArgumentException not thrown.");
        } catch (AuthorizationServiceDiscovery.MissingArgumentException e) {
            assertEquals(AuthorizationServiceDiscovery.AUTHORIZATION_ENDPOINT.key,
                    e.getMissingField());
        }
    }

    @Test
    public void testMissingIssuer() throws Exception {
        mJson.remove(AuthorizationServiceDiscovery.ISSUER.key);
        try {
            new AuthorizationServiceDiscovery(mJson);
            fail("Expected MissingArgumentException not thrown.");
        } catch (AuthorizationServiceDiscovery.MissingArgumentException e) {
            assertEquals(AuthorizationServiceDiscovery.ISSUER.key,
                    e.getMissingField());
        }
    }

    @Test
    public void testMissingJwksUri() throws Exception {
        mJson.remove(AuthorizationServiceDiscovery.JWKS_URI.key);
        try {
            new AuthorizationServiceDiscovery(mJson);
            fail("Expected MissingArgumentException not thrown.");
        } catch (AuthorizationServiceDiscovery.MissingArgumentException e) {
            assertEquals(AuthorizationServiceDiscovery.JWKS_URI.key,
                    e.getMissingField());
        }
    }

    @Test
    public void testMissingSubjectTypesSupported() throws Exception {
        mJson.remove(AuthorizationServiceDiscovery.SUBJECT_TYPES_SUPPORTED.key);
        try {
            new AuthorizationServiceDiscovery(mJson);
            fail("Expected MissingArgumentException not thrown.");
        } catch (AuthorizationServiceDiscovery.MissingArgumentException e) {
            assertEquals(AuthorizationServiceDiscovery.SUBJECT_TYPES_SUPPORTED.key,
                    e.getMissingField());
        }
    }

    @Test
    public void testMissingResponseTypesSupported() throws Exception {
        mJson.remove(AuthorizationServiceDiscovery.RESPONSE_TYPES_SUPPORTED.key);
        try {
            new AuthorizationServiceDiscovery(mJson);
            fail("Expected MissingArgumentException not thrown.");
        } catch (AuthorizationServiceDiscovery.MissingArgumentException e) {
            assertEquals(AuthorizationServiceDiscovery.RESPONSE_TYPES_SUPPORTED.key,
                    e.getMissingField());
        }
    }

    @Test
    public void testMissingIdTokenSigningAlgValuesSupported() throws Exception {
        mJson.remove(AuthorizationServiceDiscovery.ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED.key);
        try {
            new AuthorizationServiceDiscovery(mJson);
            fail("Expected MissingArgumentException not thrown.");
        } catch (AuthorizationServiceDiscovery.MissingArgumentException e) {
            assertEquals(AuthorizationServiceDiscovery.ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED.key,
                    e.getMissingField());
        }
    }

    @Test
    public void testDefaultValueClaimsParametersSupported() throws Exception {
        mJson.remove(AuthorizationServiceDiscovery.CLAIMS_PARAMETER_SUPPORTED.key);
        assertFalse(new AuthorizationServiceDiscovery(mJson).isClaimsParameterSupported());
    }

    @Test
    public void testDefaultValueRequestParameterSupported() throws Exception {
        mJson.remove(AuthorizationServiceDiscovery.REQUEST_PARAMETER_SUPPORTED.key);
        assertFalse(new AuthorizationServiceDiscovery(mJson).isRequestParameterSupported());
    }

    @Test
    public void testDefaultValueRequestUriParameterSupported() throws Exception {
        mJson.remove(AuthorizationServiceDiscovery.REQUEST_URI_PARAMETER_SUPPORTED.key);
        assertTrue(new AuthorizationServiceDiscovery(mJson).isRequestUriParameterSupported());
    }

    @Test
    public void testDefaultValueRequireRequestUriRegistration() throws Exception {
        mJson.remove(AuthorizationServiceDiscovery.REQUIRE_REQUEST_URI_REGISTRATION.key);
        assertFalse(new AuthorizationServiceDiscovery(mJson).requireRequestUriRegistration());
    }

    @Test
    public void testGetIssuer() {
        assertEquals(TEST_ISSUER, mDiscovery.getIssuer());
    }

    @Test
    public void testGetAuthorizationEndpoint() {
        assertEquals(TEST_AUTHORIZATION_ENDPOINT, mDiscovery.getAuthorizationEndpoint().toString());
    }

    @Test
    public void testGetTokenEndpoint() {
        assertEquals(TEST_TOKEN_ENDPOINT, mDiscovery.getTokenEndpoint().toString());
    }

    @Test
    public void testGetUserinfoEndpoint() {
        assertEquals(TEST_USERINFO_ENDPOINT, mDiscovery.getUserinfoEndpoint().toString());
    }

    @Test
    public void testGetJwksUri() {
        assertEquals(TEST_JWKS_URI, mDiscovery.getJwksUri().toString());
    }

    @Test
    public void testGetResponseTypeSupported() {
        assertEquals(TEST_RESPONSE_TYPE_SUPPORTED, mDiscovery.getResponseTypesSupported());
    }

    @Test
    public void testGetSubjectTypesSupported() {
        assertEquals(TEST_SUBJECT_TYPES_SUPPORTED, mDiscovery.getSubjectTypesSupported());
    }

    @Test
    public void testGetIdTokenSigningAlgorithmValuesSupported() {
        assertEquals(TEST_ID_TOKEN_SIGNING_ALG_VALUES,
                mDiscovery.getIdTokenSigningAlgorithmValuesSupported());
    }

    @Test
    public void testGetScopesSupported() {
        assertEquals(TEST_SCOPES_SUPPORTED, mDiscovery.getScopesSupported());
    }

    @Test
    public void testGetTokenEndpointAuthMethodsSupported() {
        assertEquals(TEST_TOKEN_ENDPOINT_AUTH_METHODS,
                mDiscovery.getTokenEndpointAuthMethodsSupported());
    }

    @Test
    public void testGetClaimsSupported() {
        assertEquals(TEST_CLAIMS_SUPPORTED, mDiscovery.getClaimsSupported());
    }

    private static String toJson(List<String> strings) {
        return new JSONArray(strings).toString();
    }
}
