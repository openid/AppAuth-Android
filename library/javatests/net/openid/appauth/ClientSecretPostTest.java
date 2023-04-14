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

import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.TEST_CLIENT_SECRET;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class ClientSecretPostTest {
    @Test
    public void testGetRequestParameters() throws Exception {
        ClientSecretPost csp = new ClientSecretPost(TEST_CLIENT_SECRET);
        Map<String, String> parameters = csp.getRequestParameters(TEST_CLIENT_ID);
        assertThat(parameters).containsEntry(ClientSecretPost.PARAM_CLIENT_ID, TEST_CLIENT_ID);
        assertThat(parameters).containsEntry(
                ClientSecretPost.PARAM_CLIENT_SECRET, TEST_CLIENT_SECRET);
    }

    @Test
    public void testGetRequestHeaders() throws Exception {
        ClientSecretPost csp = new ClientSecretPost(TEST_CLIENT_SECRET);
        assertThat(csp.getRequestHeaders(TEST_CLIENT_ID)).isNull();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_withNull() {
        new ClientSecretPost(null);
    }
}
