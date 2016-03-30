package net.openid.appauth;

import android.support.annotation.NonNull;
import android.util.Base64;

import java.net.HttpURLConnection;
import java.util.Map;

import static net.openid.appauth.Preconditions.checkNotNull;

public class ClientSecretBasic extends ClientAuthentication {
    public static final String NAME = "client_secret_basic";

    public ClientSecretBasic(@NonNull String clientSecret) {
        super(clientSecret);
        checkNotNull(clientSecret, "clientSecret cannot be null");
    }

    @Override
    public Map<String, String> setupRequestParameters(String clientId, HttpURLConnection connection) {
        String credentials = clientId + ":" + clientSecret;
        String basicAuth = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        connection.setRequestProperty("Authorization", "Basic " + basicAuth);
        return null; // No additional params to write in POST body
    }
}
