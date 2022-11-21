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


import org.assertj.core.data.MapEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class NoClientAuthenticationTest {

    @Test
    public void testGetRequestHeaders() {
        assertThat(NoClientAuthentication.INSTANCE.getRequestHeaders(TEST_CLIENT_ID)).isNull();
    }

    @Test
    public void testGetRequestParameters() {
        assertThat(NoClientAuthentication.INSTANCE.getRequestParameters(TEST_CLIENT_ID))
                .containsExactly(MapEntry.entry(TokenRequest.PARAM_CLIENT_ID, TEST_CLIENT_ID));
    }
}
