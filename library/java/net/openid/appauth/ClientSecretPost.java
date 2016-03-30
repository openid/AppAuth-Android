package net.openid.appauth;

import android.support.annotation.NonNull;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import static net.openid.appauth.Preconditions.checkNotNull;

public class ClientSecretPost extends ClientAuthentication {
    public static final String NAME = "client_secret_post";
    public static final String PARAM_CLIENT_ID = "client_id";
    public static final String PARAM_CLIENT_SECRET = "client_secret";

    public ClientSecretPost(@NonNull String clientSecret) {
        super(clientSecret);
        checkNotNull(clientSecret, "clientSecret cannot be null");
    }

    @Override
    public Map<String, String> setupRequestParameters(String clientId, HttpURLConnection connection) {
        Map<String, String> additionalParameters = new HashMap<>();
        additionalParameters.put(PARAM_CLIENT_ID, clientId);
        additionalParameters.put(PARAM_CLIENT_SECRET, clientSecret);
        return additionalParameters;
    }
}
