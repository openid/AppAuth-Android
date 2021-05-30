package net.openid.appauth;

import static net.openid.appauth.TestValues.TEST_APP_REDIRECT_URI;
import static net.openid.appauth.TestValues.TEST_ID_TOKEN;
import static net.openid.appauth.TestValues.getTestServiceConfig;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class EndSessionRequestTest {

    private EndSessionRequest.Builder mRequestBuilder;

    @Before
    public void setUp() {
        mRequestBuilder = new EndSessionRequest.Builder(
            getTestServiceConfig(),
            TEST_ID_TOKEN,
            TEST_APP_REDIRECT_URI);
    }

    /* ********************************** Builder() ***********************************************/
    @Test(expected = NullPointerException.class)
    public void testBuilder_nullConfiguration() {
        new EndSessionRequest.Builder(
            null,
            TEST_ID_TOKEN,
            TEST_APP_REDIRECT_URI);
    }

    @Test(expected = NullPointerException.class)
    public void testBuilder_nullIdToken() {
        new EndSessionRequest.Builder(
            getTestServiceConfig(),
            null,
            TEST_APP_REDIRECT_URI);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_emptyIdToken() {
        new EndSessionRequest.Builder(
            getTestServiceConfig(),
            "",
            TEST_APP_REDIRECT_URI);
    }

    @Test(expected = NullPointerException.class)
    public void testBuilder_nullRedirectUri() {
        new EndSessionRequest.Builder(
            getTestServiceConfig(),
            TEST_ID_TOKEN,
            null);
    }

    @Test
    public void testState_notNull() {
        EndSessionRequest request = new EndSessionRequest.Builder(
            getTestServiceConfig(),
            TEST_ID_TOKEN,
            TEST_APP_REDIRECT_URI).build();
        assertNotNull(request.getState());
    }

    /* ******************************** ui_locales ***********************************************/

    @Test
    public void testUiLocales_unspecified() {
        EndSessionRequest request = mRequestBuilder.build();
        assertThat(request.uiLocales).isNull();
        assertThat(request.getUiLocales()).isNull();
    }

    @Test
    public void testUiLocales() {
        EndSessionRequest req = mRequestBuilder
            .setUiLocales("en de fr-CA")
            .build();

        assertThat(req.uiLocales).isEqualTo("en de fr-CA");
        assertThat(req.getUiLocales())
            .hasSize(3)
            .contains("en")
            .contains("de")
            .contains("fr-CA");
    }

    @Test
    public void testUiLocales_nullValue() {
        EndSessionRequest req = mRequestBuilder.setUiLocales(null).build();
        assertThat(req.uiLocales).isNull();
        assertThat(req.getUiLocales()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUiLocales_empty() {
        mRequestBuilder.setUiLocales("").build();
    }

    @Test
    public void testUiLocales_withVarargs() {
        EndSessionRequest req = mRequestBuilder
            .setUiLocalesValues("en", "de", "fr-CA")
            .build();

        assertThat(req.uiLocales).isEqualTo("en de fr-CA");
        assertThat(req.getUiLocales())
            .hasSize(3)
            .contains("en")
            .contains("de")
            .contains("fr-CA");
    }

    @Test
    public void testUiLocales_withNullVarargsArray() {
        EndSessionRequest req = mRequestBuilder.setUiLocalesValues((String[])null).build();
        assertThat(req.uiLocales).isNull();
        assertThat(req.getUiLocales()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUiLocales_withNullStringInVarargs() {
        mRequestBuilder.setUiLocalesValues("en", null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUiLocales_withEmptyStringInVarargs() {
        mRequestBuilder.setUiLocalesValues("en", "").build();
    }

    @Test
    public void testUiLocales_withIterable() {
        EndSessionRequest req = mRequestBuilder
            .setUiLocalesValues(Arrays.asList("en", "de", "fr-CA"))
            .build();

        assertThat(req.uiLocales).isEqualTo("en de fr-CA");

        assertThat(req.getUiLocales())
            .hasSize(3)
            .contains("en")
            .contains("de")
            .contains("fr-CA");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUiLocales_withIterableContainingNullValue() {
        mRequestBuilder
            .setUiLocalesValues(Arrays.asList("en", null))
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUiLocales_withIterableContainingEmptyValue() {
        mRequestBuilder
            .setUiLocalesValues(Arrays.asList("en", ""))
            .build();
    }

    /* ******************************* toUri() ****************************************************/

    @Test
    public void testToUri() {
        EndSessionRequest request = TestValues.getTestEndSessionRequest();
        Uri requestUri = request.toUri();

        assertThat(requestUri.getQueryParameter(EndSessionRequest.KEY_ID_TOKEN_HINT))
            .isEqualTo(request.idToken);
        assertThat(requestUri.getQueryParameter(EndSessionRequest.KEY_REDIRECT_URI))
            .isEqualTo(request.redirectUri.toString());
        assertThat(requestUri.getQueryParameter(EndSessionRequest.KEY_STATE))
            .isEqualTo(request.state);

    }

    @Test
    public void testJsonSerialize() throws Exception {
        EndSessionRequest resquest = TestValues.getTestEndSessionRequest();
        EndSessionRequest copy = serializeDeserialize(resquest);
        assertThat(copy.idToken).isEqualTo(TEST_ID_TOKEN);
        assertThat(copy.state).isEqualTo(resquest.state);
        assertThat(copy.redirectUri).isEqualTo(resquest.redirectUri);
    }

    @Test
    public void testIsEndSessionRequestSuccess() {
        JSONObject json = TestValues.getTestEndSessionRequest().jsonSerialize();
        assertTrue(EndSessionRequest.isEndSessionRequest(json));
    }

    @Test
    public void testIsEndSessionRequestFailure() {
        JSONObject json = TestValues.getTestAuthRequestBuilder().build().jsonSerialize();
        assertFalse(EndSessionRequest.isEndSessionRequest(json));
    }

    private EndSessionRequest serializeDeserialize(EndSessionRequest request)
        throws JSONException {
        return EndSessionRequest.jsonDeserialize(request.jsonSerializeString());
    }

}
