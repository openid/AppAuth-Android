package net.openid.appauth.app2app;

import net.openid.appauth.BuildConfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16)
public class CertificateFingerprintEncodingTest {

    @Test
    public void testCertFingerprintsToDecodedString0() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put("98:C7:E1:43:9C:A9:C9:68:27:FE:47:16:9A:C0:60:2A:61:5B:88:2F:CC:4E:AB:66:47:8E:67:E6:2A:93:F8:68");

        Set<String> hashes = CertificateFingerprintEncoding.certFingerprintsToDecodedString(jsonArray);

        assertThat(hashes.size()).isEqualTo(1);
        assertThat(hashes.contains("mMfhQ5ypyWgn_kcWmsBgKmFbiC_MTqtmR45n5iqT-Gg=")).isTrue();
    }

    @Test
    public void testCertFingerprintsToDecodedString1() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put("58:27:63:4A:F5:D5:07:7C:DE:4B:94:27:60:B0:C7:CD:33:8D:93:13:02:8D:0B:E0:0F:C5:26:F4:88:39:F1:D5");

        Set<String> hashes = CertificateFingerprintEncoding.certFingerprintsToDecodedString(jsonArray);

        assertThat(hashes.size()).isEqualTo(1);
        assertThat(hashes.contains("WCdjSvXVB3zeS5QnYLDHzTONkxMCjQvgD8Um9Ig58dU=")).isTrue();
    }
}
