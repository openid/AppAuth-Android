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
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A base response for session management models
 * {@link AuthorizationResponse}
 * {@link EndSessionResponse}
 */
public abstract class AuthorizationManagementResponse {

    /**
     * Builds an  AuthorizationManagementResponse from {@link AuthorizationManagementRequest}
     * and response {@link Uri}
     */
    public static AuthorizationManagementResponse buildFromRequest(
            AuthorizationManagementRequest request, Uri uri) {
        if (request instanceof AuthorizationRequest) {
            return new AuthorizationResponse.Builder((AuthorizationRequest) request)
                .fromUri(uri)
                .build();
        }
        if (request instanceof EndSessionRequest) {
            return EndSessionResponse.fromRequestAndUri((EndSessionRequest)request,uri);
        }
        throw new IllegalArgumentException("Malformed request or uri");
    }


    /**
     * Extracts response from an intent produced by {@link #toIntent()}. This is
     * used to extract the response from the intent data passed to an activity registered as the
     * handler for {@link AuthorizationService#performEndOfSessionRequest}
     * or {@link AuthorizationService#performAuthManagementRequest}.
     */
    @Nullable
    public static AuthorizationManagementResponse fromIntent(@NonNull Intent dataIntent) {
        if (EndSessionResponse.containsEndSessionResoponse(dataIntent)) {
            return EndSessionResponse.fromIntent(dataIntent);
        }
        if (AuthorizationResponse.containEndSessionResoponse(dataIntent)) {
            return AuthorizationResponse.fromIntent(dataIntent);
        }
        throw new IllegalArgumentException("Malformed intent");
    }

    public abstract String getState();

    public abstract Intent toIntent();
}
