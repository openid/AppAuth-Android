package net.openid.appauth;

import android.util.Base64;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.HttpURLConnection;

import static net.openid.appauth.TestValues.TEST_CLIENT_ID;
import static net.openid.appauth.TestValues.TEST_CLIENT_SECRET;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ClientSecretBasicTest {

    @Mock
    HttpURLConnection mHttpConnection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSetupRequestParameters() throws Exception {
        ClientSecretBasic csb = new ClientSecretBasic(TEST_CLIENT_SECRET);

        csb.setupRequestParameters(TEST_CLIENT_ID, mHttpConnection);

        String credentials = TEST_CLIENT_ID + ":" + TEST_CLIENT_SECRET;
        String expectedAuthzHeader = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        verify(mHttpConnection).setRequestProperty("Authorization", expectedAuthzHeader);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_withNull() {
        new ClientSecretBasic(null);
    }
}