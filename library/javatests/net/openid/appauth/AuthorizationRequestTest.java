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

import static net.openid.appauth.TestValues.TEST_APP_REDIRECT_URI;
import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.TEST_STATE;
import static net.openid.appauth.TestValues.getTestServiceConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AuthorizationRequestTest {

    /**
     * Contains all legal characters for a code verifier.
     * @see <a href="https://tools.ietf.org/html/rfc7636#section-4.1">RFC 7636, Section 4.1</a>
     */
    private static final String TEST_CODE_VERIFIER =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_~.";

    private static final String TEST_CODE_VERIFIER_CHALLENGE_METHOD =
            "plain";

    private static final Map<String, String> TEST_ADDITIONAL_PARAMS;

    static {
        TEST_ADDITIONAL_PARAMS = new HashMap<>();
        TEST_ADDITIONAL_PARAMS.put("test_key1", "test_value1");
        TEST_ADDITIONAL_PARAMS.put("test_key2", "test_value2");
    }

    private AuthorizationRequest.Builder mRequestBuilder;
    private AuthorizationRequest mRequest;

    @Before
    public void setUp() {
        mRequestBuilder = new AuthorizationRequest.Builder(
                getTestServiceConfig(),
                TEST_CLIENT_ID,
                AuthorizationRequest.RESPONSE_TYPE_CODE,
                TEST_APP_REDIRECT_URI)
                .setState(TEST_STATE)
                .setScopes(AuthorizationRequest.SCOPE_OPENID, AuthorizationRequest.SCOPE_EMAIL)
                .setCodeVerifier(
                        TEST_CODE_VERIFIER,
                        TEST_CODE_VERIFIER,
                        TEST_CODE_VERIFIER_CHALLENGE_METHOD)
                .setResponseMode(AuthorizationRequest.RESPONSE_MODE_QUERY)
                .setAdditionalParameters(TEST_ADDITIONAL_PARAMS);
        mRequest = mRequestBuilder.build();
    }

    @Test
    public void testBuilder() {
        assertValues(mRequest);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_codeVerifierTooShort() {
        // code verifier is one character too short
        char[] codeVerifier = new char[CodeVerifierUtil.MIN_CODE_VERIFIER_LENGTH - 1];
        for (int i = 0; i < codeVerifier.length; i++) {
            codeVerifier[i] = '0';
        }
        AuthorizationRequest request = new AuthorizationRequest.Builder(
                getTestServiceConfig(),
                TEST_CLIENT_ID,
                AuthorizationRequest.RESPONSE_TYPE_CODE,
                TEST_APP_REDIRECT_URI)
                .setCodeVerifier(new String(codeVerifier))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_codeVerifierTooLong() {
        // code verifier is one character too long
        char[] codeVerifier = new char[CodeVerifierUtil.MAX_CODE_VERIFIER_LENGTH + 1];
        for (int i = 0; i < codeVerifier.length; i++) {
            codeVerifier[i] = '0';
        }
        AuthorizationRequest request = mRequestBuilder.setCodeVerifier(new String(codeVerifier))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_codeVerifierWithIllegalChars() {
        AuthorizationRequest request = mRequestBuilder
                .setCodeVerifier("##ILLEGAL!$!")
                .build();
    }

    @Test
    public void testBuilder_disableCodeVerifier() {
        AuthorizationRequest request = mRequestBuilder
                .setCodeVerifier(null)
                .build();
        assertNull(request.codeVerifier);
        assertNull(request.codeVerifierChallenge);
        assertNull(request.codeVerifierChallengeMethod);
    }

    @Test
    public void testBuilder_customCodeVerifier() {
        AuthorizationRequest request = mRequestBuilder
                .setCodeVerifier(TEST_CODE_VERIFIER, "myChallenge", "myChallengeMethod")
                .build();
        assertEquals(TEST_CODE_VERIFIER, request.codeVerifier);
        assertEquals("myChallenge", request.codeVerifierChallenge);
        assertEquals("myChallengeMethod", request.codeVerifierChallengeMethod);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_codeVerifierWithoutCodeChallenge() {
        mRequestBuilder
                .setCodeVerifier(
                        TEST_CODE_VERIFIER,
                        null,
                        CodeVerifierUtil.getCodeVerifierChallengeMethod())
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_codeVerifierWithoutCodeChallengeMethod() {
        String codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier();
        mRequestBuilder
                .setCodeVerifier(
                        TEST_CODE_VERIFIER,
                        CodeVerifierUtil.deriveCodeVerifierChallenge(TEST_CODE_VERIFIER),
                        null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_emptyResponseMode() {
        mRequestBuilder.setResponseMode("");
    }

    @Test
    public void testScopes_null() {
        AuthorizationRequest request = mRequestBuilder
                .setScopes((Iterable<String>)null)
                .build();
        assertNull(request.scope);
    }

    @Test
    public void testScopes_empty() {
        AuthorizationRequest request = mRequestBuilder
                .setScopes()
                .build();
        assertNull(request.scope);
    }

    @Test
    public void testScopes_emptyList() {
        AuthorizationRequest request = mRequestBuilder
                .setScopes(Collections.<String>emptyList())
                .build();
        assertNull(request.scope);
    }

    @Test
    public void testSerialization() throws Exception {
        AuthorizationRequest request = AuthorizationRequest.fromJson(mRequest.toJson());
        assertValues(request);
    }

    @Test
    public void testSerialization_scopesNull() throws Exception {
        AuthorizationRequest request = mRequestBuilder
                .setScopes((Iterable<String>)null)
                .build();
        AuthorizationRequest newRequest = AuthorizationRequest.fromJson(request.toJson());
        assertNull(newRequest.scope);
    }

    @Test
    public void testSerialization_scopesEmpty() throws Exception {
        AuthorizationRequest request = mRequestBuilder
                .setScopes(Collections.<String>emptyList())
                .build();
        AuthorizationRequest newRequest = AuthorizationRequest.fromJson(request.toJson());
        assertNull(newRequest.scope);
    }

    private void assertValues(AuthorizationRequest request) {
        assertEquals("unexpected client ID", TEST_CLIENT_ID, request.clientId);
        assertEquals("unexpected redirect URI", TEST_APP_REDIRECT_URI, request.redirectUri);
        assertEquals("unexpected scope string", "openid email", request.scope);
        assertEquals("unexpected state", TEST_STATE, request.state);
        assertEquals("unexpected code verifier", TEST_CODE_VERIFIER, request.codeVerifier);
        assertEquals("unexpected code verifier challenge",
                TEST_CODE_VERIFIER, request.codeVerifierChallenge);
        assertEquals("unexpected code verifier challenge method",
                TEST_CODE_VERIFIER_CHALLENGE_METHOD, request.codeVerifierChallengeMethod);
        assertEquals("unexpected response type",
                AuthorizationRequest.RESPONSE_TYPE_CODE,
                request.responseType);
        assertEquals("unexpected response mode",
                AuthorizationRequest.RESPONSE_MODE_QUERY,
                request.responseMode);
        assertEquals("unexpected additional params",
                TEST_ADDITIONAL_PARAMS,
                request.additionalParameters);
    }
}
