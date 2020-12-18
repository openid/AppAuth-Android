package net.openid.appauth.connectivity.ok;

import android.net.Uri;

import net.openid.appauth.BuildConfig;
import net.openid.appauth.connectivity.ConnectionBuilder;
import net.openid.appauth.connectivity.HttpConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16)
public class OkHttpConnectionImplTest {
    private static final String REQUEST_JSON = "{\"value\":\"some json request\"}";
    private static final String RESPONSE_JSON = "{\"value\":\"some json answer\"}";
    private static final String JSON_MIME_TYPE = "application/json";

    private MockWebServer mMockServer;
    private OkHttpClient mClient;
    private ConnectionBuilder mhttpConnectionBuilder;

    @Before
    public void setUp() throws Exception {
        mMockServer = new MockWebServer();
        mMockServer.start();
        mClient = new OkHttpClient();
        mhttpConnectionBuilder = new OkConnectionBuilder(mClient);
    }

    @After
    public void tearDown() throws Exception {
        mMockServer.shutdown();
    }

    @Test
    public void testGetRequest() throws Exception {
        mMockServer.enqueue(new MockResponse().setBody(RESPONSE_JSON));
        HttpUrl baseUrl = mMockServer.url("/some/verify");
        HttpConnection conn = mhttpConnectionBuilder.openConnection(Uri.parse(baseUrl.toString()));
        assertEquals(200, conn.getResponseCode());
        InputStream in = conn.getInputStream();
        assertEquals(RESPONSE_JSON, getStringFromIS(in));
    }

    @Test
    public void testPostRequest() throws Exception {
        mMockServer.enqueue(new MockResponse().setBody(RESPONSE_JSON));
        HttpUrl baseUrl = mMockServer.url("/some/post");
        HttpConnection conn = mhttpConnectionBuilder.openConnection(Uri.parse(baseUrl.toString()));
        conn.setRequestMethod("POST");
        conn.setRequestData(JSON_MIME_TYPE, REQUEST_JSON);
        InputStream in = conn.getInputStream();
        RecordedRequest request = mMockServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals(RESPONSE_JSON, getStringFromIS(in));
    }

    @Test
    public void testRequestHeaders() throws Exception {
        mMockServer.enqueue(new MockResponse().setBody(RESPONSE_JSON));
        HttpUrl baseUrl = mMockServer.url("/some/post");
        HttpConnection conn = mhttpConnectionBuilder.openConnection(Uri.parse(baseUrl.toString()));
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.connect();
        RecordedRequest request = mMockServer.takeRequest();
        assertEquals("application/x-www-form-urlencoded", request.getHeader("Content-Type"));
    }

    private String getStringFromIS(InputStream is) throws IOException {
        StringBuilder buffer = new StringBuilder();
        for (int value; (value = is.read()) != -1; ) {
            buffer.append((char) value);
        }
        return buffer.toString();
    }
}
