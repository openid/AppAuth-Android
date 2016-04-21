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

package org.robolectric.shadows;

import android.content.IntentFilter;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Iterator;

/**
 * Adds missing methods to the default robolectric ShadowIntentFilter. These fixes will be provided
 * upstream at some point in the future, at which point this class can be removed.
 */
@Implements(IntentFilter.class)
public class ShadowIntentFilterFixed extends ShadowIntentFilter {

    /**
     * Determines if the intent has the specified action set.
     */
    @Implementation
    public boolean hasAction(String action) {
        return actions.contains(action);
    }

    /**
     * Provides an iterator over all defined schemes. Returns {@code null} if none were
     * specified.
     */
    @Implementation
    public Iterator<String> schemesIterator() {
        return schemes.isEmpty() ? null : schemes.iterator();
    }

    /**
     * Provides an iterator over all defined authorities. Returns {@code null} if none were
     * specified.
     */
    @Implementation
    public Iterator<IntentFilter.AuthorityEntry> authoritiesIterator() {
        return authoritites.isEmpty() ? null : authoritites.iterator();
    }
}
