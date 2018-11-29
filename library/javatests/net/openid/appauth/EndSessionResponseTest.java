package net.openid.appauth;

import android.content.Intent;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=16)
public class EndSessionResponseTest {

    private static final EndSessionRequest TEST_REQUEST =  TestValues.getTestEndSessionRequest();


    @Test(expected = NullPointerException.class)
    public void testEndSessionNew_NullRequest(){
        new EndSessionResponse(null, TEST_REQUEST.state);
    }

    @Test(expected = NullPointerException.class)
    public void testEndSessionNew_NullState(){
        new EndSessionResponse(TEST_REQUEST, null);
    }

    @Test
    public void testIntentSerializeDeserialize(){
        EndSessionResponse endSessionResponse = new EndSessionResponse(TEST_REQUEST, TEST_REQUEST.state);

        Intent endSessionIntent = endSessionResponse.toIntent();

        EndSessionResponse deserializeResponse = EndSessionResponse.fromIntent(endSessionIntent);

        assertThat(deserializeResponse).isNotNull();
        assertThat(deserializeResponse.state).isEqualTo(endSessionResponse.endSessionRequest.state);
        assertThat(deserializeResponse.endSessionRequest.redirectUri)
            .isEqualTo(endSessionResponse.endSessionRequest.redirectUri);
        assertThat(deserializeResponse.endSessionRequest.configuration.endSessionEndpoint)
            .isEqualTo(endSessionResponse.endSessionRequest.configuration.endSessionEndpoint);
        assertThat(deserializeResponse.endSessionRequest.state)
            .isEqualTo(endSessionResponse.endSessionRequest.state);
        assertThat(deserializeResponse.endSessionRequest.idToken)
            .isEqualTo(endSessionResponse.endSessionRequest.idToken);
    }

    @Test
    public void  testIntentSerializeNull(){
        EndSessionResponse deserializeResponse = EndSessionResponse.fromIntent(new Intent());
        assertThat(deserializeResponse).isNull();
    }

    @Test (expected = IllegalArgumentException.class)
    public void  testIntentDeserializeError(){
        Intent intent = new Intent();
        intent.putExtra(EndSessionResponse.EXTRA_RESPONSE, "");
        EndSessionResponse.fromIntent(intent);
    }

    @Test
    public void testJsonSerializeDeserialize() throws JSONException {
        EndSessionResponse endSessionResponse =
            new EndSessionResponse(TEST_REQUEST, TEST_REQUEST.state);

        JSONObject endSessionResponseJson = endSessionResponse.jsonSerialize();

        EndSessionResponse deserializeResponse =
            EndSessionResponse.jsonDeserializeString(endSessionResponseJson);

        assertThat(deserializeResponse).isNotNull();
        assertThat(deserializeResponse.state).isEqualTo(endSessionResponse.state);
        assertThat(deserializeResponse.endSessionRequest.redirectUri)
            .isEqualTo(endSessionResponse.endSessionRequest.redirectUri);
        assertThat(deserializeResponse.endSessionRequest.configuration.endSessionEndpoint)
            .isEqualTo(endSessionResponse.endSessionRequest.configuration.endSessionEndpoint);
        assertThat(deserializeResponse.endSessionRequest.state)
            .isEqualTo(endSessionResponse.endSessionRequest.state);
        assertThat(deserializeResponse.endSessionRequest.idToken)
            .isEqualTo(endSessionResponse.endSessionRequest.idToken);
    }

    @Test
    public void testFromRequestAndUri_Success(){
        EndSessionResponse endSessionResponse =
            new EndSessionResponse(TEST_REQUEST, TEST_REQUEST.state);

        Uri endSessionUri = new Uri.Builder()
            .appendQueryParameter(EndSessionResponse.KEY_STATE, TEST_REQUEST.state)
            .build();

        EndSessionResponse endSessionResponseDeserialized =
            EndSessionResponse.fromRequestAndUri(TEST_REQUEST, endSessionUri);
        assertThat(endSessionResponseDeserialized).isNotNull();
        assertThat(endSessionResponseDeserialized.state).isEqualTo(endSessionResponse.state);
        assertThat(endSessionResponseDeserialized.endSessionRequest.redirectUri)
            .isEqualTo(endSessionResponse.endSessionRequest.redirectUri);
        assertThat(endSessionResponseDeserialized.endSessionRequest.configuration.endSessionEndpoint)
            .isEqualTo(endSessionResponse.endSessionRequest.configuration.endSessionEndpoint);
        assertThat(endSessionResponseDeserialized.endSessionRequest.state)
            .isEqualTo(endSessionResponse.endSessionRequest.state);
        assertThat(endSessionResponseDeserialized.endSessionRequest.idToken)
            .isEqualTo(endSessionResponse.endSessionRequest.idToken);
    }

    @Test
    public void testIntent_containsEndSessionResponse_True() {
        EndSessionResponse endSessionResponse = new EndSessionResponse(TEST_REQUEST, TEST_REQUEST.state);

        Intent endSessionIntent = endSessionResponse.toIntent();

        assertThat(EndSessionResponse.containsEndSessionResoponse(endSessionIntent)).isTrue();
    }

    @Test
    public void testIntent_containsEndSessionResponse_False() {
        Intent intent = new Intent();
        assertThat(EndSessionResponse.containsEndSessionResoponse(intent)).isFalse();
    }

}
