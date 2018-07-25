package net.openid.appauth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=16)
public class IDTokenTest {

    static final String TEST_ID_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjFlOWdkazcifQ.ewogIml"
    + "zcyI6ICJodHRwOi8vc2VydmVyLmV4YW1wbGUuY29tIiwKICJzdWIiOiAiMjQ4Mjg5NzYxMDAxIiwKICJhdWQiOiAiczZ"
    + "CaGRSa3F0MyIsCiAibm9uY2UiOiAibi0wUzZfV3pBMk1qIiwKICJleHAiOiAxMzExMjgxOTcwLAogImlhdCI6IDEzMTE"
    + "yODA5NzAKfQ.ggW8hZ1EuVLuxNuuIJKX_V8a_OMXzR0EHR9R6jgdqrOOF4daGU96Sr_P6qJp6IcmD3HP99Obi1PRs-cw"
    + "h3LO-p146waJ8IhehcwL7F09JdijmBqkvPeB2T9CJ NqeGpe-gccMg4vfKjkM8FcGvnzZUN4_KSP0aAp1tOJ1zZwgjxq"
    + "GByKHiOtX7TpdQyHE5lcMiKPXfEIQILVq0pc_E2DzL7emopWoaoZTF_m0_N0YzFC6g6EJbOEoRoSK5hoDalrcvRYLSrQ"
    + "AZZKflyuVCyixEoV9GfNQC3_osjzw2PAithfubEEBLuVVk4XUVrWOLrLl0nx7RkKU8NXNHq-rvKMzqg";
    static final String TEST_SUBJECT = "SUBJ3CT";


    @Test
    public void testFrom() throws Exception {
        IDToken idToken = IDToken.from(TEST_ID_TOKEN);
    }

    @Test(expected = IDToken.IDTokenException.class)
    public void testFrom_shouldFailOnMissingSection() throws IDToken.IDTokenException, JSONException {
        IDToken.from("header.");
    }

    @Test(expected = JSONException.class)
    public void testFrom_shouldFailOnMalformedInput() throws IDToken.IDTokenException, JSONException {
        IDToken.from("header.claims");
    }

    @Test(expected = JSONException.class)
    public void testFrom_shouldFailOnMissingIssuer() throws IDToken.IDTokenException, JSONException {

        IDToken.from("header.claims");
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
