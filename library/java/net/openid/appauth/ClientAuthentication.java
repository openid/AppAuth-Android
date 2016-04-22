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

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.Map;

public class ClientAuthentication {
    @Nullable
    protected String mClientSecret;

    /**
     * Creates a {@link ClientAuthentication} which does not perform any client
     * authentication.
     */
    public ClientAuthentication() {
        this(null);
    }

    protected ClientAuthentication(@Nullable String clientSecret) {
        mClientSecret = clientSecret;
    }

    /**
     * Constructs any extra parameters necessary to include in the request for the client
     * authentication.
     * <p/>
     * <p>Subclasses implementing a client authentication scheme should override this method and
     * return any extra parameters that should be included in the request body.
     * The subclass MUST NOT send any request data on the given connection, however it may
     * send any necessary HTTP headers with
     * {@link HttpURLConnection#setRequestProperty(String, String)}.</p>
     */
    protected Map<String, String> setupRequestParameters(
            String clientId, HttpURLConnection connection) {
        // No extra request parameters by default, meaning no client authentication
        return null;
    }

    /**
     * Applies client authentication for the specified request based on the {@link TokenRequest}.
     *
     * @throws IOException if the connection fails
     */
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
