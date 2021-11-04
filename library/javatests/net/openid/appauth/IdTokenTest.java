package net.openid.appauth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Base64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.openid.appauth.AuthorizationServiceDiscovery.MissingArgumentException;
import net.openid.appauth.IdToken.IdTokenException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static net.openid.appauth.AuthorizationServiceDiscoveryTest.TEST_AUTHORIZATION_ENDPOINT;
import static net.openid.appauth.AuthorizationServiceDiscoveryTest.TEST_CLAIMS_SUPPORTED;
import static net.openid.appauth.AuthorizationServiceDiscoveryTest.TEST_END_SESSION_ENDPOINT;
import static net.openid.appauth.AuthorizationServiceDiscoveryTest.TEST_ID_TOKEN_SIGNING_ALG_VALUES;
import static net.openid.appauth.AuthorizationServiceDiscoveryTest.TEST_JWKS_URI;
import static net.openid.appauth.AuthorizationServiceDiscoveryTest.TEST_REGISTRATION_ENDPOINT;
import static net.openid.appauth.AuthorizationServiceDiscoveryTest.TEST_RESPONSE_TYPES_SUPPORTED;
import static net.openid.appauth.AuthorizationServiceDiscoveryTest.TEST_SCOPES_SUPPORTED;
import static net.openid.appauth.AuthorizationServiceDiscoveryTest.TEST_SUBJECT_TYPES_SUPPORTED;
import static net.openid.appauth.AuthorizationServiceDiscoveryTest.TEST_TOKEN_ENDPOINT;
import static net.openid.appauth.AuthorizationServiceDiscoveryTest.TEST_TOKEN_ENDPOINT_AUTH_METHODS;
import static net.openid.appauth.AuthorizationServiceDiscoveryTest.TEST_USERINFO_ENDPOINT;
import static net.openid.appauth.TestValues.TEST_APP_REDIRECT_URI;
import static net.openid.appauth.TestValues.TEST_AUTH_CODE;
import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.TEST_CODE_VERIFIER;
import static net.openid.appauth.TestValues.TEST_ISSUER;
import static net.openid.appauth.TestValues.TEST_NONCE;
import static net.openid.appauth.TestValues.getDiscoveryDocumentJson;
import static net.openid.appauth.TestValues.getTestAuthCodeExchangeRequest;
import static net.openid.appauth.TestValues.getTestAuthCodeExchangeRequestBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;


@RunWith(RobolectricTestRunner.class)
@Config(sdk=16)
public class IdTokenTest {

    static final String TEST_SUBJECT = "SUBJ3CT";
    static final String TEST_AUDIENCE = "AUDI3NCE";


    @Test
    public void testFrom() throws Exception {
        String testToken = getUnsignedIdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            TEST_AUDIENCE,
            TEST_NONCE
        );
        IdToken idToken = IdToken.from(testToken);
        assertEquals(TEST_ISSUER, idToken.issuer);
        assertEquals(TEST_SUBJECT, idToken.subject);
        assertThat(idToken.audience, contains(TEST_AUDIENCE));
        assertEquals(TEST_NONCE, idToken.nonce);
    }

    @Test
    public void testFrom_withAdditionalClaims() throws Exception {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);

        Map<String, Object> additionalClaims = new HashMap<>();
        additionalClaims.put("claim1", "value1");
        additionalClaims.put("claim2", Arrays.asList("value2", "value3"));

        String testToken = getUnsignedIdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            TEST_AUDIENCE,
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds,
            TEST_NONCE,
            additionalClaims
        );

        IdToken idToken = IdToken.from(testToken);
        assertEquals("value1", idToken.additionalClaims.get("claim1"));
        assertEquals("value2", ((ArrayList<String>)idToken.additionalClaims.get("claim2")).get(0));
    }

    @Test
    public void testFrom_shouldParseAudienceList() throws Exception {
        List<String> audienceList = Arrays.asList(TEST_AUDIENCE, "AUDI3NCE2");
        String testToken = getUnsignedIdTokenWithAudienceList(
            TEST_ISSUER,
            TEST_SUBJECT,
            audienceList,
            TEST_NONCE
        );
        IdToken idToken = IdToken.from(testToken);
        assertEquals(TEST_ISSUER, idToken.issuer);
        assertEquals(TEST_SUBJECT, idToken.subject);
        assertEquals(audienceList, idToken.audience);
        assertEquals(TEST_NONCE, idToken.nonce);
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
        String testToken = getUnsignedIdToken(
            null,
            TEST_SUBJECT,
            TEST_AUDIENCE,
            TEST_NONCE
        );
        IdToken.from(testToken);
    }

    @Test(expected = JSONException.class)
    public void testFrom_shouldFailOnMissingSubject() throws IdTokenException, JSONException {
        String testToken = getUnsignedIdToken(
            TEST_ISSUER,
            null,
            TEST_AUDIENCE,
            TEST_NONCE
        );
        IdToken.from(testToken);
    }

    @Test(expected = JSONException.class)
    public void testFrom_shouldFailOnMissingAudience() throws IdTokenException, JSONException {
        String testToken = getUnsignedIdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            null,
            TEST_NONCE
        );
        IdToken.from(testToken);
    }

    @Test(expected = JSONException.class)
    public void testFrom_shouldFailOnMissingExpiration() throws IdTokenException, JSONException {
        String testToken = getUnsignedIdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            TEST_AUDIENCE,
            null,
            0L,
            TEST_NONCE
        );
        IdToken.from(testToken);
    }

    @Test(expected = JSONException.class)
    public void testFrom_shouldFailOnMissingIssuedAt() throws IdTokenException, JSONException {
        String testToken = getUnsignedIdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            TEST_AUDIENCE,
            0L,
            null,
            TEST_NONCE
        );
        IdToken.from(testToken);
    }

    @Test
    public void testValidate() throws AuthorizationException {
        IdToken idToken = getValidIdToken();
        TokenRequest tokenRequest = getAuthCodeExchangeRequestWithNonce();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    @Test
    public void testValidate_withoutNonce() throws AuthorizationException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        IdToken idToken = new IdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            Collections.singletonList(TEST_CLIENT_ID),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds
        );
        TokenRequest tokenRequest = getTestAuthCodeExchangeRequestBuilder().build();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    @Test(expected = AuthorizationException.class)
    public void testValidate_shouldFailOnIssuerMismatch() throws AuthorizationException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        IdToken idToken = new IdToken(
            "https://other.issuer",
            TEST_SUBJECT,
            Collections.singletonList(TEST_CLIENT_ID),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds
        );
        TokenRequest tokenRequest = getAuthCodeExchangeRequestWithNonce();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    @Test(expected = AuthorizationException.class)
    public void testValidate_shouldFailOnNonHttpsIssuer()
        throws AuthorizationException, JSONException, MissingArgumentException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        IdToken idToken = new IdToken(
            "http://other.issuer",
            TEST_SUBJECT,
            Collections.singletonList(TEST_CLIENT_ID),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds
        );

        String serviceDocJsonWithOtherIssuer = getDiscoveryDocJsonWithIssuer("http://other.issuer");
        AuthorizationServiceDiscovery discoveryDoc = new AuthorizationServiceDiscovery(
            new JSONObject(serviceDocJsonWithOtherIssuer));
        AuthorizationServiceConfiguration serviceConfiguration =
            new AuthorizationServiceConfiguration(discoveryDoc);
        TokenRequest tokenRequest = new TokenRequest.Builder(serviceConfiguration, TEST_CLIENT_ID)
            .setAuthorizationCode(TEST_AUTH_CODE)
            .setCodeVerifier(TEST_CODE_VERIFIER)
            .setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
            .setRedirectUri(TEST_APP_REDIRECT_URI)
            .build();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    @Test
    public void testValidate_shouldSkipNonHttpsIssuer()
        throws AuthorizationException, JSONException, MissingArgumentException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        IdToken idToken = new IdToken(
            "http://other.issuer",
            TEST_SUBJECT,
            Collections.singletonList(TEST_CLIENT_ID),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds
        );

        String serviceDocJsonWithOtherIssuer = getDiscoveryDocJsonWithIssuer("http://other.issuer");
        AuthorizationServiceDiscovery discoveryDoc = new AuthorizationServiceDiscovery(
            new JSONObject(serviceDocJsonWithOtherIssuer));
        AuthorizationServiceConfiguration serviceConfiguration =
            new AuthorizationServiceConfiguration(discoveryDoc);
        TokenRequest tokenRequest = new TokenRequest.Builder(serviceConfiguration, TEST_CLIENT_ID)
            .setAuthorizationCode(TEST_AUTH_CODE)
            .setCodeVerifier(TEST_CODE_VERIFIER)
            .setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
            .setRedirectUri(TEST_APP_REDIRECT_URI)
            .build();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock, true);
    }

    @Test(expected = AuthorizationException.class)
    public void testValidate_shouldFailOnIssuerMissingHost()
        throws AuthorizationException, JSONException, MissingArgumentException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        IdToken idToken = new IdToken(
            "https://",
            TEST_SUBJECT,
            Collections.singletonList(TEST_CLIENT_ID),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds
        );

        String serviceDocJsonWithIssuerMissingHost = getDiscoveryDocJsonWithIssuer("https://");
        AuthorizationServiceDiscovery discoveryDoc = new AuthorizationServiceDiscovery(
            new JSONObject(serviceDocJsonWithIssuerMissingHost));
        AuthorizationServiceConfiguration serviceConfiguration =
            new AuthorizationServiceConfiguration(discoveryDoc);
        TokenRequest tokenRequest = new TokenRequest.Builder(serviceConfiguration, TEST_CLIENT_ID)
            .setAuthorizationCode(TEST_AUTH_CODE)
            .setCodeVerifier(TEST_CODE_VERIFIER)
            .setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
            .setRedirectUri(TEST_APP_REDIRECT_URI)
            .build();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    @Test(expected = AuthorizationException.class)
    public void testValidate_shouldFailOnIssuerWithQueryParam()
        throws AuthorizationException, JSONException, MissingArgumentException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        IdToken idToken = new IdToken(
            "https://some.issuer?param=value",
            TEST_SUBJECT,
            Collections.singletonList(TEST_CLIENT_ID),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds
        );

        String serviceDocJsonWithIssuerMissingHost = getDiscoveryDocJsonWithIssuer(
            "https://some.issuer?param=value");
        AuthorizationServiceDiscovery discoveryDoc = new AuthorizationServiceDiscovery(
            new JSONObject(serviceDocJsonWithIssuerMissingHost));
        AuthorizationServiceConfiguration serviceConfiguration =
            new AuthorizationServiceConfiguration(discoveryDoc);
        TokenRequest tokenRequest = new TokenRequest.Builder(serviceConfiguration, TEST_CLIENT_ID)
            .setAuthorizationCode(TEST_AUTH_CODE)
            .setCodeVerifier(TEST_CODE_VERIFIER)
            .setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
            .setRedirectUri(TEST_APP_REDIRECT_URI)
            .build();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    @Test(expected = AuthorizationException.class)
    public void testValidate_shouldFailOnIssuerWithFragment()
        throws AuthorizationException, JSONException, MissingArgumentException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        IdToken idToken = new IdToken(
            "https://some.issuer/#/fragment",
            TEST_SUBJECT,
            Collections.singletonList(TEST_CLIENT_ID),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds
        );

        String serviceDocJsonWithIssuerMissingHost = getDiscoveryDocJsonWithIssuer(
            "https://some.issuer/#/fragment");
        AuthorizationServiceDiscovery discoveryDoc = new AuthorizationServiceDiscovery(
            new JSONObject(serviceDocJsonWithIssuerMissingHost));
        AuthorizationServiceConfiguration serviceConfiguration =
            new AuthorizationServiceConfiguration(discoveryDoc);
        TokenRequest tokenRequest = new TokenRequest.Builder(serviceConfiguration, TEST_CLIENT_ID)
            .setAuthorizationCode(TEST_AUTH_CODE)
            .setCodeVerifier(TEST_CODE_VERIFIER)
            .setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
            .setRedirectUri(TEST_APP_REDIRECT_URI)
            .build();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    @Test
    public void testValidate_audienceMatch() throws AuthorizationException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        IdToken idToken = new IdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            Collections.singletonList(TEST_CLIENT_ID),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds
        );
        TokenRequest tokenRequest = getTestAuthCodeExchangeRequest();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    @Test(expected = AuthorizationException.class)
    public void testValidate_shouldFailOnAudienceMismatch() throws AuthorizationException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        IdToken idToken = new IdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            Collections.singletonList("some_other_audience"),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds
        );
        TokenRequest tokenRequest = getAuthCodeExchangeRequestWithNonce();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    @Test
    public void testValidate_authorizedPartyMatch() throws AuthorizationException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        IdToken idToken = new IdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            Collections.singletonList("some_other_audience"),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds,
            TEST_NONCE,
            TEST_CLIENT_ID
        );
        TokenRequest tokenRequest = getAuthCodeExchangeRequestWithNonce();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    @Test(expected = AuthorizationException.class)
    public void testValidate_shouldFailOnAudienceAndAuthorizedPartyMismatch()
            throws AuthorizationException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        Map<String, Object> additionalClaims = new HashMap<>();
        IdToken idToken = new IdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            Collections.singletonList("some_other_audience"),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds,
            TEST_NONCE,
            "some_other_party",
            additionalClaims
        );
        TokenRequest tokenRequest = getAuthCodeExchangeRequestWithNonce();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    @Test(expected = AuthorizationException.class)
    public void testValidate_shouldFailOnExpiredToken() throws AuthorizationException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        IdToken idToken = new IdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            Collections.singletonList(TEST_CLIENT_ID),
            nowInSeconds - tenMinutesInSeconds,
            nowInSeconds
        );
        TokenRequest tokenRequest = getAuthCodeExchangeRequestWithNonce();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    @Test(expected = AuthorizationException.class)
    public void testValidate_shouldFailOnIssuedAtOverTenMinutesAgo() throws AuthorizationException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        IdToken idToken = new IdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            Collections.singletonList(TEST_CLIENT_ID),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds - (tenMinutesInSeconds * 2)
        );
        TokenRequest tokenRequest = getAuthCodeExchangeRequestWithNonce();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    @Test(expected = AuthorizationException.class)
    public void testValidate_shouldFailOnNonceMismatch() throws AuthorizationException {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        IdToken idToken = new IdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            Collections.singletonList(TEST_CLIENT_ID),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds,
            "some_other_nonce",
            null
        );
        TokenRequest tokenRequest = getAuthCodeExchangeRequestWithNonce();
        Clock clock = SystemClock.INSTANCE;
        idToken.validate(tokenRequest, clock);
    }

    private static String base64UrlNoPaddingEncode(byte[] data) {
        return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    private String getDiscoveryDocJsonWithIssuer(String issuer) {
        return getDiscoveryDocumentJson(
            issuer,
            TEST_AUTHORIZATION_ENDPOINT,
            TEST_TOKEN_ENDPOINT,
            TEST_USERINFO_ENDPOINT,
            TEST_REGISTRATION_ENDPOINT,
            TEST_END_SESSION_ENDPOINT,
            TEST_JWKS_URI,
            TEST_RESPONSE_TYPES_SUPPORTED,
            TEST_SUBJECT_TYPES_SUPPORTED,
            TEST_ID_TOKEN_SIGNING_ALG_VALUES,
            TEST_SCOPES_SUPPORTED,
            TEST_TOKEN_ENDPOINT_AUTH_METHODS,
            TEST_CLAIMS_SUPPORTED
        );
    }

    private TokenRequest getAuthCodeExchangeRequestWithNonce() {
        return getTestAuthCodeExchangeRequestBuilder()
            .setNonce(TEST_NONCE)
            .build();
    }

    private static IdToken getValidIdToken() {
        Long nowInSeconds = SystemClock.INSTANCE.getCurrentTimeMillis() / 1000;
        Long tenMinutesInSeconds = (long) (10 * 60);
        Map<String, Object> additionalClaims = new HashMap<>();
        return new IdToken(
            TEST_ISSUER,
            TEST_SUBJECT,
            Collections.singletonList(TEST_CLIENT_ID),
            nowInSeconds + tenMinutesInSeconds,
            nowInSeconds,
            TEST_NONCE,
            TEST_CLIENT_ID,
            additionalClaims
        );
    }

    static String getUnsignedIdTokenWithAudienceList(
        @Nullable String issuer,
        @Nullable String subject,
        @Nullable List<String> audience,
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
        @Nullable List<String> audience,
        @Nullable Long expiration,
        @Nullable Long issuedAt,
        @Nullable String nonce) {
        JSONObject header = new JSONObject();
        JsonUtil.put(header, "typ", "JWT");

        JSONObject claims = new JSONObject();
        JsonUtil.putIfNotNull(claims, "iss", issuer);
        JsonUtil.putIfNotNull(claims, "sub", subject);
        JsonUtil.put(claims, "aud", new JSONArray(audience));
        JsonUtil.putIfNotNull(claims, "exp", expiration != null ? String.valueOf(expiration) : null);
        JsonUtil.putIfNotNull(claims, "iat", issuedAt != null ? String.valueOf(issuedAt) : null);
        JsonUtil.putIfNotNull(claims, "nonce", nonce);


        String encodedHeader = base64UrlNoPaddingEncode(header.toString().getBytes());
        String encodedClaims = base64UrlNoPaddingEncode(claims.toString().getBytes());
        return encodedHeader + "." + encodedClaims;
    }

    static String getUnsignedIdToken(
        @Nullable String issuer,
        @Nullable String subject,
        @Nullable String audience,
        @Nullable Long expiration,
        @Nullable Long issuedAt,
        @Nullable String nonce) {
        return getUnsignedIdToken(issuer, subject, audience, expiration, issuedAt, nonce, Collections.emptyMap());
    }

    static String getUnsignedIdToken(
        @Nullable String issuer,
        @Nullable String subject,
        @Nullable String audience,
        @Nullable Long expiration,
        @Nullable Long issuedAt,
        @Nullable String nonce,
        @NonNull Map<String, Object> additionalClaims) {
        JSONObject header = new JSONObject();
        JsonUtil.put(header, "typ", "JWT");

        JSONObject claims = new JSONObject();
        JsonUtil.putIfNotNull(claims, "iss", issuer);
        JsonUtil.putIfNotNull(claims, "sub", subject);
        JsonUtil.putIfNotNull(claims, "aud", audience);
        JsonUtil.putIfNotNull(claims, "exp", expiration != null ? String.valueOf(expiration) : null);
        JsonUtil.putIfNotNull(claims, "iat", issuedAt != null ? String.valueOf(issuedAt) : null);
        JsonUtil.putIfNotNull(claims, "nonce", nonce);

        for (String key: additionalClaims.keySet()) {
            JsonUtil.putIfNotNull(claims, key, additionalClaims.get(key));
        }

        String encodedHeader = base64UrlNoPaddingEncode(header.toString().getBytes());
        String encodedClaims = base64UrlNoPaddingEncode(claims.toString().getBytes());
        return encodedHeader + "." + encodedClaims;
    }
}
