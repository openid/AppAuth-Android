package net.openid.appauth;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.Null;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import static net.openid.appauth.TestValues.TEST_APP_REDIRECT_URI;
import static net.openid.appauth.TestValues.TEST_AUTH_CODE;
import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.TEST_CLIENT_SECRET;
import static net.openid.appauth.TestValues.getTestServiceConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ClientSecretPostTest {

    private OutputStream mOutputStream;
    @Mock
    HttpURLConnection mHttpConnection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mOutputStream = new ByteArrayOutputStream();
        when(mHttpConnection.getOutputStream()).thenReturn(mOutputStream);
    }

    @Test
    public void testSetupRequestParameters() throws Exception {
        ClientSecretPost csp = new ClientSecretPost(TEST_CLIENT_SECRET);

        Map<String, String> additionalParams = csp.setupRequestParameters(TEST_CLIENT_ID, mHttpConnection);
        Map<String, String> expectedAdditionalParameters = new HashMap<>();
        expectedAdditionalParameters.put(ClientSecretPost.PARAM_CLIENT_ID, TEST_CLIENT_ID);
        expectedAdditionalParameters.put(ClientSecretPost.PARAM_CLIENT_SECRET, TEST_CLIENT_SECRET);
        assertThat(additionalParams).isEqualTo(expectedAdditionalParameters);
    }

    @Test
    public void testSetupRequestParameters_shouldIncludeAdditionalParameters() throws Exception {
        ClientSecretPost csp = new ClientSecretPost(TEST_CLIENT_SECRET);

        TokenRequest request = new TokenRequest.Builder(getTestServiceConfig(), TEST_CLIENT_ID)
                .setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
                .setAuthorizationCode(TEST_AUTH_CODE)
                .setRedirectUri(TEST_APP_REDIRECT_URI)
                .build();

        csp.apply(request, mHttpConnection);

        Uri postBody = new Uri.Builder().encodedQuery(mOutputStream.toString()).build();
        // Client authentication parameters
        assertThat(postBody.getQueryParameter(ClientSecretPost.PARAM_CLIENT_ID)).isEqualTo(TEST_CLIENT_ID);
        assertThat(postBody.getQueryParameter(ClientSecretPost.PARAM_CLIENT_SECRET)).isEqualTo(TEST_CLIENT_SECRET);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_withNull() {
        new ClientSecretPost(null);
    }
}