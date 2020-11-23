package net.openid.appauth.connectivity;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;

public class DefaultHttpConnectionImpl implements HttpConnection {
    private final HttpURLConnection connection;

    public DefaultHttpConnectionImpl(HttpURLConnection con) {
        connection = con;
    }

    @Override
    public void setRequestMethod(String method) throws ProtocolException {
        connection.setRequestMethod(method);
    }

    @Override
    public void setDoInput(boolean doInput) {
        connection.setDoInput(doInput);
    }

    @Override
    public void setDoOutput(boolean doOutput) {
        connection.setDoOutput(doOutput);
    }

    @Override
    public void setRequestProperty(String key, String value) {
        connection.setRequestProperty(key, value);
    }

    @Override
    public void setConnectTimeout(int timeoutMilliSeconds) {
        connection.setConnectTimeout(timeoutMilliSeconds);
    }

    @Override
    public void setReadTimeout(int readTimeoutMilliSeconds) {
        connection.setReadTimeout(readTimeoutMilliSeconds);
    }

    @Override
    public void setInstanceFollowRedirects(boolean followRedirects) {
        connection.setInstanceFollowRedirects(followRedirects);
    }

    @Override
    public void setRequestData(String mimeType, String data) throws IOException {
        OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
        wr.write(data);
        wr.flush();
    }


    @Override
    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    @Override
    public String getRequestProperty(String key) throws IllegalStateException {
        return connection.getRequestProperty(key);
    }

    @Override
    public int getResponseCode() throws IOException {
        return connection.getResponseCode();
    }

    @Override
    public InputStream getErrorStream() {
        return connection.getErrorStream();
    }

    @Override
    public void connect() throws IOException, IOError {
        connection.connect();
    }
}
