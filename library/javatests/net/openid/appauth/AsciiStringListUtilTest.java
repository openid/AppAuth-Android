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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=16)
public final class AsciiStringListUtilTest {

    private static final List<String> SCOPES = Arrays.asList("email", "profile", "openid");
    private static final String SCOPE_STRING = "email profile openid";

    @Test
    public void testScopeIterableToString() throws Exception {
        assertEquals(SCOPE_STRING, AsciiStringListUtil.iterableToString(SCOPES));
    }

    @Test
    public void testScopeIterableToString_nullIterable() throws Exception {
        assertNull(AsciiStringListUtil.iterableToString(null));
    }

    @Test
    public void testScopeStringToSet() throws Exception {
        Set<String> result = AsciiStringListUtil.stringToSet("email profile email openid");
        assertNotNull(result);
        assertEquals(SCOPES.size(), result.size());
        assertTrue(result.contains("email"));
        assertTrue(result.contains("profile"));
        assertTrue(result.contains("openid"));
    }
}
