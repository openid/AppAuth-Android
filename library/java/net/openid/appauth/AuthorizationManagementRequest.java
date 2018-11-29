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

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;

/**
 * A base request for session management models
 * {@link AuthorizationRequest}
 * {@link EndSessionRequest}
 */
public abstract class AuthorizationManagementRequest {

    private static final int STATE_LENGTH = 16;

    /**
     * Reads an authorization request from a JSON string representation produced by
     * {@link #jsonSerialize()}.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    public static AuthorizationManagementRequest jsonDeserialize(String jsonStr)
            throws JSONException {
        Preconditions.checkNotNull(jsonStr, "jsonStr can not be null");
        JSONObject json = new JSONObject(jsonStr);
        if (AuthorizationRequest.isAuthorizationRequest(json)) {
            return AuthorizationRequest.jsonDeserialize(json);
        }
        if (EndSessionRequest.isEndSessionRequest(json)) {
            return EndSessionRequest.jsonDeserialize(json);
        }
        throw new IllegalArgumentException(
            "No AuthorizationManagementRequest found maching to this json schema");
    }

    static String generateRandomState() {
        SecureRandom sr = new SecureRandom();
        byte[] random = new byte[STATE_LENGTH];
        sr.nextBytes(random);
        return Base64.encodeToString(random, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
    }

    /**
     * Produces a JSON representation of the authorization request for persistent storage or local
     * transmission (e.g. between activities).
     */
    public abstract JSONObject jsonSerialize();

    /**
     * Produces a JSON string representation of the authorization request for persistent storage or
     * local transmission (e.g. between activities). This method is just a convenience wrapper
     * for {@link #jsonSerialize()}, converting the JSON object to its string form.
     */
    public abstract String jsonSerializeString();

    public abstract String getState();

    public abstract Uri toUri();

}
