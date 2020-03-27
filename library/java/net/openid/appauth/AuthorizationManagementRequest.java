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

import android.net.Uri;
import android.util.Base64;

import org.json.JSONObject;

import java.security.SecureRandom;

/**
 * A base request for session management models
 * {@link AuthorizationRequest}
 * {@link EndSessionRequest}
 */
abstract class AuthorizationManagementRequest {

    private static final int STATE_LENGTH = 16;

    static String generateRandomState() {
        SecureRandom sr = new SecureRandom();
        byte[] random = new byte[STATE_LENGTH];
        sr.nextBytes(random);
        return Base64.encodeToString(random, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
    }

    /**
     * Produces a JSON representation of the request for persistent storage or local transmission
     * (e.g. between activities).
     */
    public abstract JSONObject jsonSerialize();

    /**
     * Produces a JSON string representation of the request for persistent storage or
     * local transmission (e.g. between activities). This method is just a convenience wrapper
     * for {@link #jsonSerialize()}, converting the JSON object to its string form.
     */
    public String jsonSerializeString() {
        return jsonSerialize().toString();
    }

    /**
     * An opaque value used by the client to maintain state between the request and callback.
     */
    public abstract String getState();

    /**
     * Produces a request URI, that can be used to dispatch the request.
     */
    public abstract Uri toUri();

}
