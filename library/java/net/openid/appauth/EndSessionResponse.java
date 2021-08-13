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
import static net.openid.appauth.Preconditions.checkNullOrNotEmpty;

import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A response to end session request.
 *
 * @see EndSessionRequest
 * @see "OpenID Connect RP-Initiated Logout 1.0 - draft 01
 * <https://openid.net/specs/openid-connect-rpinitiated-1_0.html>"
 */
public class EndSessionResponse extends AuthorizationManagementResponse {

    /**
     * The extra string used to store an {@link EndSessionResponse} in an intent by
     * {@link #toIntent()}.
     */
    public static final String EXTRA_RESPONSE = "net.openid.appauth.EndSessionResponse";

    @VisibleForTesting
    static final String KEY_REQUEST = "request";

    @VisibleForTesting
    static final String KEY_STATE = "state";

    /**
     * The end session request associated with this response.
     */
    @NonNull
    public final EndSessionRequest request;

    /**
     * The returned state parameter, which must match the value specified in the request.
     * AppAuth for Android ensures that this is the case.
     */
    @Nullable
    public final String state;

    /**
     * Creates instances of {@link EndSessionResponse}.
     */
    public static final class Builder {
        @NonNull
        private EndSessionRequest mRequest;

        @Nullable
        private String mState;


        public Builder(@NonNull EndSessionRequest request) {
            setRequest(request);
        }

        @VisibleForTesting
        Builder fromUri(@NonNull Uri uri) {
            setState(uri.getQueryParameter(KEY_STATE));
            return this;
        }

        public Builder setRequest(@NonNull EndSessionRequest request) {
            mRequest = checkNotNull(request, "request cannot be null");
            return this;
        }

        public Builder setState(@Nullable String state) {
            mState = checkNullOrNotEmpty(state, "state must not be empty");
            return this;
        }

        /**
         * Builds the response object.
         */
        @NonNull
        public EndSessionResponse build() {
            return new EndSessionResponse(
                mRequest,
                mState);
        }
    }

    private EndSessionResponse(
            @NonNull EndSessionRequest request,
            @Nullable String state) {
        this.request = request;
        this.state = state;
    }

    @Override
    @Nullable
    public String getState() {
        return state;
    }

    /**
     * Produces a JSON representation of the end session response for persistent storage or local
     * transmission (e.g. between activities).
     */
    @Override
    @NonNull
    public JSONObject jsonSerialize() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_REQUEST, request.jsonSerialize());
        JsonUtil.putIfNotNull(json, KEY_STATE, state);
        return json;
    }

    /**
     * Reads an end session response from a JSON string representation produced by
     * {@link #jsonSerialize()}.
     *
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static EndSessionResponse jsonDeserialize(@NonNull JSONObject json)
            throws JSONException {
        if (!json.has(KEY_REQUEST)) {
            throw new IllegalArgumentException(
                "authorization request not provided and not found in JSON");
        }

        return new EndSessionResponse(
                EndSessionRequest.jsonDeserialize(json.getJSONObject(KEY_REQUEST)),
                JsonUtil.getStringIfDefined(json, KEY_STATE));
    }

    /**
     * Reads an end session response from a JSON string representation produced by
     * {@link #jsonSerializeString()}. This method is just a convenience wrapper for
     * {@link #jsonDeserialize(JSONObject)}, converting the JSON string to its JSON object form.
     *
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static EndSessionResponse jsonDeserialize(@NonNull String jsonStr)
            throws JSONException {
        return jsonDeserialize(new JSONObject(jsonStr));
    }

    /**
     * Produces an intent containing this end session response. This is used to deliver the
     * end session response to the registered handler after a call to
     * {@link AuthorizationService#performEndSessionRequest}.
     */
    @Override
    public Intent toIntent() {
        Intent data = new Intent();
        data.putExtra(EXTRA_RESPONSE, this.jsonSerializeString());
        return data;
    }

    /**
     * Extracts an end session response from an intent produced by {@link #toIntent()}. This is
     * used to extract the response from the intent data passed to an activity registered as the
     * handler for {@link AuthorizationService#performEndSessionRequest}.
     */
    @Nullable
    public static EndSessionResponse fromIntent(@NonNull Intent dataIntent) {
        checkNotNull(dataIntent, "dataIntent must not be null");
        if (!dataIntent.hasExtra(EXTRA_RESPONSE)) {
            return null;
        }

        try {
            return EndSessionResponse.jsonDeserialize(dataIntent.getStringExtra(EXTRA_RESPONSE));
        } catch (JSONException ex) {
            throw new IllegalArgumentException("Intent contains malformed auth response", ex);
        }
    }

    static boolean containsEndSessionResponse(@NonNull Intent intent) {
        return intent.hasExtra(EXTRA_RESPONSE);
    }
}
