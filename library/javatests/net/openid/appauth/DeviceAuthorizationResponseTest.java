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

import static net.openid.appauth.TestValues.TEST_DEVICE_CODE;
import static net.openid.appauth.TestValues.TEST_DEVICE_CODE_EXPIRES_IN;
import static net.openid.appauth.TestValues.TEST_DEVICE_CODE_POLL_INTERVAL;
import static net.openid.appauth.TestValues.TEST_DEVICE_USER_CODE;
import static net.openid.appauth.TestValues.TEST_DEVICE_VERIFICATION_COMPLETE_URI;
import static net.openid.appauth.TestValues.TEST_DEVICE_VERIFICATION_URI;
import static net.openid.appauth.TestValues.getMinimalDeviceAuthRequestBuilder;
import static net.openid.appauth.TestValues.getTestDeviceAuthorizationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class DeviceAuthorizationResponseTest {

    // the test is asserted to be running at time 23
    private static final Long TEST_START_TIME = 23L;
    // expiration time, in seconds
    private static final Long TEST_EXPIRES_IN = 79L;
    private static final Long TEST_CODE_EXPIRE_TIME = 79023L;

    private DeviceAuthorizationResponse.Builder mDeviceAuthResponseBuilder;
    private DeviceAuthorizationResponse mDeviceAuthResponse;

    TestClock mClock;

    @Before
    public void setUp() {
        mClock = new TestClock(TEST_START_TIME);
        mDeviceAuthResponseBuilder = new DeviceAuthorizationResponse.Builder(
                getTestDeviceAuthorizationRequest())
            .setDeviceCode(TEST_DEVICE_CODE)
            .setUserCode(TEST_DEVICE_USER_CODE)
            .setVerificationUri(TEST_DEVICE_VERIFICATION_URI)
            .setVerificationUriComplete(TEST_DEVICE_VERIFICATION_COMPLETE_URI)
            .setCodeExpiresIn(TEST_DEVICE_CODE_EXPIRES_IN, mClock)
            .setTokenPollingIntervalTime(TEST_DEVICE_CODE_POLL_INTERVAL);

        mDeviceAuthResponse = mDeviceAuthResponseBuilder.build();
    }

    @Test
    public void testBuilder() {
        checkExpectedFields(mDeviceAuthResponseBuilder.build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuild_setAdditionalParams_withBuiltInParam() {
        mDeviceAuthResponseBuilder.setAdditionalParameters(
                Collections.singletonMap(DeviceAuthorizationResponse.KEY_DEVICE_CODE,
                        "deviceCode"));
    }

    @Test
    public void testExpiresIn() {
        DeviceAuthorizationResponse deviceAuthResponse = mDeviceAuthResponseBuilder
                .setCodeExpiresIn(TEST_EXPIRES_IN, mClock)
                .build();
        assertEquals(TEST_CODE_EXPIRE_TIME, deviceAuthResponse.codeExpirationTime);
    }

    @Test
    public void testExpirationTime() {
        DeviceAuthorizationResponse deviceAuthResponse = mDeviceAuthResponseBuilder
            .setCodeExpirationTime(TEST_CODE_EXPIRE_TIME)
            .build();
        assertEquals(TEST_CODE_EXPIRE_TIME, deviceAuthResponse.codeExpirationTime);
    }

    @Test
    public void testHasExpired() {
        mClock.currentTime.set(TEST_START_TIME + 1);
        assertFalse(mDeviceAuthResponse.hasCodeExpired(mClock));
        mClock.currentTime.set(TEST_CODE_EXPIRE_TIME - 1);
        assertFalse(mDeviceAuthResponse.hasCodeExpired(mClock));
        mClock.currentTime.set(TEST_CODE_EXPIRE_TIME + 1);
        assertTrue(mDeviceAuthResponse.hasCodeExpired(mClock));
    }

    @Test
    public void testSerialization() throws Exception {
        String json = mDeviceAuthResponse.jsonSerializeString();
        DeviceAuthorizationResponse deviceAuthResponse = DeviceAuthorizationResponse
                .jsonDeserialize(json);
        checkExpectedFields(deviceAuthResponse);
    }

    @Test
    public void testCreateTokenExchangeRequest() {
        TokenRequest tokenExchangeRequest = mDeviceAuthResponse.createTokenExchangeRequest();
        assertThat(tokenExchangeRequest.grantType)
            .isEqualTo(GrantTypeValues.DEVICE_CODE);
        assertThat(tokenExchangeRequest.deviceCode)
            .isEqualTo(TEST_DEVICE_CODE);
    }

    @Test
    public void testCreateTokenExchangeRequest_failsForImplicitFlowResponse() {
        // simulate an implicit flow request and response
        DeviceAuthorizationRequest request = getMinimalDeviceAuthRequestBuilder().build();
        DeviceAuthorizationResponse response = new DeviceAuthorizationResponse.Builder(request)
            .setUserCode(TEST_DEVICE_USER_CODE)
            .setVerificationUri(TEST_DEVICE_VERIFICATION_URI)
            .setVerificationUriComplete(TEST_DEVICE_VERIFICATION_COMPLETE_URI)
            .setCodeExpiresIn(TEST_DEVICE_CODE_EXPIRES_IN)
            .setTokenPollingIntervalTime(TEST_DEVICE_CODE_POLL_INTERVAL)
            .build();

        // as there is no device code in the response, this will fail
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(response::createTokenExchangeRequest)
            .withMessage("deviceCode not available for exchange request");
    }

    private void checkExpectedFields(DeviceAuthorizationResponse deviceAuthResponse) {
        assertEquals("device code does not match",
            TEST_DEVICE_CODE, deviceAuthResponse.deviceCode);
        assertEquals("user code does not match",
            TEST_DEVICE_USER_CODE, deviceAuthResponse.userCode);
        assertEquals("verification uri does not match",
            TEST_DEVICE_VERIFICATION_URI, deviceAuthResponse.verificationUri);
        assertEquals("verification uri complete does not match",
            TEST_DEVICE_VERIFICATION_COMPLETE_URI, deviceAuthResponse.verificationUriComplete);
        assertEquals("user code expiration time does not match",
            TEST_CODE_EXPIRE_TIME, deviceAuthResponse.codeExpirationTime);
        assertEquals("access token polling interval does not match",
            TEST_DEVICE_CODE_POLL_INTERVAL, deviceAuthResponse.tokenPollingIntervalTime);
    }
}
