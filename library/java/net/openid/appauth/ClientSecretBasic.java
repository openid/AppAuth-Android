/*
 * Copyright 2016 The AppAuth for Android Authors. All Rights Reserved.
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

import android.util.Base64;
import androidx.annotation.NonNull;

import net.openid.appauth.internal.UriUtil;

import java.util.Collections;
import java.util.Map;

/**
 * Implementation of the client authentication method 'client_secret_basic'.
 *
 * @see "OpenID Connect Core 1.0, Section 9
 * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.9>"
 */
public class ClientSecretBasic implements ClientAuthentication {
    /**
     * Name of this authentication method.
     *
     * @see "OpenID Connect Core 1.0, Section 9
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.9>"
     */
    public static final String NAME = "client_secret_basic";

    @NonNull
    private String mClientSecret;

    /**
     * Creates a {@link ClientAuthentication} which will use the client authentication method
     * `client_secret_basic`.
     */
    public ClientSecretBasic(@NonNull String clientSecret) {
        mClientSecret = checkNotNull(clientSecret, "mClientSecret cannot be null");
    }

    @Override
    public final Map<String, String> getRequestHeaders(@NonNull String clientId) {
        // From the OAuth2 RFC, client ID and secret should be encoded prior to concatenation and
        // conversion to Base64: https://tools.ietf.org/html/rfc6749#section-2.3.1
        String encodedClientId = UriUtil.formUrlEncodeValue(clientId);
        String encodedClientSecret = UriUtil.formUrlEncodeValue(mClientSecret);
        String credentials = encodedClientId + ":" + encodedClientSecret;
        String basicAuth = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        return Collections.singletonMap("Authorization", "Basic " + basicAuth);
    }

    @Override
    public final Map<String, String> getRequestParameters(@NonNull String clientId) {
        return null;
    }
}
