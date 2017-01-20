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

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for extracting parameters from Uri objects.
 */
class UriUtil {

    private UriUtil() {
        throw new IllegalStateException("This type is not intended to be instantiated");
    }

    public static Uri parseUriIfAvailable(@Nullable String uri) {
        if (uri == null) {
            return null;
        }

        return Uri.parse(uri);
    }

    public static void appendQueryParameterIfNotNull(
            @NonNull Uri.Builder uriBuilder,
            @NonNull String paramName,
            @Nullable Object value) {
        if (value == null) {
            return;
        }

        String valueStr = value.toString();
        if (valueStr == null) {
            return;
        }

        uriBuilder.appendQueryParameter(paramName, value.toString());
    }

    public static Long getLongQueryParameter(@NonNull Uri uri, @NonNull String param) {
        String valueStr = uri.getQueryParameter(param);
        if (valueStr != null) {
            return Long.parseLong(valueStr);
        }
        return null;
    }

    public static String formUrlEncode(Map<String, String> parameters) {
        if (parameters == null) {
            return "";
        }

        List<String> queryParts = new ArrayList<>();
        for (Map.Entry<String, String> param : parameters.entrySet()) {
            try {
                queryParts.add(param.getKey() + "=" + URLEncoder.encode(param.getValue(), "utf-8"));
            } catch (UnsupportedEncodingException e) {
                // Should not end up here
                Logger.error("Could not utf-8 encode.");
            }
        }
        return TextUtils.join("&", queryParts);
    }
}
