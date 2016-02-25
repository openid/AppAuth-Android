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

import static net.openid.appauth.Preconditions.checkArgument;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Convenience methods for building and parsing scope strings.
 */
final class ScopeUtil {

    /**
     * Converts an iterable collection of scope strings into the consolidated, space-delimited
     * scope string that is carried in authorization requests. If the provided iterable is
     * {@code null}, or contains no elements, then {@code null} will be returned.
     * If any individual scope element is null or empty, an exception will be thrown.
     */
    @Nullable
    public static String scopeIterableToString(@Nullable Iterable<String> scopes) {
        if (scopes == null) {
            return null;
        }

        Set<String> scopeSet = new LinkedHashSet<>();
        for (String scope : scopes) {
            checkArgument(!TextUtils.isEmpty(scope),
                    "individual scopes cannot be null or empty");
            scopeSet.add(scope);
        }

        if (scopeSet.isEmpty()) {
            return null;
        }

        return TextUtils.join(" ", scopeSet);
    }

    /**
     * Converts the consolidated, space-delimited scope string to a set. If the supplied scope
     * string is {@code null}, then {@code null} will be returned.
     */
    @Nullable
    public static Set<String> scopeStringToSet(@Nullable String scopeStr) {
        if (scopeStr == null) {
            return null;
        }
        List<String> scopes = Arrays.asList(TextUtils.split(scopeStr, " "));
        LinkedHashSet<String> scopeSet = new LinkedHashSet<>(scopes.size());
        scopeSet.addAll(scopes);
        return scopeSet;
    }
}
