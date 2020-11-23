package net.openid.appauth.connectivity.ok;

import android.net.Uri;

import androidx.annotation.NonNull;

import net.openid.appauth.connectivity.ConnectionBuilder;
import net.openid.appauth.connectivity.HttpConnection;

import java.io.IOException;

import okhttp3.OkHttpClient;

public class OkConnectionBuilder implements ConnectionBuilder {
    private final OkHttpClient client;

    public OkConnectionBuilder(OkHttpClient client) {
        this.client = client;
    }

    @NonNull
    @Override
    public HttpConnection openConnection(@NonNull Uri uri) {
        return new OkHttpConnectionImpl(client, uri);
    }
}
