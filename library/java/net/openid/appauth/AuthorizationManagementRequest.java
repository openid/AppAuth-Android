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

import org.json.JSONObject;

/**
 * A base request for session management models
 * {@link AuthorizationRequest}
 * {@link EndSessionRequest}
 */
public interface AuthorizationManagementRequest {

    /**
     * Produces a JSON representation of the request for persistent storage or local transmission
     * (e.g. between activities).
     */
    JSONObject jsonSerialize();

    /**
     * Produces a JSON string representation of the request for persistent storage or
     * local transmission (e.g. between activities). This method is just a convenience wrapper
     * for {@link #jsonSerialize()}, converting the JSON object to its string form.
     */
    String jsonSerializeString();

    /**
     * An opaque value used by the client to maintain state between the request and callback.
     */
    String getState();

    /**
     * Produces a request URI, that can be used to dispatch the request.
     */
    Uri toUri();
}
