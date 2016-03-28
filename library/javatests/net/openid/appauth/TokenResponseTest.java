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
import static net.openid.appauth.TestValues.TEST_AUTH_CODE;
import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.getTestServiceConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class TokenResponseTest {

    private TokenResponse.Builder mMinimalBuilder;

    @Before
    public void setUp() {
        TokenRequest request = new TokenRequest.Builder(getTestServiceConfig(), TEST_CLIENT_ID)
                .setAuthorizationCode(TEST_AUTH_CODE)
                .setRedirectUri(TEST_APP_REDIRECT_URI)
                .build();
        mMinimalBuilder = new TokenResponse.Builder(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_setAdditionalParams_withBuiltInParam() {
        mMinimalBuilder.setAdditionalParameters(
                Collections.singletonMap(TokenRequest.PARAM_SCOPE, "scope"));
    }
}
