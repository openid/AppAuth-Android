/*
 * Copyright 2016 The AppAuth for Android Authors. All Rights Reserved.
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    private AuthorizationRequest.Builder mMinimalRequestBuilder;

    @Before
    public void setUp() {

        mMinimalRequestBuilder = new AuthorizationRequest.Builder(
                getTestServiceConfig(),
                TEST_CLIENT_ID,
                ResponseTypeValues.CODE,
                TEST_APP_REDIRECT_URI);

        mRequestBuilder = new AuthorizationRequest.Builder(
                getTestServiceConfig(),
                TEST_CLIENT_ID,
                ResponseTypeValues.CODE,
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
                ResponseTypeValues.CODE,
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

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_setAdditionalParams_withBuiltInParam() {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(AuthorizationRequest.PARAM_SCOPE, AuthorizationRequest.SCOPE_EMAIL);
        mRequestBuilder.setAdditionalParameters(additionalParams);
    }

    @Test
    public void testDisplay() {
        AuthorizationRequest req = mMinimalRequestBuilder
                .setDisplay(AuthorizationRequest.Display.TOUCH)
                .build();

        assertThat(req.display).isEqualTo(AuthorizationRequest.Display.TOUCH);
    }

    @Test
    public void testDisplay_isNullByDefault() {
        AuthorizationRequest req = mMinimalRequestBuilder.build();
        assertThat(req.display).isNull();
    }

    @Test
     public void testDisplay_withNullValue() {
        AuthorizationRequest req = mMinimalRequestBuilder
                .setDisplay(null)
                .build();

        assertThat(req.display).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDisplay_withEmptyValue() {
        mMinimalRequestBuilder.setDisplay("").build();
    }

    @Test
    public void testPrompt() {
        AuthorizationRequest req = mMinimalRequestBuilder
                .setPrompt(AuthorizationRequest.Prompt.LOGIN)
                .build();

        assertThat(req.prompt).isEqualTo(AuthorizationRequest.Prompt.LOGIN);
    }

    @Test
    public void testPrompt_isNullByDefault() {
        AuthorizationRequest req = mMinimalRequestBuilder.build();
        assertThat(req.prompt).isNull();
    }

    @Test
    public void testPrompt_withNullValue() {
        AuthorizationRequest req = mMinimalRequestBuilder
                .setPrompt(null)
                .build();

        assertThat(req.prompt).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrompt_withEmptyString() {
        mMinimalRequestBuilder.setPrompt("");
    }

    @Test
    public void testPrompt_withVarargs() {
        AuthorizationRequest req = mMinimalRequestBuilder
                .setPromptValues(
                        AuthorizationRequest.Prompt.LOGIN,
                        AuthorizationRequest.Prompt.CONSENT)
                .build();

        assertThat(req.prompt).isEqualTo(
                AuthorizationRequest.Prompt.LOGIN + " " + AuthorizationRequest.Prompt.CONSENT);
    }

    @Test
    public void testPrompt_withNullVarargsArray() {
        AuthorizationRequest req = mMinimalRequestBuilder
                .setPromptValues((String[])null)
                .build();

        assertThat(req.prompt).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrompt_withNullStringInVarargs() {
        mMinimalRequestBuilder.setPromptValues(AuthorizationRequest.Prompt.LOGIN, null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrompt_withEmptyStringInVarargs() {
        mMinimalRequestBuilder.setPromptValues(AuthorizationRequest.Prompt.LOGIN, "").build();
    }

    @Test
    public void testPrompt_withIterable() {
        ArrayList<String> promptValues = new ArrayList<>();
        promptValues.add(AuthorizationRequest.Prompt.SELECT_ACCOUNT);
        promptValues.add(AuthorizationRequest.Prompt.CONSENT);
        AuthorizationRequest req = mMinimalRequestBuilder
                .setPromptValues(promptValues)
                .build();

        assertThat(req.prompt).isEqualTo(
                AuthorizationRequest.Prompt.SELECT_ACCOUNT + " "
                        + AuthorizationRequest.Prompt.CONSENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrompt_withIterableContainingNullValue() {
        ArrayList<String> promptValues = new ArrayList<>();
        promptValues.add(AuthorizationRequest.Prompt.SELECT_ACCOUNT);
        promptValues.add(null);
        mMinimalRequestBuilder.setPromptValues(promptValues).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrompt_withIterableContainingEmptyValue() {
        ArrayList<String> promptValues = new ArrayList<>();
        promptValues.add(AuthorizationRequest.Prompt.SELECT_ACCOUNT);
        promptValues.add("");
        mMinimalRequestBuilder.setPromptValues(promptValues).build();
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
    public void testToUri() throws Exception {
        Uri uri = mRequest.toUri();

        Uri authEndpoint = mRequest.configuration.authorizationEndpoint;
        assertThat(uri.getScheme()).isEqualTo(authEndpoint.getScheme());
        assertThat(uri.getAuthority()).isEqualTo(authEndpoint.getAuthority());
        assertThat(uri.getPath()).isEqualTo(authEndpoint.getPath());
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_REDIRECT_URI))
                .isEqualTo(mRequest.redirectUri.toString());
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_CLIENT_ID))
                .isEqualTo(mRequest.clientId);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_RESPONSE_TYPE))
                .isEqualTo(mRequest.responseType);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_STATE))
                .isEqualTo(mRequest.state);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_SCOPE))
                .isEqualTo(mRequest.scope);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_RESPONSE_MODE))
                .isEqualTo(mRequest.responseMode);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_CODE_CHALLENGE))
                .isEqualTo(mRequest.codeVerifierChallenge);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_CODE_CHALLENGE_METHOD))
                .isEqualTo(mRequest.codeVerifierChallengeMethod);
    }

    @Test
    public void testToUri_withMinimalConfiguration() throws Exception {
        AuthorizationRequest req = mMinimalRequestBuilder.build();

        Uri uri = req.toUri();
        assertThat(uri.getQueryParameterNames())
                .isEqualTo(new HashSet<>(Arrays.asList(
                        AuthorizationRequest.PARAM_CLIENT_ID,
                        AuthorizationRequest.PARAM_RESPONSE_TYPE,
                        AuthorizationRequest.PARAM_REDIRECT_URI,
                        AuthorizationRequest.PARAM_STATE,
                        AuthorizationRequest.PARAM_CODE_CHALLENGE,
                        AuthorizationRequest.PARAM_CODE_CHALLENGE_METHOD)));
    }

    @Test
    public void testToUri_withNoState() throws Exception {
        AuthorizationRequest req = mMinimalRequestBuilder.setState(null).build();
        assertThat(req.toUri().getQueryParameterNames())
                .doesNotContain(AuthorizationRequest.PARAM_STATE);
    }

    @Test
    public void testToUri_withNoVerifier() throws Exception {
        AuthorizationRequest req = mMinimalRequestBuilder.setCodeVerifier(null).build();
        assertThat(req.toUri().getQueryParameterNames())
                .doesNotContain(AuthorizationRequest.PARAM_CODE_CHALLENGE)
                .doesNotContain(AuthorizationRequest.PARAM_CODE_CHALLENGE_METHOD);
    }

    @Test
    public void testToUri_withAdditionalParameters() throws Exception {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put("my_param", "1234");
        additionalParams.put("another_param", "5678");
        AuthorizationRequest req = mMinimalRequestBuilder
                .setAdditionalParameters(additionalParams)
                .build();

        Uri uri = req.toUri();
        assertThat(uri.getQueryParameter("my_param"))
                .isEqualTo("1234");
        assertThat(uri.getQueryParameter("another_param"))
                .isEqualTo("5678");
    }

    @Test
    public void testToUri_withPrompt() throws Exception {
        AuthorizationRequest req = mMinimalRequestBuilder
                .setPrompt(AuthorizationRequest.Prompt.LOGIN)
                .build();

        Uri uri = req.toUri();
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_PROMPT))
                .isEqualTo(AuthorizationRequest.Prompt.LOGIN);
    }

    @Test
    public void testToUri_withDisplay() throws Exception {
        AuthorizationRequest req = mMinimalRequestBuilder
                .setDisplay(AuthorizationRequest.Display.TOUCH)
                .build();

        Uri uri = req.toUri();
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_DISPLAY))
                .isEqualTo(AuthorizationRequest.Display.TOUCH);
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
                ResponseTypeValues.CODE,
                request.responseType);
        assertEquals("unexpected response mode",
                AuthorizationRequest.RESPONSE_MODE_QUERY,
                request.responseMode);
        assertEquals("unexpected additional params",
                TEST_ADDITIONAL_PARAMS,
                request.additionalParameters);
    }
}
