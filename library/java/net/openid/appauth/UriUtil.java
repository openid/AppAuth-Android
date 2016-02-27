/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for extracting parameters from Uri objects.
 */
class UriUtil {

    private UriUtil() {}

    public static Uri parseUriIfAvailable(@Nullable String uri) {
        if (uri == null) {
            return null;
        }

        return Uri.parse(uri);
    }

    public static void appendQueryParameterIfNotNull(
            @NonNull Uri.Builder uriBuilder,
            @NonNull String paramName,
            @Nullable String value) {
        if (value == null) {
            return;
        }

        uriBuilder.appendQueryParameter(paramName, value);
    }

    public static Map<String, String> extractAdditionalParameters(
            @NonNull Uri uri,
            @NonNull Set<String> ignore) {
        LinkedHashMap<String, String> additionalParams = new LinkedHashMap<>();
        for (String param : uri.getQueryParameterNames()) {
            if (!ignore.contains(param)) {
                additionalParams.put(param, uri.getQueryParameter(param));
            }
        }
        return additionalParams;
    }

    public static Long getLongQueryParameter(@NonNull Uri uri, @NonNull String param) {
        String valueStr = uri.getQueryParameter(param);
        if (valueStr != null) {
            return Long.parseLong(valueStr);
        }
        return null;
    }
}
