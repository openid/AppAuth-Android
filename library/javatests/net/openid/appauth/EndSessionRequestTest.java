package net.openid.appauth;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=16)
public class EndSessionRequestTest {


    @Test(expected = NullPointerException.class)
    public void buildWithNulConfiguration_NullPointerException() {
        new EndSessionRequest(null, TestValues.TEST_ID_TOKEN, TestValues.TEST_APP_REDIRECT_URI);
    }

    @Test(expected = NullPointerException.class)
    public void buildWithNulIdToken_NullPointerException() {
        new EndSessionRequest(TestValues.getTestServiceConfig(), null, TestValues.TEST_APP_REDIRECT_URI);
    }

    @Test(expected = NullPointerException.class)
    public void buildWithNullRedirectUri_NullPointerException() {
        new EndSessionRequest(TestValues.getTestServiceConfig(), TestValues.TEST_ID_TOKEN, null);
    }

    @Test
    public void getState_notNull() {
        EndSessionRequest request = new EndSessionRequest(TestValues.getTestServiceConfig(), TestValues.TEST_ID_TOKEN, TestValues.TEST_APP_REDIRECT_URI);
        assertNotNull(request.getState());
    }

    @Test
    public void testToUri() {
        EndSessionRequest request = TestValues.getTestEndSessionRequest();
        Uri requestUri = request.toUri();

        assertThat(requestUri.getQueryParameter(EndSessionRequest.KEY_ID_TOKEN_HINT))
            .isEqualTo(request.idToken);
        assertThat(requestUri.getQueryParameter(EndSessionRequest.KEY_REDIECT_URI))
            .isEqualTo(request.redirectUri.toString());
        assertThat(requestUri.getQueryParameter(EndSessionRequest.KEY_STATE))
            .isEqualTo(request.state);

    }

    @Test
    public void testJsonSerialize() throws Exception {
        EndSessionRequest resquest = TestValues.getTestEndSessionRequest();
        EndSessionRequest copy = serializeDeserialize(resquest);
        assertThat(copy.idToken).isEqualTo(TestValues.TEST_ID_TOKEN);
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
