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

import static net.openid.appauth.TestValues.TEST_APP_REDIRECT_URI;
import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.TEST_EMAIL_ADDRESS;
import static net.openid.appauth.TestValues.TEST_NONCE;
import static net.openid.appauth.TestValues.TEST_STATE;
import static net.openid.appauth.TestValues.getTestServiceConfig;
import static org.assertj.core.api.Assertions.assertThat;

import android.net.Uri;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class AuthorizationRequestTest {

    /**
     * Contains all legal characters for a code verifier.
     * @see <a href="https://tools.ietf.org/html/rfc7636#section-4.1">RFC 7636, Section 4.1</a>
     */
    private static final String TEST_CODE_VERIFIER =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_~.";

    private static final Map<String, String> TEST_ADDITIONAL_PARAMS;

    static {
        TEST_ADDITIONAL_PARAMS = new HashMap<>();
        TEST_ADDITIONAL_PARAMS.put("test_key1", "test_value1");
        TEST_ADDITIONAL_PARAMS.put("test_key2", "test_value2");
    }

    private static final String TEST_CLAIMS = "{\n"
        + " \"userinfo\":{\"email\":{\"essential\":true}},\n"
        + " \"id_token\":{\"gender\":null}\n"
        + "}";

    private AuthorizationRequest.Builder mRequestBuilder;

    @Before
    public void setUp() {
        mRequestBuilder = new AuthorizationRequest.Builder(
                getTestServiceConfig(),
                TEST_CLIENT_ID,
                ResponseTypeValues.CODE,
                TEST_APP_REDIRECT_URI);
    }

    /* ********************************** Builder() ***********************************************/

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void testBuilder_nullConfiguration() {
        new AuthorizationRequest.Builder(
                null,
                TEST_CLIENT_ID,
                ResponseTypeValues.CODE,
                TEST_APP_REDIRECT_URI);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void testBuilder_nullClientId() {
        new AuthorizationRequest.Builder(
                getTestServiceConfig(),
                null,
                ResponseTypeValues.CODE,
                TEST_APP_REDIRECT_URI);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_emptyClientId() {
        new AuthorizationRequest.Builder(
                getTestServiceConfig(),
                "",
                ResponseTypeValues.CODE,
                TEST_APP_REDIRECT_URI);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void testBuilder_nullResponseType() {
        new AuthorizationRequest.Builder(
                getTestServiceConfig(),
                TEST_CLIENT_ID,
                null,
                TEST_APP_REDIRECT_URI);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_emptyResponseType() {
        new AuthorizationRequest.Builder(
                getTestServiceConfig(),
                TEST_CLIENT_ID,
                "",
                TEST_APP_REDIRECT_URI);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void testBuilder_nullRedirectUri() {
        new AuthorizationRequest.Builder(
                getTestServiceConfig(),
                TEST_CLIENT_ID,
                ResponseTypeValues.CODE,
                null);
    }

    /* ************************************** clientId ********************************************/

    @Test
    public void testClientId_fromConstructor() {
        AuthorizationRequest request = mRequestBuilder.build();
        assertThat(request.clientId).isEqualTo(TEST_CLIENT_ID);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void testClientId_null() {
        mRequestBuilder.setClientId(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testClientId_empty() {
        mRequestBuilder.setClientId("").build();
    }

    /* ************************************** codeVerifier ****************************************/

    @Test
    public void testCodeVerifier_autoGenerated() {
        AuthorizationRequest request = mRequestBuilder.build();
        assertThat(request.codeVerifier).isNotEmpty();
        assertThat(request.codeVerifierChallenge).isNotEmpty();
        assertThat(request.codeVerifierChallengeMethod).isNotEmpty();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCodeVerifier_tooShort() {
        mRequestBuilder
                .setCodeVerifier(generateString(CodeVerifierUtil.MIN_CODE_VERIFIER_LENGTH - 1))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCodeVerifier_tooLong() {
        mRequestBuilder
                .setCodeVerifier(generateString(CodeVerifierUtil.MAX_CODE_VERIFIER_LENGTH + 1))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCodeVerifier_illegalChars() {
        mRequestBuilder.setCodeVerifier("##ILLEGAL!$!").build();
    }

    @Test
    public void testCodeVerifier_disabled() {
        AuthorizationRequest request = mRequestBuilder
                .setCodeVerifier(null)
                .build();
        assertThat(request.codeVerifier).isNull();
        assertThat(request.codeVerifierChallenge).isNull();
        assertThat(request.codeVerifierChallengeMethod).isNull();
    }

    @Test
    public void testCodeVerifier_customized() {
        AuthorizationRequest request = mRequestBuilder
                .setCodeVerifier(TEST_CODE_VERIFIER, "myChallenge", "myChallengeMethod")
                .build();
        assertThat(request.codeVerifier).isEqualTo(TEST_CODE_VERIFIER);
        assertThat(request.codeVerifierChallenge).isEqualTo("myChallenge");
        assertThat(request.codeVerifierChallengeMethod).isEqualTo("myChallengeMethod");
    }

    @Test(expected = NullPointerException.class)
    public void testCodeVerifier_withoutCodeChallenge() {
        mRequestBuilder
                .setCodeVerifier(
                        TEST_CODE_VERIFIER,
                        null,
                        CodeVerifierUtil.getCodeVerifierChallengeMethod())
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testCodeVerifier_withoutCodeChallengeMethod() {
        mRequestBuilder
                .setCodeVerifier(
                        TEST_CODE_VERIFIER,
                        CodeVerifierUtil.deriveCodeVerifierChallenge(TEST_CODE_VERIFIER),
                        null)
                .build();
    }

    /* ************************************** display *********************************************/

    @Test
    public void testDisplay_unspecified() {
        AuthorizationRequest request = mRequestBuilder.build();
        assertThat(request.display).isNull();
    }

    @Test
    public void testDisplay() {
        AuthorizationRequest req = mRequestBuilder
                .setDisplay(AuthorizationRequest.Display.TOUCH)
                .build();
        assertThat(req.display).isEqualTo(AuthorizationRequest.Display.TOUCH);
    }

    @Test
    public void testDisplay_withNullValue() {
        AuthorizationRequest req = mRequestBuilder
                .setDisplay(null)
                .build();

        assertThat(req.display).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDisplay_withEmptyValue() {
        mRequestBuilder.setDisplay("").build();
    }

    /* ***********************************  login_hint ********************************************/

    @Test
    public void testLoginHint_unspecified() {
        AuthorizationRequest request = mRequestBuilder.build();
        assertThat(request.loginHint).isNull();
    }

    @Test
    public void testLoginHint() {
        AuthorizationRequest req = mRequestBuilder
            .setLoginHint(TEST_EMAIL_ADDRESS)
            .build();
        assertThat(req.loginHint).isEqualTo(TEST_EMAIL_ADDRESS);
    }

    @Test
    public void testLoginHint_withNullValue() {
        AuthorizationRequest req = mRequestBuilder
            .setLoginHint(null)
            .build();

        assertThat(req.loginHint).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoginHint_withEmptyValue() {
        mRequestBuilder.setLoginHint("").build();
    }

    /* ************************************** prompt **********************************************/

    @Test
    public void testPrompt_unspecified() {
        AuthorizationRequest request = mRequestBuilder.build();
        assertThat(request.prompt).isNull();
        assertThat(request.getPromptValues()).isNull();
    }

    @Test
    public void testPrompt() {
        AuthorizationRequest req = mRequestBuilder
                .setPrompt(AuthorizationRequest.Prompt.LOGIN)
                .build();

        assertThat(req.prompt).isEqualTo(AuthorizationRequest.Prompt.LOGIN);
        assertThat(req.getPromptValues())
                .hasSize(1)
                .contains(AuthorizationRequest.Prompt.LOGIN);
    }

    @Test
    public void testPrompt_nullValue() {
        AuthorizationRequest req = mRequestBuilder.setPrompt(null).build();
        assertThat(req.prompt).isNull();
        assertThat(req.getPromptValues()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrompt_empty() {
        mRequestBuilder.setPrompt("").build();
    }

    @Test
    public void testPrompt_withVarargs() {
        AuthorizationRequest req = mRequestBuilder
                .setPromptValues(
                        AuthorizationRequest.Prompt.LOGIN,
                        AuthorizationRequest.Prompt.CONSENT)
                .build();

        assertThat(req.prompt).isEqualTo(
                AuthorizationRequest.Prompt.LOGIN + " " + AuthorizationRequest.Prompt.CONSENT);
        assertThat(req.getPromptValues())
                .hasSize(2)
                .contains(AuthorizationRequest.Prompt.LOGIN)
                .contains(AuthorizationRequest.Prompt.CONSENT);
    }

    @Test
    public void testPrompt_withNullVarargsArray() {
        AuthorizationRequest req = mRequestBuilder.setPromptValues((String[])null).build();
        assertThat(req.prompt).isNull();
        assertThat(req.getPromptValues()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrompt_withNullStringInVarargs() {
        mRequestBuilder.setPromptValues(AuthorizationRequest.Prompt.LOGIN, null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrompt_withEmptyStringInVarargs() {
        mRequestBuilder.setPromptValues(AuthorizationRequest.Prompt.LOGIN, "").build();
    }

    @Test
    public void testPrompt_withIterable() {
        AuthorizationRequest req = mRequestBuilder
                .setPromptValues(Arrays.asList(
                        AuthorizationRequest.Prompt.SELECT_ACCOUNT,
                        AuthorizationRequest.Prompt.CONSENT))
                .build();

        assertThat(req.prompt).isEqualTo(
                AuthorizationRequest.Prompt.SELECT_ACCOUNT
                        + " "
                        + AuthorizationRequest.Prompt.CONSENT);

        assertThat(req.getPromptValues())
                .hasSize(2)
                .contains(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
                .contains(AuthorizationRequest.Prompt.CONSENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrompt_withIterableContainingNullValue() {
        mRequestBuilder
                .setPromptValues(Arrays.asList(
                        AuthorizationRequest.Prompt.SELECT_ACCOUNT,
                        null))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrompt_withIterableContainingEmptyValue() {
        mRequestBuilder
                .setPromptValues(Arrays.asList(
                        AuthorizationRequest.Prompt.SELECT_ACCOUNT,
                        ""))
                .build();
    }

    /* ******************************** ui_locales ***********************************************/

    @Test
    public void testUiLocales_unspecified() {
        AuthorizationRequest request = mRequestBuilder.build();
        assertThat(request.uiLocales).isNull();
        assertThat(request.getUiLocales()).isNull();
    }

    @Test
    public void testUiLocales() {
        AuthorizationRequest req = mRequestBuilder
            .setUiLocales("en de fr-CA")
            .build();

        assertThat(req.uiLocales).isEqualTo("en de fr-CA");
        assertThat(req.getUiLocales())
            .hasSize(3)
            .contains("en")
            .contains("de")
            .contains("fr-CA");
    }

    @Test
    public void testUiLocales_nullValue() {
        AuthorizationRequest req = mRequestBuilder.setUiLocales(null).build();
        assertThat(req.uiLocales).isNull();
        assertThat(req.getUiLocales()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUiLocales_empty() {
        mRequestBuilder.setUiLocales("").build();
    }

    @Test
    public void testUiLocales_withVarargs() {
        AuthorizationRequest req = mRequestBuilder
            .setUiLocalesValues("en", "de", "fr-CA")
            .build();

        assertThat(req.uiLocales).isEqualTo("en de fr-CA");
        assertThat(req.getUiLocales())
            .hasSize(3)
            .contains("en")
            .contains("de")
            .contains("fr-CA");
    }

    @Test
    public void testUiLocales_withNullVarargsArray() {
        AuthorizationRequest req = mRequestBuilder.setUiLocalesValues((String[])null).build();
        assertThat(req.uiLocales).isNull();
        assertThat(req.getUiLocales()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUiLocales_withNullStringInVarargs() {
        mRequestBuilder.setUiLocalesValues("en", null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUiLocales_withEmptyStringInVarargs() {
        mRequestBuilder.setUiLocalesValues("en", "").build();
    }

    @Test
    public void testUiLocales_withIterable() {
        AuthorizationRequest req = mRequestBuilder
            .setUiLocalesValues(Arrays.asList("en", "de", "fr-CA"))
            .build();

        assertThat(req.uiLocales).isEqualTo("en de fr-CA");

        assertThat(req.getUiLocales())
            .hasSize(3)
            .contains("en")
            .contains("de")
            .contains("fr-CA");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUiLocales_withIterableContainingNullValue() {
        mRequestBuilder
            .setUiLocalesValues(Arrays.asList("en", null))
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUiLocales_withIterableContainingEmptyValue() {
        mRequestBuilder
            .setUiLocalesValues(Arrays.asList("en", ""))
            .build();
    }

    /* ******************************** redirectUri ***********************************************/

    @Test
    public void testRedirectUri_fromConstructor() {
        AuthorizationRequest request = mRequestBuilder.build();
        assertThat(request.redirectUri).isEqualTo(TEST_APP_REDIRECT_URI);
    }

    /* ******************************* responseMode ***********************************************/

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_emptyResponseMode() {
        mRequestBuilder.setResponseMode("").build();
    }

    /* ******************************* responseType ***********************************************/

    @Test
    public void testResponseType() {
        AuthorizationRequest request = mRequestBuilder.build();
        assertThat(request.responseType).isEqualTo(ResponseTypeValues.CODE);
    }

    /* *********************************** scope **************************************************/

    @Test
    public void testScope_null() {
        AuthorizationRequest request = mRequestBuilder
                .setScopes((Iterable<String>)null)
                .build();
        assertThat(request.scope).isNull();
    }

    @Test
    public void testScope_empty() {
        AuthorizationRequest request = mRequestBuilder
                .setScopes()
                .build();
        assertThat(request.scope).isNull();
    }

    @Test
    public void testScope_emptyList() {
        AuthorizationRequest request = mRequestBuilder
                .setScopes(Collections.<String>emptyList())
                .build();
        assertThat(request.scope).isNull();
    }

    /* *********************************** state **************************************************/

    @Test
    public void testState_autoGenerated() {
        AuthorizationRequest request = mRequestBuilder.build();
        assertThat(request.state).isNotEmpty();
    }

    /* *********************************** nonce **************************************************/

    @Test
    public void testNonce_autoGenerated() {
        AuthorizationRequest request = mRequestBuilder.build();
        assertThat(request.nonce).isNotEmpty();
    }

    /* ********************************** claims **************************************************/

    @Test
    public void testClaims_unspecified() {
        AuthorizationRequest request = mRequestBuilder.build();
        assertThat(request.claims).isNull();
    }

    @Test
    public void testClaims() throws JSONException {
        JSONObject claims = new JSONObject(TEST_CLAIMS);
        AuthorizationRequest req = mRequestBuilder
            .setClaims(claims)
            .build();
        assertThat(req.claims.toString()).isEqualTo(claims.toString());
    }

    @Test
    public void testClaims_withNullValue() {
        AuthorizationRequest req = mRequestBuilder
            .setClaims(null)
            .build();

        assertThat(req.claims).isNull();
    }

    /* ******************************* claims_locales *********************************************/

    @Test
    public void testClaimsLocales_unspecified() {
        AuthorizationRequest request = mRequestBuilder.build();
        assertThat(request.claimsLocales).isNull();
        assertThat(request.getClaimsLocales()).isNull();
    }

    @Test
    public void testClaimsLocales() {
        AuthorizationRequest req = mRequestBuilder
            .setClaimsLocales("en de fr-CA")
            .build();

        assertThat(req.claimsLocales).isEqualTo("en de fr-CA");
        assertThat(req.getClaimsLocales())
            .hasSize(3)
            .contains("en")
            .contains("de")
            .contains("fr-CA");
    }

    @Test
    public void testClaimsLocales_nullValue() {
        AuthorizationRequest req = mRequestBuilder.setClaimsLocales(null).build();
        assertThat(req.claimsLocales).isNull();
        assertThat(req.getClaimsLocales()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testClaimsLocales_empty() {
        mRequestBuilder.setUiLocales("").build();
    }

    @Test
    public void testClaimsLocales_withVarargs() {
        AuthorizationRequest req = mRequestBuilder
            .setClaimsLocalesValues("en", "de", "fr-CA")
            .build();

        assertThat(req.claimsLocales).isEqualTo("en de fr-CA");
        assertThat(req.getClaimsLocales())
            .hasSize(3)
            .contains("en")
            .contains("de")
            .contains("fr-CA");
    }

    @Test
    public void testClaimsLocales_withNullVarargsArray() {
        AuthorizationRequest req = mRequestBuilder.setClaimsLocalesValues((String[])null).build();
        assertThat(req.claimsLocales).isNull();
        assertThat(req.getClaimsLocales()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testClaimsLocales_withNullStringInVarargs() {
        mRequestBuilder.setClaimsLocalesValues("en", null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testClaimsLocales_withEmptyStringInVarargs() {
        mRequestBuilder.setClaimsLocalesValues("en", "").build();
    }

    @Test
    public void testClaimsLocales_withIterable() {
        AuthorizationRequest req = mRequestBuilder
            .setClaimsLocalesValues(Arrays.asList("en", "de", "fr-CA"))
            .build();

        assertThat(req.claimsLocales).isEqualTo("en de fr-CA");

        assertThat(req.getClaimsLocales())
            .hasSize(3)
            .contains("en")
            .contains("de")
            .contains("fr-CA");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testClaimsLocales_withIterableContainingNullValue() {
        mRequestBuilder
            .setClaimsLocalesValues(Arrays.asList("en", null))
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testClaimsLocales_withIterableContainingEmptyValue() {
        mRequestBuilder
            .setClaimsLocalesValues(Arrays.asList("en", ""))
            .build();
    }

    /* ******************************* additionalParams *******************************************/

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_setAdditionalParams_withBuiltInParam() {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(AuthorizationRequest.PARAM_SCOPE, AuthorizationRequest.Scope.EMAIL);
        mRequestBuilder.setAdditionalParameters(additionalParams);
    }

    /* ******************************* toUri() ****************************************************/

    @Test
    public void testToUri() throws Exception {
        AuthorizationRequest request = mRequestBuilder.build();
        Uri uri = request.toUri();
        assertThat(uri.getQueryParameterNames())
                .isEqualTo(new HashSet<>(Arrays.asList(
                        AuthorizationRequest.PARAM_CLIENT_ID,
                        AuthorizationRequest.PARAM_RESPONSE_TYPE,
                        AuthorizationRequest.PARAM_REDIRECT_URI,
                        AuthorizationRequest.PARAM_STATE,
                        AuthorizationRequest.PARAM_NONCE,
                        AuthorizationRequest.PARAM_CODE_CHALLENGE,
                        AuthorizationRequest.PARAM_CODE_CHALLENGE_METHOD)));

        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_CLIENT_ID))
                .isEqualTo(TEST_CLIENT_ID);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_RESPONSE_TYPE))
                .isEqualTo(ResponseTypeValues.CODE);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_REDIRECT_URI))
                .isEqualTo(TEST_APP_REDIRECT_URI.toString());
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_STATE))
                .isEqualTo(request.state);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_NONCE))
                .isEqualTo(request.nonce);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_CODE_CHALLENGE))
                .isEqualTo(request.codeVerifierChallenge);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_CODE_CHALLENGE_METHOD))
                .isEqualTo(request.codeVerifierChallengeMethod);
    }

    @Test
    public void testToUri_noCodeVerifier() throws Exception {
        AuthorizationRequest req = mRequestBuilder.setCodeVerifier(null).build();
        assertThat(req.toUri().getQueryParameterNames())
                .doesNotContain(AuthorizationRequest.PARAM_CODE_CHALLENGE)
                .doesNotContain(AuthorizationRequest.PARAM_CODE_CHALLENGE_METHOD);
    }

    @Test
    public void testToUri_displayParam() {
        Uri uri = mRequestBuilder
                .setDisplay(AuthorizationRequest.Display.PAGE)
                .build()
                .toUri();
        assertThat(uri.getQueryParameterNames())
                .contains(AuthorizationRequest.PARAM_DISPLAY);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_DISPLAY))
                .isEqualTo(AuthorizationRequest.Display.PAGE);
    }

    @Test
    public void testToUri_loginHint() {
        Uri uri = mRequestBuilder
            .setLoginHint(TEST_EMAIL_ADDRESS)
            .build()
            .toUri();

        assertThat(uri.getQueryParameterNames())
            .contains(AuthorizationRequest.PARAM_LOGIN_HINT);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_LOGIN_HINT))
            .isEqualTo(TEST_EMAIL_ADDRESS);
    }


    @Test
    public void testToUri_promptParam() {
        Uri uri = mRequestBuilder
                .setPrompt(AuthorizationRequest.Prompt.CONSENT)
                .build()
                .toUri();
        assertThat(uri.getQueryParameterNames())
                .contains(AuthorizationRequest.PARAM_PROMPT);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_PROMPT))
                .isEqualTo(AuthorizationRequest.Prompt.CONSENT);
    }

    @Test
    public void testToUri_uiLocalesParam() {
        Uri uri = mRequestBuilder
            .setUiLocales("en de fr-CA")
            .build()
            .toUri();
        assertThat(uri.getQueryParameterNames())
            .contains(AuthorizationRequest.PARAM_UI_LOCALES);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_UI_LOCALES))
            .isEqualTo("en de fr-CA");
    }

    @Test
    public void testToUri_responseModeParam() {
        Uri uri = mRequestBuilder
                .setResponseMode(AuthorizationRequest.ResponseMode.QUERY)
                .build()
                .toUri();
        assertThat(uri.getQueryParameterNames())
                .contains(AuthorizationRequest.PARAM_RESPONSE_MODE);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_RESPONSE_MODE))
                .isEqualTo(AuthorizationRequest.ResponseMode.QUERY);
    }

    @Test
    public void testToUri_scopeParam() {
        Uri uri = mRequestBuilder
                .setScope(AuthorizationRequest.Scope.EMAIL)
                .build()
                .toUri();
        assertThat(uri.getQueryParameterNames())
                .contains(AuthorizationRequest.PARAM_SCOPE);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_SCOPE))
                .isEqualTo(AuthorizationRequest.Scope.EMAIL);
    }

    @Test
    public void testToUri_stateParam() {
        Uri uri = mRequestBuilder
                .setState(TEST_STATE)
                .build()
                .toUri();
        assertThat(uri.getQueryParameterNames())
                .contains(AuthorizationRequest.PARAM_STATE);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_STATE))
                .isEqualTo(TEST_STATE);
    }

    @Test
    public void testToUri_noStateParam() throws Exception {
        AuthorizationRequest req = mRequestBuilder.setState(null).build();
        assertThat(req.toUri().getQueryParameterNames())
                .doesNotContain(AuthorizationRequest.PARAM_STATE);
    }

    @Test
    public void testToUri_nonceParam() {
        Uri uri = mRequestBuilder
            .setNonce(TEST_NONCE)
            .build()
            .toUri();
        assertThat(uri.getQueryParameterNames())
            .contains(AuthorizationRequest.PARAM_NONCE);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_NONCE))
            .isEqualTo(TEST_NONCE);
    }

    @Test
    public void testToUri_noNonceParam() throws Exception {
        AuthorizationRequest req = mRequestBuilder.setNonce(null).build();
        assertThat(req.toUri().getQueryParameterNames())
            .doesNotContain(AuthorizationRequest.PARAM_NONCE);
    }

    @Test
    public void testToUri_claimsParam() throws JSONException {
        AuthorizationRequest req = mRequestBuilder
            .setClaims(new JSONObject(TEST_CLAIMS))
            .build();
        Uri uri = req.toUri();
        assertThat(uri.getQueryParameterNames())
            .contains(AuthorizationRequest.PARAM_CLAIMS);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_CLAIMS))
            .isEqualTo(req.claims.toString());
    }

    @Test
    public void testToUri_claimsLocalesParam() {
        Uri uri = mRequestBuilder
            .setClaimsLocales("en de fr-CA")
            .build()
            .toUri();
        assertThat(uri.getQueryParameterNames())
            .contains(AuthorizationRequest.PARAM_CLAIMS_LOCALES);
        assertThat(uri.getQueryParameter(AuthorizationRequest.PARAM_CLAIMS_LOCALES))
            .isEqualTo("en de fr-CA");
    }

    @Test
    public void testToUri_additionalParams() throws Exception {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put("my_param", "1234");
        additionalParams.put("another_param", "5678");
        AuthorizationRequest req = mRequestBuilder
                .setAdditionalParameters(additionalParams)
                .build();

        Uri uri = req.toUri();
        assertThat(uri.getQueryParameter("my_param"))
                .isEqualTo("1234");
        assertThat(uri.getQueryParameter("another_param"))
                .isEqualTo("5678");
    }

    /* ************************** jsonSerialize() / jsonDeserialize() *****************************/

    @Test
    public void testJsonSerialize_clientId() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder.setClientId(TEST_CLIENT_ID).build());
        assertThat(copy.clientId).isEqualTo(TEST_CLIENT_ID);
    }

    @Test
    public void testJsonSerialize_display() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder.setDisplay(AuthorizationRequest.Display.POPUP).build());
        assertThat(copy.display).isEqualTo(AuthorizationRequest.Display.POPUP);
    }

    @Test
    public void testJsonSerialize_loginHint() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder.setLoginHint(TEST_EMAIL_ADDRESS).build());
        assertThat(copy.loginHint).isEqualTo(TEST_EMAIL_ADDRESS);
    }

    @Test
    public void testJsonSerialize_prompt() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder.setPrompt(AuthorizationRequest.Prompt.CONSENT).build());
        assertThat(copy.prompt).isEqualTo(AuthorizationRequest.Prompt.CONSENT);
    }

    @Test
    public void testJsonSerialize_uiLocales() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
            mRequestBuilder.setUiLocales("en de fr-CA").build());
        assertThat(copy.uiLocales).isEqualTo("en de fr-CA");
    }

    @Test
    public void testJsonSerialize_redirectUri() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder.setRedirectUri(TEST_APP_REDIRECT_URI).build());
        assertThat(copy.redirectUri).isEqualTo(TEST_APP_REDIRECT_URI);
    }

    @Test
    public void testJsonSerialize_responseMode() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder.setResponseMode(AuthorizationRequest.ResponseMode.QUERY).build());
        assertThat(copy.responseMode).isEqualTo(AuthorizationRequest.ResponseMode.QUERY);
    }

    @Test
    public void testJsonSerialize_responseType() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder.setResponseType(ResponseTypeValues.CODE).build());
        assertThat(copy.responseType).isEqualTo(ResponseTypeValues.CODE);
    }

    @Test
    public void testJsonSerialize_scope() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder.setScope(AuthorizationRequest.Scope.EMAIL).build());
        assertThat(copy.scope).isEqualTo(AuthorizationRequest.Scope.EMAIL);
    }

    @Test
    public void testSerialization_scopeNull() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder.setScopes((Iterable<String>)null).build());
        assertThat(copy.scope).isNull();
    }

    @Test
    public void testSerialization_scopeEmpty() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder
                        .setScopes(Collections.<String>emptyList())
                        .build());
        assertThat(copy.scope).isNull();
    }

    @Test
    public void testJsonSerialize_state() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder.setState(TEST_STATE).build());
        assertThat(copy.state).isEqualTo(TEST_STATE);
    }

    @Test
    public void testJsonSerialize_nonce() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
            mRequestBuilder.setNonce(TEST_NONCE).build());
        assertThat(copy.nonce).isEqualTo(TEST_NONCE);
    }

    @Test
    public void testJsonSerialize_claims() throws Exception {
        JSONObject claims = new JSONObject(TEST_CLAIMS);
        AuthorizationRequest copy = serializeDeserialize(
            mRequestBuilder.setClaims(claims).build());
        assertThat(copy.claims.toString()).isEqualTo(claims.toString());
    }

    @Test
    public void testJsonSerialize_claimsLocales() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
            mRequestBuilder.setClaimsLocales("en de fr-CA").build());
        assertThat(copy.claimsLocales).isEqualTo("en de fr-CA");
    }

    @Test
    public void testJsonSerialize_additionalParams() throws Exception {
        AuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder.setAdditionalParameters(TEST_ADDITIONAL_PARAMS).build());
        assertThat(copy.additionalParameters).isEqualTo(TEST_ADDITIONAL_PARAMS);
    }

    private AuthorizationRequest serializeDeserialize(AuthorizationRequest request)
            throws JSONException {
        return AuthorizationRequest.jsonDeserialize(request.jsonSerializeString());
    }

    private String generateString(int length) {
        char[] chars = new char[length];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = '0';
        }

        return new String(chars);
    }
}
