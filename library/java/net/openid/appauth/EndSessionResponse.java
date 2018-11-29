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

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A response to end session request.
 *
 * @see EndSessionRequest
 * @see "OpenID Connect Session Management 1.0 - draft 28, 5 RP-Initiated Logout
 * <https://openid.net/specs/openid-connect-session-1_0.html#RPLogout>"
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
     * The end session reques associated with this response.
     */
    @NonNull
    public final EndSessionRequest endSessionRequest;

    /**
     * The returned state parameter, which must match the value specified in the request.
     * AppAuth for Android ensures that this is the case.
     */
    @NonNull
    public final String state;

    EndSessionResponse(@NonNull EndSessionRequest endSessionRequest, @NonNull String state) {
        Preconditions.checkNotNull(endSessionRequest);
        Preconditions.checkNotNull(state);
        this.endSessionRequest = endSessionRequest;
        this.state = state;
    }

    @Override
    public String getState() {
        return state;
    }

    /**
     * Produces an intent containing this end session response. This is used to deliver the
     * end session response to the registered handler after a call to
     * {@link AuthorizationService#performEndOfSessionRequest}.
     */
    @Override
    public Intent toIntent() {
        Intent data = new Intent();
        data.putExtra(EXTRA_RESPONSE, this.jsonSerialize().toString());
        return data;
    }

    /**
     * Produces a JSON representation of the authorization response for persistent storage or local
     * transmission (e.g. between activities).
     */
    @NonNull
    public JSONObject jsonSerialize() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_REQUEST, endSessionRequest.jsonSerialize());
        JsonUtil.putIfNotNull(json, KEY_STATE, state);
        return json;
    }

    public static EndSessionResponse fromRequestAndUri(@NonNull EndSessionRequest request,
                                                       @NonNull Uri uri) {
        checkNotNull(request, "request can not be null");
        checkNotNull(uri, "uri can not be null");
        return new EndSessionResponse(request, uri.getQueryParameter(EndSessionRequest.KEY_STATE));
    }

    /**
     * Extracts an end session response from an intent produced by {@link #toIntent()}. This is
     * used to extract the response from the intent data passed to an activity registered as the
     * handler for {@link AuthorizationService#performEndOfSessionRequest}.
     */
    @Nullable
    public static EndSessionResponse fromIntent(@NonNull Intent dataIntent) {
        checkNotNull(dataIntent, "dataIntent must not be null");
        if (!dataIntent.hasExtra(EXTRA_RESPONSE)) {
            return null;
        }

        try {
            return EndSessionResponse
                    .jsonDeserializeString(dataIntent.getStringExtra(EXTRA_RESPONSE));
        } catch (JSONException ex) {
            throw new IllegalArgumentException("Intent contains malformed auth response", ex);
        }
    }

    /**
     * Reads an authorization response from a JSON string representation produced by
     * {@link #jsonSerialize()}.
     *
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static EndSessionResponse jsonDeserializeString(@NonNull String jsonStr)
            throws JSONException {
        return jsonDeserializeString(new JSONObject(jsonStr));
    }

    /**
     * Reads an authorization request from a JSON representation produced by
     * {@link #jsonSerialize()} converted to String. This method is just a convenience wrapper for
     * {@link #jsonDeserializeString(JSONObject)},
     * converting the JSON string to its JSON object form.
     *
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static EndSessionResponse jsonDeserializeString(@NonNull JSONObject json)
            throws JSONException {
        if (!json.has(KEY_REQUEST)) {
            throw new IllegalArgumentException(
                "authorization request not provided and not found in JSON");
        }

        EndSessionRequest request =
                EndSessionRequest.jsonDeserialize(json.getJSONObject(KEY_REQUEST));

        return new EndSessionResponse(
                request,
                JsonUtil.getString(json, KEY_STATE)
            );
    }


    static boolean containsEndSessionResoponse(@NonNull Intent intent) {
        return intent.hasExtra(EXTRA_RESPONSE);
    }
}
