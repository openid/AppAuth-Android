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

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.openid.appauth.TestValues.TEST_APP_REDIRECT_URI;
import static net.openid.appauth.TestValues.TEST_APP_SCHEME;
import static net.openid.appauth.TestValues.getTestServiceConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class RegistrationRequestTest {

    private static final Map<String, String> TEST_ADDITIONAL_PARAMS;

    static {
        TEST_ADDITIONAL_PARAMS = new HashMap<>();
        TEST_ADDITIONAL_PARAMS.put("test_key1", "test_value1");
        TEST_ADDITIONAL_PARAMS.put("test_key2", "test_value2");
    }

    private static final String TEST_JSON = "{\n"
            + " \"application_type\": \"" + RegistrationRequest.APPLICATION_TYPE_NATIVE + "\",\n"
            + " \"redirect_uris\": [\"" + TEST_APP_REDIRECT_URI + "\"],\n"
            + " \"subject_type\": \"" + RegistrationRequest.SUBJECT_TYPE_PAIRWISE + "\",\n"
            + " \"response_types\": [\"" + ResponseTypeValues.ID_TOKEN + "\"],\n"
            + " \"grant_types\": [\"" + GrantTypeValues.IMPLICIT + "\"]\n"
            + "}";

    public static final Uri TEST_JWKS_URI = Uri.parse("https://mydomain/path/keys");
    private static final String TEST_JWKS = "{\n"
        + " \"keys\": [\n"
        + "  {\n"
        + "   \"kty\": \"RSA\",\n"
        + "   \"kid\": \"key1\",\n"
        + "   \"n\": \"AJnc...L0HU=\",\n"
        + "   \"e\": \"AQAB\"\n"
        + "  }\n"
        + " ]\n"
        + "}";


    private RegistrationRequest.Builder mMinimalRequestBuilder;
    private RegistrationRequest.Builder mMaximalRequestBuilder;
    private JSONObject mJson;
    private List<Uri> mRedirectUris;

    @Before
    public void setUp() throws JSONException {
        mRedirectUris = Arrays.asList(TEST_APP_REDIRECT_URI);
        mMinimalRequestBuilder = new RegistrationRequest.Builder(
                getTestServiceConfig(), mRedirectUris);
        mMaximalRequestBuilder = new RegistrationRequest.Builder(
                getTestServiceConfig(), mRedirectUris)
                .setResponseTypeValues(ResponseTypeValues.ID_TOKEN)
                .setGrantTypeValues(GrantTypeValues.IMPLICIT)
                .setSubjectType(RegistrationRequest.SUBJECT_TYPE_PAIRWISE);
        mJson = new JSONObject(TEST_JSON);
    }

    @Test
    public void testBuilder() {
        assertValues(mMinimalRequestBuilder.build());
    }

    @Test(expected = NullPointerException.class)
    public void testBuild_nullConfiguration() {
        new RegistrationRequest.Builder(null, mRedirectUris).build();
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
        RegistrationRequest request = mMaximalRequestBuilder.build();
        String jsonStr = request.toJsonString();
        assertMaximalValuesInJson(request, new JSONObject(jsonStr));
    }

    @Test
    public void testToJsonString_withJwksUri() throws JSONException {
        RegistrationRequest request = mMinimalRequestBuilder
            .setJwksUri(TEST_JWKS_URI)
            .build();

        String jsonStr = request.toJsonString();
        JSONObject json = new JSONObject(jsonStr);

        assertThat(Uri.parse(json.getString(RegistrationRequest.PARAM_JWKS_URI)))
            .isEqualTo(TEST_JWKS_URI);
    }


    @Test
    public void testToJsonString_withJwks() throws JSONException {
        RegistrationRequest request = mMinimalRequestBuilder
            .setJwks(new JSONObject(TEST_JWKS))
            .build();
        assertThat(request.jwks).isNotNull();

        String jsonStr = request.toJsonString();
        JSONObject json = new JSONObject(jsonStr);

        assertThat(json.getJSONObject(RegistrationRequest.PARAM_JWKS).toString())
            .isEqualTo(request.jwks.toString());
    }

    @Test
    public void testSerialize() throws JSONException {
        RegistrationRequest request = mMaximalRequestBuilder.build();
        JSONObject json = request.jsonSerialize();
        assertMaximalValuesInJson(request, json);
        assertThat(json.getJSONObject(RegistrationRequest.KEY_CONFIGURATION).toString())
                .isEqualTo(request.configuration.toJson().toString());
    }

    @Test
    public void testSerialize_withAdditionalParameters() throws JSONException {
        Map<String, String> additionalParameters = Collections.singletonMap("test1", "value1");
        RegistrationRequest request = mMaximalRequestBuilder
                .setAdditionalParameters(additionalParameters).build();
        JSONObject json = request.jsonSerialize();
        assertMaximalValuesInJson(request, json);
        assertThat(JsonUtil.getStringMap(json, RegistrationRequest.KEY_ADDITIONAL_PARAMETERS))
                .isEqualTo(additionalParameters);
    }

    @Test
    public void testDeserialize() throws JSONException {
        mJson.put(RegistrationRequest.KEY_CONFIGURATION, getTestServiceConfig().toJson());
        RegistrationRequest request = RegistrationRequest.jsonDeserialize(mJson);
        assertThat(request.configuration.toJsonString())
                .isEqualTo(getTestServiceConfig().toJsonString());
        assertMaximalValuesInJson(request, mJson);
    }

    @Test
    public void testDeserialize_withAdditionalParameters() throws JSONException {
        mJson.put(RegistrationRequest.KEY_CONFIGURATION, getTestServiceConfig().toJson());
        Map<String, String> additionalParameters = new HashMap<>();
        additionalParameters.put("key1", "value1");
        additionalParameters.put("key2", "value2");
        mJson.put(RegistrationRequest.KEY_ADDITIONAL_PARAMETERS,
                JsonUtil.mapToJsonObject(additionalParameters));
        RegistrationRequest request = RegistrationRequest.jsonDeserialize(mJson);
        assertThat(request.additionalParameters).isEqualTo(additionalParameters);
    }

    private void assertValues(RegistrationRequest request) {
        assertEquals("unexpected redirect URI", TEST_APP_REDIRECT_URI,
                request.redirectUris.iterator().next());
        assertEquals("unexpected application type", RegistrationRequest.APPLICATION_TYPE_NATIVE,
                request.applicationType);
    }

    private void assertMaximalValuesInJson(RegistrationRequest request, JSONObject json)
            throws JSONException {
        assertThat(json.get(RegistrationRequest.PARAM_REDIRECT_URIS))
                .isEqualTo(JsonUtil.toJsonArray(request.redirectUris));
        assertThat(json.get(RegistrationRequest.PARAM_APPLICATION_TYPE))
                .isEqualTo(RegistrationRequest.APPLICATION_TYPE_NATIVE);
        assertThat(json.get(RegistrationRequest.PARAM_RESPONSE_TYPES))
                .isEqualTo(JsonUtil.toJsonArray(request.responseTypes));
        assertThat(json.get(RegistrationRequest.PARAM_GRANT_TYPES))
                .isEqualTo(JsonUtil.toJsonArray(request.grantTypes));
        assertThat(json.get(RegistrationRequest.PARAM_SUBJECT_TYPE))
                .isEqualTo(request.subjectType);
    }
}
