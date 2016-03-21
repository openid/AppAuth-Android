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

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static net.openid.appauth.TestValues.TEST_APP_REDIRECT_URI;
import static net.openid.appauth.TestValues.TEST_APP_SCHEME;
import static net.openid.appauth.TestValues.getTestServiceConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RegistrationRequestTest {

    private static final Map<String, String> TEST_ADDITIONAL_PARAMS;

    static {
        TEST_ADDITIONAL_PARAMS = new HashMap<>();
        TEST_ADDITIONAL_PARAMS.put("test_key1", "test_value1");
        TEST_ADDITIONAL_PARAMS.put("test_key2", "test_value2");
    }

    private RegistrationRequest.Builder mMinimalRequestBuilder;

    @Before
    public void setUp() {
        mMinimalRequestBuilder = new RegistrationRequest.Builder(
                getTestServiceConfig(),
                TEST_APP_REDIRECT_URI);
    }

    @Test
    public void testBuilder() {
        assertValues(mMinimalRequestBuilder.build());
    }

    @Test(expected = NullPointerException.class)
    public void testBuild_nullConfiguration() {
        new RegistrationRequest.Builder(null, TEST_APP_REDIRECT_URI).build();
    }

    @Test(expected = NullPointerException.class)
    public void testBuild_nullRedirectUri() {
        new RegistrationRequest.Builder(getTestServiceConfig(), null)
                .build();
    }

    @Test
    public void testBuilder_setRedirectUriValues() {
        Uri redirect1 = Uri.parse(TEST_APP_SCHEME + ":/callback1");
        Uri redirect2 = Uri.parse(TEST_APP_SCHEME + ":/callback2");
        mMinimalRequestBuilder.setRedirectUriValues(redirect1, redirect2);
        RegistrationRequest request = mMinimalRequestBuilder.build();
        assertThat(request.redirectUris.containsAll(Arrays.asList(redirect1, redirect2))).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_setAdditionalParams_withBuiltInParam() {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(RegistrationRequest.PARAM_APPLICATION_TYPE, "web");
        mMinimalRequestBuilder.setAdditionalParameters(additionalParams);
    }

    @Test
    public void testApplicationTypeIsNativeByDefault() {
        RegistrationRequest request = mMinimalRequestBuilder.build();
        assertThat(request.applicationType).isEqualTo(RegistrationRequest.APPLICATION_TYPE_NATIVE);
    }

    @Test
    public void testToJsonString_withAdditionalParameters() throws JSONException {
        RegistrationRequest request = mMinimalRequestBuilder
                .setAdditionalParameters(TEST_ADDITIONAL_PARAMS)
                .build();
        String jsonStr = request.toJsonString();

        JSONObject json = new JSONObject(jsonStr);
        for (Map.Entry<String, String> param : TEST_ADDITIONAL_PARAMS.entrySet()) {
            assertThat(json.get(param.getKey())).isEqualTo(param.getValue());
        }

        assertThat(request.applicationType).isEqualTo(RegistrationRequest.APPLICATION_TYPE_NATIVE);
    }

    @Test
    public void testToJsonString() throws JSONException {
        RegistrationRequest request = mMinimalRequestBuilder
                .setResponseTypeValues(ResponseTypeValues.ID_TOKEN)
                .setGrantTypeValues(GrantTypeValues.IMPLICIT)
                .setSubjectType(RegistrationRequest.SUBJECT_TYPE_PAIRWISE)
                .build();
        String jsonStr = request.toJsonString();

        JSONObject json = new JSONObject(jsonStr);
        assertThat(json.get(RegistrationRequest.PARAM_REDIRECT_URIS))
                .isEqualTo(JsonUtil.toJsonArray(request.redirectUris));
        assertThat(json.get(RegistrationRequest.PARAM_APPLICATION_TYPE))
                .isEqualTo(request.APPLICATION_TYPE_NATIVE);
        assertThat(json.get(RegistrationRequest.PARAM_RESPONSE_TYPES))
                .isEqualTo(JsonUtil.toJsonArray(request.responseTypes));
        assertThat(json.get(RegistrationRequest.PARAM_GRANT_TYPES))
                .isEqualTo(JsonUtil.toJsonArray(request.grantTypes));
        assertThat(json.get(RegistrationRequest.PARAM_SUBJECT_TYPE))
                .isEqualTo(request.subjectType);
    }


    private void assertValues(RegistrationRequest request) {
        assertEquals("unexpected redirect URI", TEST_APP_REDIRECT_URI,
                request.redirectUris.iterator().next());
        assertEquals("unexpected application tyoe", RegistrationRequest.APPLICATION_TYPE_NATIVE,
                request.applicationType);
    }
}
