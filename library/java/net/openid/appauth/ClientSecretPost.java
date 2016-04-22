/*
 * Copyright 2015 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openid.appauth;

import static net.openid.appauth.Preconditions.checkNotNull;

import android.support.annotation.NonNull;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;


/**
 * Implementation of the client authentication method 'client_secret_post'.
 *
 * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">
 * "OpenID Connect Core 1.0", Section 9</a>
 */
public class ClientSecretPost extends ClientAuthentication {
    /**
     * Name of this authentication method.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">
     * "OpenID Connect Core 1.0", Section 9</a>
     */
    public static final String NAME = "client_secret_post";
    static final String PARAM_CLIENT_ID = "client_id";
    static final String PARAM_CLIENT_SECRET = "client_secret";

    /**
     * Creates a {@link ClientAuthentication} which will use the client authentication method
     * 'client_secret_post'.
     */
    public ClientSecretPost(@NonNull String clientSecret) {
        super(clientSecret);
        checkNotNull(clientSecret, "clientSecret cannot be null");
    }

    @Override
    protected final Map<String, String> setupRequestParameters(
            String clientId, HttpURLConnection connection) {
        Map<String, String> additionalParameters = new HashMap<>();
        additionalParameters.put(PARAM_CLIENT_ID, clientId);
        additionalParameters.put(PARAM_CLIENT_SECRET, mClientSecret);
        return additionalParameters;
    }
}
