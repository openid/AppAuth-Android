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

package net.openid.appauthdemo;

import android.app.PendingIntent;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

// COPIED FROM APPAUTH LIBRARY SINCE IT IS NON-PUBLIC CLASS;
// MODIFIED, stores only 1 logout pending intent!


/**
 * Stores {@link PendingIntent} associated with each {@link AuthorizationRequest} made via
 * {@link AuthorizationService#performAuthorizationRequest}.
 * The pending intents are read and sent by
 * the {@link RedirectUriReceiverActivity} when the redirect Uri is received.
 */
/* package */ public class PendingIntentStore {
    private static final String TAG = "PendingIntentStore";
    private static final String KEY_ONLY_INTENT = "onlyPendingIntentKey";

    private Map<String, PendingIntent> mPendingIntents = new HashMap<>();

    private static PendingIntentStore sInstance;

    private PendingIntentStore() {
    }

    public static synchronized PendingIntentStore getPendingIntentStoreInstance() {
        if (sInstance == null) {
            sInstance = new PendingIntentStore();
        }
        return sInstance;
    }

    public void addPendingIntent(PendingIntent intent) {
        Log.v(TAG, "Adding pending intent for state %s" + KEY_ONLY_INTENT);
        mPendingIntents.put(KEY_ONLY_INTENT, intent);
    }

    public PendingIntent getPendingIntent() {
        Log.v(TAG, "Retrieving pending intent for scheme %s" + KEY_ONLY_INTENT);
        return mPendingIntents.remove(KEY_ONLY_INTENT);
    }

    @VisibleForTesting
    void clearPendingIntents() {
        mPendingIntents.clear();
    }
}
