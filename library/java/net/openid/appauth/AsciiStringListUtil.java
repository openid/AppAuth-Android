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

import android.text.TextUtils;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Convenience methods for building and parsing space-delimited string lists, which are
 * frequently used in OAuth2 and OpenID Connect parameters.
 */
final class AsciiStringListUtil {

    private AsciiStringListUtil() {
        throw new IllegalStateException("This type is not intended to be instantiated");
    }

    /**
     * Converts an iterable collection of strings into a consolidated, space-delimited
     * format. If the provided iterable is `null`, or contains no elements, then
     * `null` will be returned. If any individual scope element is `null` or empty, an
     * exception will be thrown.
     */
    @Nullable
    public static String iterableToString(@Nullable Iterable<String> strings) {
        if (strings == null) {
            return null;
        }

        Set<String> stringSet = new LinkedHashSet<>();
        for (String str : strings) {
            checkArgument(!TextUtils.isEmpty(str),
                    "individual scopes cannot be null or empty");
            stringSet.add(str);
        }

        if (stringSet.isEmpty()) {
            return null;
        }

        return TextUtils.join(" ", stringSet);
    }

    /**
     * Converts the consolidated, space-delimited scope string to a set. If the supplied scope
     * string is `null`, then `null` will be returned.
     */
    @Nullable
    public static Set<String> stringToSet(@Nullable String spaceDelimitedStr) {
        if (spaceDelimitedStr == null) {
            return null;
        }
        List<String> strings = Arrays.asList(TextUtils.split(spaceDelimitedStr, " "));
        LinkedHashSet<String> stringSet = new LinkedHashSet<>(strings.size());
        stringSet.addAll(strings);
        return stringSet;
    }
}
