package net.openid.appauth;

import android.support.annotation.Nullable;
import android.util.Base64;

import net.openid.appauth.IdToken.IdTokenException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static net.openid.appauth.TestValues.TEST_NONCE;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=16)
public class IdTokenTest {

    static final String TEST_ISSUER = "https://test.issuer";
    static final String TEST_SUBJECT = "SUBJ3CT";
    static final String TEST_AUDIENCE = "AUDI3NCE";


    @Test
    public void testFrom() throws Exception {
        String serializedIdToken = getUnsignedIdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            TEST_AUDIENCE,
            TEST_NONCE);
        IdToken idToken = IdToken.from(serializedIdToken);
        assertEquals(idToken.issuer, TEST_ISSUER);
        assertEquals(idToken.subject, TEST_SUBJECT);
        assertThat(idToken.audience, contains(TEST_AUDIENCE));
        assertEquals(idToken.nonce, TEST_NONCE);
    }

    @Test(expected = IdTokenException.class)
    public void testFrom_shouldFailOnMissingSection() throws IdTokenException, JSONException {
        IdToken.from("header.");
    }

    @Test(expected = JSONException.class)
    public void testFrom_shouldFailOnMalformedInput() throws IdTokenException, JSONException {
        IdToken.from("header.claims");
    }

    @Test(expected = JSONException.class)
    public void testFrom_shouldFailOnMissingIssuer() throws IdTokenException, JSONException {

        IdToken.from("header.claims");
    }

    private static String base64UrlNoPaddingEncode(byte[] data) {
        return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    static String getUnsignedIdToken(
        @Nullable String issuer,
        @Nullable String subject,
        @Nullable String audience,
        @Nullable String nonce) {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        return getUnsignedIdToken(
            issuer,
            subject,
            audience,
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds,
            nonce);
    }

    static String getUnsignedIdToken(
        @Nullable String issuer,
        @Nullable String subject,
        @Nullable String audience,
        @Nullable Long expiration,
        @Nullable Long issuedAt,
        @Nullable String nonce) {
        JSONObject header = new JSONObject();
        JsonUtil.put(header, "typ", "JWT");

        JSONObject claims = new JSONObject();
        JsonUtil.putIfNotNull(claims, "iss", issuer);
        JsonUtil.putIfNotNull(claims, "sub", subject);
        JsonUtil.putIfNotNull(claims, "aud", audience);
        JsonUtil.putIfNotNull(claims, "exp", expiration != null ? String.valueOf(expiration) : null);
        JsonUtil.putIfNotNull(claims, "iat", issuedAt != null ? String.valueOf(issuedAt) : null);
        JsonUtil.putIfNotNull(claims, "nonce", nonce);


        String encodedHeader = base64UrlNoPaddingEncode(header.toString().getBytes());
        String encodedClaims = base64UrlNoPaddingEncode(claims.toString().getBytes());
        return encodedHeader + "." + encodedClaims;
    }
}
