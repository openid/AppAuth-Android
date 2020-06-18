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

package net.openid.appauth.internal;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsService;
import androidx.core.util.Pair;

import net.openid.appauth.Preconditions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for extracting parameters from Uri objects.
 */
public final class UriUtil {

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

    public static List<Bundle> toCustomTabUriBundle(Uri[] uris, int startIndex) {
        Preconditions.checkArgument(startIndex >= 0, "startIndex must be positive");
        if (uris == null || uris.length <= startIndex) {
            return Collections.emptyList();
        }

        List<Bundle> uriBundles = new ArrayList<>(uris.length - startIndex);
        for (int i = startIndex; i < uris.length; i++) {
            if (uris[i] == null) {
                Logger.warn("Null URI in possibleUris list - ignoring");
                continue;
            }

            Bundle uriBundle = new Bundle();
            uriBundle.putParcelable(CustomTabsService.KEY_URL, uris[i]);
            uriBundles.add(uriBundle);
        }

        return uriBundles;
    }

    @NonNull
    public static String formUrlEncode(@Nullable Map<String, String> parameters) {
        if (parameters == null) {
            return "";
        }

        List<String> queryParts = new ArrayList<>();
        for (Map.Entry<String, String> param : parameters.entrySet()) {
            queryParts.add(param.getKey() + "=" + formUrlEncodeValue(param.getValue()));
        }
        return TextUtils.join("&", queryParts);
    }

    @NonNull
    public static String formUrlEncodeValue(@NonNull String value) {
        Preconditions.checkNotNull(value);
        try {
            return URLEncoder.encode(value, "utf-8");
        } catch (UnsupportedEncodingException ex) {
            // utf-8 should always be supported
            throw new IllegalStateException("Unable to encode using UTF-8");
        }
    }

    public static List<Pair<String, String>> formUrlDecode(String encoded) {
        if (TextUtils.isEmpty(encoded)) {
            return Collections.emptyList();
        }

        String[] parts = encoded.split("&");
        List<Pair<String, String>> params = new ArrayList<>();

        for (String part : parts) {
            String[] paramAndValue = part.split("=");
            String param = paramAndValue[0];
            String encodedValue = paramAndValue[1];

            try {
                params.add(Pair.create(param, URLDecoder.decode(encodedValue, "utf-8")));
            } catch (UnsupportedEncodingException ex) {
                Logger.error("Unable to decode parameter, ignoring", ex);
            }
        }

        return params;
    }

    public static Map<String, String> formUrlDecodeUnique(String encoded) {
        List<Pair<String, String>> params = UriUtil.formUrlDecode(encoded);
        Map<String, String> uniqueParams = new HashMap<>();

        for (Pair<String, String> param : params) {
            uniqueParams.put(param.first, param.second);
        }

        return uniqueParams;
    }
}
