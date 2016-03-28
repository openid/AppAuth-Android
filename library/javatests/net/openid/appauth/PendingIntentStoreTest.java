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

import static net.openid.appauth.TestValues.getTestAuthRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.PendingIntent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class PendingIntentStoreTest {
    @Mock private PendingIntent mPendingIntent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PendingIntentStore.getInstance().clearPendingIntents();
    }

    @After
    public void tearDown() {
        PendingIntentStore.getInstance().clearPendingIntents();
    }

    @Test
    public void testAddGetPendingIntent() {
        AuthorizationRequest authRequest = getTestAuthRequest();
        PendingIntentStore.getInstance().addPendingIntent(authRequest, mPendingIntent);
        assertEquals(mPendingIntent,
                PendingIntentStore.getInstance().getPendingIntent(authRequest.state));
    }

    @Test
    public void testGetNonExistingPendingIntent() {
        assertNull(PendingIntentStore.getInstance().getPendingIntent("unknown"));
    }

    @Test
    public void testAddGetPendingIntentTwice() {
        AuthorizationRequest authRequest = getTestAuthRequest();
        PendingIntentStore.getInstance().addPendingIntent(authRequest, mPendingIntent);
        assertEquals(mPendingIntent,
                PendingIntentStore.getInstance().getPendingIntent(authRequest.state));
        assertNull(PendingIntentStore.getInstance().getPendingIntent(authRequest.state));
    }
}
