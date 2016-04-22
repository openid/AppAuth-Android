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
import static net.openid.appauth.TestValues.TEST_AUTH_CODE;
import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.TEST_CLIENT_SECRET;
import static net.openid.appauth.TestValues.getTestServiceConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ClientAuthenticationTest {

    private TokenRequest mTokenRequest;
    private OutputStream mOutputStream;
    @Mock
    HttpURLConnection mHttpConnection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mOutputStream = new ByteArrayOutputStream();
        when(mHttpConnection.getOutputStream()).thenReturn(mOutputStream);

        mTokenRequest = new TokenRequest.Builder(getTestServiceConfig(), TEST_CLIENT_ID)
                .setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
                .setAuthorizationCode(TEST_AUTH_CODE)
                .setRedirectUri(TEST_APP_REDIRECT_URI)
                .build();
    }


    @Test
    public void testApply() throws Exception {
        ClientAuthentication ca = new ClientAuthentication(TEST_CLIENT_SECRET);
        ca.apply(mTokenRequest, mHttpConnection);

        Uri postBody = new Uri.Builder().encodedQuery(mOutputStream.toString()).build();
        assertQueryParameters(postBody);
    }

    @Test
    public void testApply_withExtraAuthenticationParameters() throws Exception {
        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("test1", "value1");
        extraParams.put("test2", "value2 value3");
        ClientAuthentication csp = spy(new ClientAuthentication(TEST_CLIENT_SECRET));
        when(csp.setupRequestParameters(TEST_CLIENT_ID, mHttpConnection)).thenReturn(extraParams);

        csp.apply(mTokenRequest, mHttpConnection);
        Uri postBody = new Uri.Builder().encodedQuery(mOutputStream.toString()).build();
        assertQueryParameters(postBody);
        assertThat(postBody.getQueryParameter("test1")).isEqualTo("value1");
        assertThat(postBody.getQueryParameter("test2")).isEqualTo("value2 value3");
    }

    private void assertQueryParameters(Uri postBody) {
        assertThat(postBody.getQueryParameter(TokenRequest.PARAM_CODE))
                .isEqualTo(mTokenRequest.authorizationCode);
        assertThat(postBody.getQueryParameter(TokenRequest.PARAM_GRANT_TYPE))
                .isEqualTo(mTokenRequest.grantType);
        assertThat(postBody.getQueryParameter(TokenRequest.PARAM_REDIRECT_URI))
                .isEqualTo(mTokenRequest.redirectUri.toString());
    }
}
