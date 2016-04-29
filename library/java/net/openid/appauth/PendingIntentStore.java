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

import android.app.PendingIntent;
import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores {@link PendingIntent} associated with each {@link AuthorizationRequest} made via
 * {@link AuthorizationService#performAuthorizationRequest}.
 * The pending intents are read and sent by
 * the {@link RedirectUriReceiverActivity} when the redirect Uri is received.
 */
/* package */ class PendingIntentStore {
    private Map<String, AuthorizationRequest> mRequests = new HashMap<>();
    private Map<String, PendingIntent> mPendingIntents = new HashMap<>();

    private static PendingIntentStore sInstance;

    private PendingIntentStore() {}

    public static synchronized PendingIntentStore getInstance() {
        if (sInstance == null) {
            sInstance = new PendingIntentStore();
        }
        return sInstance;
    }

    public void addPendingIntent(AuthorizationRequest request, PendingIntent intent) {
        Logger.verbose("Adding pending intent for state %s", request.state);
        mRequests.put(request.state, request);
        mPendingIntents.put(request.state, intent);
    }

    public AuthorizationRequest getOriginalRequest(String state) {
        Logger.verbose("Retrieving original request for state %s", state);
        return mRequests.remove(state);
    }

    public PendingIntent getPendingIntent(String state) {
        Logger.verbose("Retrieving pending intent for scheme %s", state);
        return mPendingIntents.remove(state);
    }

    @VisibleForTesting void clearPendingIntents() {
        mPendingIntents.clear();
    }
}
