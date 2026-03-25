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

package net.openid.appauth.app2app;

import android.util.Base64;
import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;
import java.util.Set;

final class CertificateFingerprintEncoding {

    private static final int DECIMAL = 10;
    private static final int HEXADECIMAL = 16;
    private static final int HALF_BYTE = 4;

    private CertificateFingerprintEncoding() {}

    /**
     * This method takes the certificate fingerprints from the '/.well-known/assetlinks.json' file
     * and decodes it in the correct way to compare the hashes with the ones found on the device.
     */
    @NonNull
    protected static Set<String> certFingerprintsToDecodedString(
            @NonNull JSONArray certFingerprints) {
        Set<String> hashes = new HashSet<>();

        for (int i = 0; i < certFingerprints.length(); i++) {
            try {
                byte[] byteArray = hexStringToByteArray(certFingerprints.get(i).toString());
                String str = Base64.encodeToString(byteArray, DECIMAL);
                hashes.add(str);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return hashes;
    }

    /**
     * This method converts a hex string that is separated by colons into a ByteArray.
     *
     * <p>Example hexString: 4F:69:88:01:...
     */
    @NonNull
    private static byte[] hexStringToByteArray(@NonNull String hexString) {
        String[] hexValues = hexString.split(":");
        byte[] byteArray = new byte[hexValues.length];
        String str;
        int tmp = 0;

        for (int i = 0; i < hexValues.length; ++i) {
            str = hexValues[i];
            tmp = 0;
            tmp = hexValue(str.charAt(0));
            tmp <<= HALF_BYTE;
            tmp |= hexValue(str.charAt(1));
            byteArray[i] = (byte) tmp;
        }

        return byteArray;
    }

    /** Converts a single hex digit into its decimal value. */
    private static int hexValue(char hexChar) {
        int digit = Character.digit(hexChar, HEXADECIMAL);
        if (digit < 0) {
            throw new IllegalArgumentException("Invalid hex char " + hexChar);
        } else {
            return digit;
        }
    }
}
