package net.openid.appauth;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.Map;

public class ClientAuthentication {
    @Nullable
    protected String clientSecret;

    public ClientAuthentication() {
        this(null);
    }

    protected ClientAuthentication(@Nullable String clientSecret) {
        this.clientSecret = clientSecret;
    }

    protected Map<String, String> setupRequestParameters(String clientId, HttpURLConnection connection) {
        return null;
    }

    public void apply(TokenRequest request, HttpURLConnection connection) throws IOException {
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // required by some providers to ensure JSON response
        connection.setRequestProperty("Accept", "application/json");

        connection.setInstanceFollowRedirects(false);
        connection.setDoOutput(true);


        Map<String, String> parameters = request.getRequestParameters();
        Map<String, String> extraParameters = setupRequestParameters(request.clientId, connection);
        if (extraParameters != null) {
            parameters.putAll(extraParameters);
        }
        String queryData = UriUtil.formUrlEncode(parameters);

        connection.setRequestProperty("Content-Length", String.valueOf(queryData.length()));
        OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
        wr.write(queryData);
        wr.flush();
    }
}
