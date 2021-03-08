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
import static net.openid.appauth.TestValues.TEST_CODE_VERIFIER;
import static net.openid.appauth.TestValues.getTestServiceConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class TokenRequestTest {

    private static final String TEST_AUTHORIZATION_CODE = "ABCDEFGH";
    private static final String TEST_REFRESH_TOKEN = "IJKLMNOP";

    private TokenRequest.Builder mMinimalBuilder;
    private TokenRequest.Builder mAuthorizationCodeRequestBuilder;

    @Before
    public void setUp() {
        mMinimalBuilder = new TokenRequest.Builder(
                getTestServiceConfig(),
                TEST_CLIENT_ID);

        mAuthorizationCodeRequestBuilder = new TokenRequest.Builder(
                getTestServiceConfig(),
                TEST_CLIENT_ID)
                .setAuthorizationCode(TEST_AUTHORIZATION_CODE)
                .setRedirectUri(TEST_APP_REDIRECT_URI);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void testBuild_nullConfiguration() {
        new TokenRequest.Builder(null, TEST_CLIENT_ID).build();
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void testBuild_nullClientId() {
        new TokenRequest.Builder(getTestServiceConfig(), null)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuild_emptyClientId() {
        new TokenRequest.Builder(getTestServiceConfig(), "")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuild_emptyAuthorizationCode() {
        mMinimalBuilder
                .setAuthorizationCode("")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuild_emptyRefreshToken() {
        mMinimalBuilder
                .setRefreshToken("")
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuild_noRedirectUriForAuthorizationCodeExchange() {
        mMinimalBuilder
                .setAuthorizationCode(TEST_AUTHORIZATION_CODE)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testBuild_additionalParamWithNullValue() {
        Map<String, String> badMap = new HashMap<>();
        badMap.put("x", null);
        mMinimalBuilder
                .setAdditionalParameters(badMap)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_setAdditionalParams_withBuiltInParam() {
        mMinimalBuilder.setAdditionalParameters(
                Collections.singletonMap(TokenRequest.PARAM_SCOPE, "scope"));
    }


    @Test(expected = IllegalArgumentException.class)
    public void testBuild_badScopeString() {
        mMinimalBuilder
                .setScopes("")
                .build();
    }

    @Test
    public void testGetRequestParameters_forCodeExchange() {
        TokenRequest request = mAuthorizationCodeRequestBuilder.build();

        Map<String, String> params = request.getRequestParameters();
        assertThat(params).containsEntry(
                TokenRequest.PARAM_GRANT_TYPE,
                GrantTypeValues.AUTHORIZATION_CODE);
        assertThat(params).containsEntry(
                TokenRequest.PARAM_CODE,
                TEST_AUTHORIZATION_CODE);
        assertThat(params).containsEntry(
                TokenRequest.PARAM_REDIRECT_URI,
                TEST_APP_REDIRECT_URI.toString());
    }

    @Test
    public void testGetRequestParameters_forRefreshToken() {
        TokenRequest request = mMinimalBuilder
                .setRefreshToken(TEST_REFRESH_TOKEN)
                .build();

        Map<String, String> params = request.getRequestParameters();
        assertThat(params).containsEntry(
                TokenRequest.PARAM_GRANT_TYPE,
                GrantTypeValues.REFRESH_TOKEN);
        assertThat(params).containsEntry(
                TokenRequest.PARAM_REFRESH_TOKEN,
                TEST_REFRESH_TOKEN);
    }

    @Test
    public void testGetRequestParameters_withCodeVerifier() {
        TokenRequest request = mAuthorizationCodeRequestBuilder
                .setCodeVerifier(TEST_CODE_VERIFIER)
                .build();

        assertThat(request.getRequestParameters())
                .containsEntry(TokenRequest.PARAM_CODE_VERIFIER, TEST_CODE_VERIFIER);
    }

    @Test
    public void testToUri_withScope() {
        TokenRequest request = mMinimalBuilder
                .setRefreshToken(TEST_REFRESH_TOKEN)
                .setScope("email profile")
                .build();

        assertThat(request.getRequestParameters())
                .containsEntry(TokenRequest.PARAM_SCOPE, "email profile");
    }

    @Test
    public void testToUri_withAdditionalParameters() {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put("p1", "v1");
        additionalParams.put("p2", "v2");
        TokenRequest request = mAuthorizationCodeRequestBuilder
                .setAdditionalParameters(additionalParams)
                .build();

        Map<String, String> params = request.getRequestParameters();
        assertThat(params).containsEntry("p1", "v1");
        assertThat(params).containsEntry("p2", "v2");
    }
}
