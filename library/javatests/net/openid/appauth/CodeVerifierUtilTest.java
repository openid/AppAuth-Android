/*
 * Copyright 2017 The AppAuth for Android Authors. All Rights Reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.security.SecureRandom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class CodeVerifierUtilTest {

    @Test
    public void checkCodeVerifier_tooShort_throwsException() {
        String codeVerifier = createString(CodeVerifierUtil.MIN_CODE_VERIFIER_LENGTH - 1);
        try {
            CodeVerifierUtil.checkCodeVerifier(codeVerifier);
            fail("expected exception not thrown");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage())
                .isEqualTo("codeVerifier length is shorter than allowed by the PKCE specification");
        }
    }

    @Test
    public void checkCodeVerifier_tooLong_throwsException() {
        String codeVerifier = createString(CodeVerifierUtil.MAX_CODE_VERIFIER_LENGTH + 1);
        try {
            CodeVerifierUtil.checkCodeVerifier(codeVerifier);
            fail("expected exception not thrown");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage())
                .isEqualTo("codeVerifier length is longer than allowed by the PKCE specification");
        }
    }

    @Test
    public void generateRandomCodeVerifier_nullEntropySource_throwsException() {
        try {
            CodeVerifierUtil.generateRandomCodeVerifier(
                null,
                CodeVerifierUtil.MIN_CODE_VERIFIER_ENTROPY);
            fail("expected exception not thrown");
        } catch (NullPointerException ex) {
            assertThat(ex.getMessage())
                .isEqualTo("entropySource cannot be null");
        }
    }

    @Test
    public void generateRandomCodeVerifier_tooLittleEntropy_throwsException() {
        try {
            CodeVerifierUtil.generateRandomCodeVerifier(
                new SecureRandom(),
                CodeVerifierUtil.MIN_CODE_VERIFIER_ENTROPY - 1);
            fail("expected exception not thrown");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage())
                .isEqualTo("entropyBytes is less than the minimum permitted");
        }
    }

    @Test
    public void generateRandomCodeVerifier_tooMuchEntropy_throwsException() {
        try {
            CodeVerifierUtil.generateRandomCodeVerifier(
                new SecureRandom(),
                CodeVerifierUtil.MAX_CODE_VERIFIER_ENTROPY + 1);
            fail("expected exception not thrown");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage())
                .isEqualTo("entropyBytes is greater than the maximum permitted");
        }
    }

    private String createString(int length) {
        char[] strChars = new char[length];
        for (int i = 0; i < strChars.length; i++) {
            strChars[i] = 'a';
        }
        return new String(strChars);
    }
}
