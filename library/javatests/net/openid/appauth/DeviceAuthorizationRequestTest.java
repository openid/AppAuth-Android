/*
 * Copyright 2021 The AppAuth for Android Authors. All Rights Reserved.
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
import static org.assertj.core.api.Assertions.assertThat;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class DeviceAuthorizationRequestTest {

    private static final Map<String, String> TEST_ADDITIONAL_PARAMS;

    static {
        TEST_ADDITIONAL_PARAMS = new HashMap<>();
        TEST_ADDITIONAL_PARAMS.put("test_key1", "test_value1");
        TEST_ADDITIONAL_PARAMS.put("test_key2", "test_value2");
    }

    private DeviceAuthorizationRequest.Builder mRequestBuilder;

    @Before
    public void setUp() {
        mRequestBuilder = new DeviceAuthorizationRequest.Builder(
                getTestServiceConfig(),
                TEST_CLIENT_ID);
    }

    /* ********************************** Builder() ***********************************************/

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void testBuilder_nullConfiguration() {
        new DeviceAuthorizationRequest.Builder(
                null,
                TEST_CLIENT_ID);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void testBuilder_nullClientId() {
        new DeviceAuthorizationRequest.Builder(
                getTestServiceConfig(),
                null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_emptyClientId() {
        new DeviceAuthorizationRequest.Builder(
                getTestServiceConfig(),
                "");
    }

    /* ************************************** clientId ********************************************/

    @Test
    public void testClientId_fromConstructor() {
        DeviceAuthorizationRequest request = mRequestBuilder.build();
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

    /* *********************************** scope **************************************************/

    @Test
    public void testScope_null() {
        DeviceAuthorizationRequest request = mRequestBuilder
                .setScopes((Iterable<String>)null)
                .build();
        assertThat(request.scope).isNull();
    }

    @Test
    public void testScope_empty() {
        DeviceAuthorizationRequest request = mRequestBuilder
                .setScopes()
                .build();
        assertThat(request.scope).isNull();
    }

    @Test
    public void testScope_emptyList() {
        DeviceAuthorizationRequest request = mRequestBuilder
                .setScopes(Collections.<String>emptyList())
                .build();
        assertThat(request.scope).isNull();
    }

    /* ******************************* additionalParams *******************************************/

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_setAdditionalParams_withBuiltInParam() {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(AuthorizationRequest.PARAM_SCOPE, AuthorizationRequest.Scope.EMAIL);
        mRequestBuilder.setAdditionalParameters(additionalParams);
    }

    /* ************************** jsonSerialize() / jsonDeserialize() *****************************/

    @Test
    public void testJsonSerialize_clientId() throws Exception {
        DeviceAuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder.setClientId(TEST_CLIENT_ID).build());
        assertThat(copy.clientId).isEqualTo(TEST_CLIENT_ID);
    }

    @Test
    public void testJsonSerialize_scope() throws Exception {
        DeviceAuthorizationRequest copy = serializeDeserialize(
            mRequestBuilder.setScope(AuthorizationRequest.Scope.EMAIL).build());
        assertThat(copy.scope).isEqualTo(AuthorizationRequest.Scope.EMAIL);
    }

    @Test
    public void testSerialization_scopeNull() throws Exception {
        DeviceAuthorizationRequest copy = serializeDeserialize(
            mRequestBuilder.setScopes((Iterable<String>)null).build());
        assertThat(copy.scope).isNull();
    }

    @Test
    public void testSerialization_scopeEmpty() throws Exception {
        DeviceAuthorizationRequest copy = serializeDeserialize(
            mRequestBuilder
                .setScopes(Collections.<String>emptyList())
                .build());
        assertThat(copy.scope).isNull();
    }

    @Test
    public void testJsonSerialize_additionalParams() throws Exception {
        DeviceAuthorizationRequest copy = serializeDeserialize(
                mRequestBuilder.setAdditionalParameters(TEST_ADDITIONAL_PARAMS).build());
        assertThat(copy.additionalParameters).isEqualTo(TEST_ADDITIONAL_PARAMS);
    }

    private DeviceAuthorizationRequest serializeDeserialize(DeviceAuthorizationRequest request)
            throws JSONException {
        return DeviceAuthorizationRequest.jsonDeserialize(request.jsonSerializeString());
    }

}
