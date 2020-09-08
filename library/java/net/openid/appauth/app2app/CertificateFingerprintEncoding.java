package net.openid.appauth.app2app;

import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;
import java.util.Set;

final class CertificateFingerprintEncoding {

    private CertificateFingerprintEncoding() {
    }

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
                String str = Base64.encodeToString(byteArray, 10);
                hashes.add(str);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return hashes;
    }

    /**
     * This method converts a hex string that is separated by colons into a ByteArray.
     * <p>
     * Example hexString: 4F:69:88:01:...
     */
    @NonNull
    private static byte[] hexStringToByteArray(@NonNull String hexString) {
        String[] hexValues = hexString.split(":");
        byte[] byteArray = new byte[hexValues.length];
        String str;
        int b = 0;

        for (int i = 0; i < hexValues.length; ++i) {
            str = hexValues[i];
            b = 0;
            b = hexValue(str.charAt(0));
            b <<= 4;
            b |= hexValue(str.charAt(1));
            byteArray[i] = (byte) b;
        }

        return byteArray;
    }

    /**
     * Converts a single hex digit into its decimal value.
     */
    private static int hexValue(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if (digit < 0) {
            throw new IllegalArgumentException("Invalid hex char " + hexChar);
        } else {
            return digit;
        }
    }
}
