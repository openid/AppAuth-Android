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

package net.openid.appauth.internal;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Originally created by thoeflicker on 5/5/16.
 */
public class UriParser {
    /**
     * The URI response parameter mode
     */
    private enum UriResponseMode {
        QUERY,
        FRAGMENT

    }

    private static final String TAG = "UriParser";

    private final Uri mUri;
    private final UriResponseMode mMode;
    private Map<String, String> mFragmentParamMap;

    public UriParser(@NonNull Uri uri) {
        this.mUri = uri;
        mMode = uri.getFragment() != null ? UriResponseMode.FRAGMENT : UriResponseMode.QUERY;
        if (mMode == UriResponseMode.FRAGMENT) {
            parseFragmentUri();
        }
    }

    /**
     * Returns a set of the unique names of all query parameters. Iterating
     * over the set will return the names in order of their first occurrence.
     *
     * @return a set of decoded names
     * @throws UnsupportedOperationException if this isn't a hierarchical URI
     */
    public Set<String> getQueryParameterNames() {
        if (mMode == UriResponseMode.QUERY) {
            return mUri.getQueryParameterNames();
        } else {
            return mFragmentParamMap.keySet();
        }
    }


    /**
     * Searches the query string for parameter values with the given key.
     *
     * @param key which will be encoded
     * @return a list of decoded values
     * @throws UnsupportedOperationException if this isn't a hierarchical URI
     * @throws NullPointerException          if key is null
     */
    public String getQueryParameter(String key) {
        if (mMode == UriResponseMode.QUERY) {
            return mUri.getQueryParameter(key);
        } else {
            key = Uri.encode(key);
            return mFragmentParamMap.get(key);
        }
    }

    @VisibleForTesting
    void parseFragmentUri() {
        mFragmentParamMap = new HashMap<>();
        String fragment = mUri.getEncodedFragment();
        if (fragment == null) {
            return;
        }
        String[] keyValuePairs = fragment.split("&");
        for (String keyValue : keyValuePairs) {
            String[] raw = keyValue.split("=");
            if (raw.length != 2) {
                Log.d(TAG, ("parseFragmentUri: "
                        + "Unqualified URI response argument encountered: ").concat(keyValue));
                continue;
            }
            mFragmentParamMap.put(raw[0], raw[1]);
        }
    }
}
