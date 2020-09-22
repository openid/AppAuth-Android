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

import android.content.Intent;
import androidx.annotation.NonNull;

import org.json.JSONObject;

/**
 * A base response for session management models
 * {@link AuthorizationResponse}
 * {@link EndSessionResponse}
 */
public abstract class AuthorizationManagementResponse {

    public abstract String getState();

    public abstract Intent toIntent();

    /**
     * Produces a JSON representation of the request for persistent storage or local transmission
     * (e.g. between activities).
     */
    public abstract JSONObject jsonSerialize();

    /**
     * Produces a JSON representation of the end session response for persistent storage or local
     * transmission (e.g. between activities). This method is just a convenience wrapper
     * for {@link #jsonSerialize()}, converting the JSON object to its string form.
     */
    @NonNull
    public String jsonSerializeString() {
        return jsonSerialize().toString();
    }
}
