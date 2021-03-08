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
public class ClientSecretBasicTest {
    @Test
    public void testGetRequestHeaders() {
        ClientSecretBasic csb = new ClientSecretBasic(TEST_CLIENT_SECRET);
        Map<String, String> headers = csb.getRequestHeaders(TEST_CLIENT_ID);

        String expectedAuthzHeader = "Basic dGVzdF9jbGllbnRfaWQ6dGVzdF9jbGllbnRfc2VjcmV0";
        assertThat(headers.size()).isEqualTo(1);
        assertThat(headers).containsEntry("Authorization", expectedAuthzHeader);
    }

    @Test
    public void testGetRequestHeaders_idAndSecretAreUrlEncoded() {
        String secretThatChangesWhenEncoded = "1/2_3+4";
        String idThatChangesWhenEncoded = "0!1*$$";
        ClientSecretBasic csb = new ClientSecretBasic(secretThatChangesWhenEncoded);

        String expectedAuthzHeader = "Basic MCUyMTEqJTI0JTI0OjElMkYyXzMlMkI0";

        Map<String, String> headers = csb.getRequestHeaders(idThatChangesWhenEncoded);
        assertThat(headers).containsEntry("Authorization", expectedAuthzHeader);
    }

    @Test
    public void testGetRequestHeaders_testValuesFromIssue337() {
        // see: https://github.com/openid/AppAuth-Android/issues/337

        String secret = "z/tZ9VwFZqApmIQ+ZH1I5pLk/uB4ud:X2/8bL+wfFTt1rFw=";
        String id = "1PpG/Q 1";
        ClientSecretBasic csb = new ClientSecretBasic(secret);

        String expectedAuthzHeader = "Basic " +
            "MVBwRyUyRlErMTp6JTJGdFo5VndGWnFBcG1JUSUyQlpIMUk1cExrJTJGdUI0dWQl" +
            "M0FYMiUyRjhiTCUyQndmRlR0MXJGdyUzRA==";

        assertThat(csb.getRequestHeaders(id)).containsEntry("Authorization", expectedAuthzHeader);
    }

    @Test
    public void testGetRequestHeaders_encodingTests() {
        // the set of characters that must be transformed is defined in the WHATWG URL spec here:
        // https://url.spec.whatwg.org/#urlencoded-serializing

        // based on the above, let the secret be the string containing all the ascii characters that
        // are not encoded ...
        String secret = "*-._0123456789abcdefghijklmnoprstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

        // ... and let the id contain a selection of characters that do require encoding
        String id = "!@#$%^&()_=+µΩß¡";

        ClientSecretBasic csb = new ClientSecretBasic(secret);

        String expectedAuthzHeader = "Basic " +
            "JTIxJTQwJTIzJTI0JTI1JTVFJTI2JTI4JTI5XyUzRCUyQiVDMiVCNSVDRSVBOSVD" +
            "MyU5RiVDMiVBMToqLS5fMDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3Byc3R1dnd4" +
            "eXpBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWg==";

        Map<String, String> headers = csb.getRequestHeaders(id);
        assertThat(headers).containsEntry("Authorization", expectedAuthzHeader);
    }

    @Test
    public void testGetRequestParameters() {
        ClientSecretBasic csb = new ClientSecretBasic(TEST_CLIENT_SECRET);
        assertThat(csb.getRequestParameters(TEST_CLIENT_ID)).isNull();
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void testConstructor_withNull() {
        new ClientSecretBasic(null);
    }
}
