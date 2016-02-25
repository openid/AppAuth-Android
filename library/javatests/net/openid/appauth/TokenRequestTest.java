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

import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.getTestServiceConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TokenRequestTest {

    private static final String TEST_AUTHORIZATION_CODE = "ABCDEFGH";
    private static final String TEST_REFRESH_TOKEN = "IJKLMNOP";


    @Test(expected = NullPointerException.class)
    public void testBuild_nullConfiguration() {
        new TokenRequest.Builder(null, TEST_CLIENT_ID).build();
    }

    @Test(expected = IllegalArgumentException.class)
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
        new TokenRequest.Builder(getTestServiceConfig(), TEST_CLIENT_ID)
                .setAuthorizationCode("")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuild_emptyRefreshToken() {
        new TokenRequest.Builder(getTestServiceConfig(), TEST_CLIENT_ID)
                .setRefreshToken("")
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuild_noRedirectUriForAuthorizationCodeExchange() {
        new TokenRequest.Builder(getTestServiceConfig(), TEST_CLIENT_ID)
                .setAuthorizationCode(TEST_AUTHORIZATION_CODE)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testBuild_additionalParamWithNullValue() {
        Map<String, String> badMap = new HashMap<>();
        badMap.put("x", null);
        new TokenRequest.Builder(getTestServiceConfig(), TEST_CLIENT_ID)
                .setAdditionalParameters(badMap)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuild_badScopeString() {
        new TokenRequest.Builder(getTestServiceConfig(), TEST_CLIENT_ID)
                .setScopes("")
                .build();
    }
}
