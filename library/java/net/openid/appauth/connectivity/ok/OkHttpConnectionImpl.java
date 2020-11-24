package net.openid.appauth.connectivity.ok;

import android.net.Uri;

import net.openid.appauth.connectivity.HttpConnection;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpConnectionImpl implements HttpConnection {

    private final Request.Builder requestBuilder = new Request.Builder();
    private final OkHttpClient.Builder httpClientBuilder;
    private final Headers.Builder headersBuilder = new Headers.Builder();
    private String method = "GET";
    private RequestBody requestBody;
    private Response response = null;

    public OkHttpConnectionImpl(OkHttpClient client, Uri uri) {
        httpClientBuilder = client.newBuilder();
        requestBuilder.url(uri.toString());
    }

    @Override
    public void setRequestMethod(String method) {
        this.method = method;
    }

    @Override
    public void setDoInput(boolean doInput) {
        //STUP
    }

    @Override
    public void setDoOutput(boolean doOutput) {
        //STUP
    }

    @Override
    public void setRequestProperty(String key, String value) {
        headersBuilder.set(key, value);
    }

    @Override
    public void setConnectTimeout(int timeoutMilliSeconds) {
        httpClientBuilder.connectTimeout(timeoutMilliSeconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setReadTimeout(int readTimeoutMilliSeconds) {
        httpClientBuilder.readTimeout(readTimeoutMilliSeconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setInstanceFollowRedirects(boolean followRedirects) {
        httpClientBuilder.followRedirects(followRedirects);
    }

    @Override
    public void setRequestData(String mimeType, String data) {
        requestBody = RequestBody.create(data, MediaType.get(mimeType));
    }

    @Override
    public InputStream getInputStream() throws IOException, IOError {
        if(response == null) {
            connect();
        }
        return response.body().byteStream();
    }

    @Override
    public String getRequestProperty(String key) throws IllegalStateException {
        return headersBuilder.get(key);
    }

    @Override
    public int getResponseCode() throws IOException {
        if(response == null) {
            connect();
        }
        return response.code();
    }

    @Override
    public InputStream getErrorStream() throws IOException {
        if(200 <= getResponseCode()  && getResponseCode() < 300) {
            return null;
        }
        return response.body().byteStream();
    }

    @Override
    public void connect() throws IOException, IOError {
        if(!method.equals("GET")) {
            requestBuilder.method(method, requestBody);
        }
        response = httpClientBuilder.build()
            .newCall(requestBuilder
                .headers(headersBuilder.build())
                .build()).execute();
    }
}
