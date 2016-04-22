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

import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.TEST_CLIENT_SECRET;
import static org.mockito.Mockito.verify;

import android.util.Base64;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.HttpURLConnection;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ClientSecretBasicTest {

    @Mock
    HttpURLConnection mHttpConnection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSetupRequestParameters() throws Exception {
        ClientSecretBasic csb = new ClientSecretBasic(TEST_CLIENT_SECRET);

        csb.setupRequestParameters(TEST_CLIENT_ID, mHttpConnection);

        String credentials = TEST_CLIENT_ID + ":" + TEST_CLIENT_SECRET;
        String authz = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        String expectedAuthzHeader = "Basic " + authz;
        verify(mHttpConnection).setRequestProperty("Authorization", expectedAuthzHeader);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_withNull() {
        new ClientSecretBasic(null);
    }
}
