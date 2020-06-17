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

import static net.openid.appauth.Preconditions.checkArgument;
import static net.openid.appauth.Preconditions.checkNotNull;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for handling additional parameters in requests and responses.
 */
class AdditionalParamsProcessor {

    static Set<String> builtInParams(String... params) {
        if (params == null || params.length == 0) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(params)));
    }

    static Map<String, String> checkAdditionalParams(
            @Nullable Map<String, String> params,
            @NonNull Set<String> builtInParams) {
        if (params == null) {
            return Collections.emptyMap();
        }

        Map<String, String> additionalParams = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            checkNotNull(key, "additional parameter keys cannot be null");
            checkNotNull(value, "additional parameter values cannot be null");

            checkArgument(!builtInParams.contains(key),
                    "Parameter %s is directly supported via the authorization request builder, "
                            + "use the builder method instead",
                    key);
            additionalParams.put(key, value);
        }

        return Collections.unmodifiableMap(additionalParams);
    }

    static Map<String, String> extractAdditionalParams(
            JSONObject json,
            Set<String> builtInParams) throws JSONException {
        Map<String, String> additionalParams = new LinkedHashMap<>();

        Iterator<String> keysIter = json.keys();
        while (keysIter.hasNext()) {
            String key = keysIter.next();
            if (!builtInParams.contains(key)) {
                additionalParams.put(key, json.get(key).toString());
            }
        }
        return additionalParams;
    }

    static Map<String, String> extractAdditionalParams(
            Uri uri,
            Set<String> builtInParams) {
        Map<String, String> additionalParams = new LinkedHashMap<>();
        for (String param : uri.getQueryParameterNames()) {
            if (!builtInParams.contains(param)) {
                additionalParams.put(param, uri.getQueryParameter(param));
            }
        }
        return additionalParams;
    }

    private AdditionalParamsProcessor() {}
}
