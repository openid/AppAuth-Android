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
import static net.openid.appauth.Preconditions.checkCollectionNotEmpty;
import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;
import static net.openid.appauth.Preconditions.checkNullOrNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class PreconditionsTest {
    private static final String TEST_MSG = "test";

    @Test
    public void testNotNull() {
        checkNotNull(new String());
    }

    @Test(expected = NullPointerException.class)
    public void testNotNull_null() {
        checkNotNull(null);
    }

    @Test
    public void testNotNullWithMessage() {
        checkNotNull(new String(), TEST_MSG);
    }

    @Test
    public void testNotNullWithMessage_null() {
        try {
            checkNotNull(null, TEST_MSG);
            fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException ex) {
            assertEquals(TEST_MSG, ex.getMessage());
        }
    }

    @Test
    public void testCheckArgument() {
        checkArgument(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckArgument_false() {
        checkArgument(false);
    }

    @Test
    public void testCheckArgumentWithMessage() {
        checkArgument(true, TEST_MSG);
    }

    @Test
    public void testCheckArgumentWithMessage_false() {
        try {
            checkArgument(false, TEST_MSG);
            fail("Expected IllegalArgumentException not thrown.");
        } catch (IllegalArgumentException ex) {
            assertEquals(TEST_MSG, ex.getMessage());
        }
    }

    @Test
    public void testCheckNotEmpty() {
        String testString = "I am not empty";
        assertSame(testString, checkNotEmpty(testString, TEST_MSG));
    }

    @Test(expected = NullPointerException.class)
    public void testCheckNotEmpty_nullString() {
        checkNotEmpty(null, TEST_MSG);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckNotEmpty_emptyString() {
        checkNotEmpty("", TEST_MSG);
    }

    @Test
    public void testCheckNullOrNotEmpty() {
        String testString = "I am not empty";
        assertSame(testString, checkNullOrNotEmpty(testString, TEST_MSG));
    }

    @Test
    public void testCheckNullOrNotEmpty_nullString() {
        assertNull(checkNullOrNotEmpty(null, TEST_MSG));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckNullOrNotEmpty_emptyString() {
        checkNullOrNotEmpty("", TEST_MSG);
    }

    @Test(expected = NullPointerException.class)
    public void testCheckCollectionNotEmpty_withNull() {
        checkCollectionNotEmpty(null, TEST_MSG);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckCollectionNotEmpty_withEmptyList() {
        checkCollectionNotEmpty(new ArrayList<Object>(), TEST_MSG);
    }

    @Test
    public void testCheckCollectionNotEmpty() {
        checkCollectionNotEmpty(Arrays.asList("value1", "value2"), TEST_MSG);
    }
}
