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

import static net.openid.appauth.TestValues.TEST_ACCESS_TOKEN;
import static net.openid.appauth.TestValues.TEST_APP_SCHEME;
import static net.openid.appauth.TestValues.TEST_AUTH_CODE;
import static net.openid.appauth.TestValues.TEST_ID_TOKEN;
import static net.openid.appauth.TestValues.TEST_STATE;
import static net.openid.appauth.TestValues.getTestAuthRequest;
import static net.openid.appauth.TestValues.getTestAuthRequestBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class RedirectUriReceiverActivityTest {

    private static final long TEST_START_TIME = 100L;
    private static final String TOKEN_EXPIRES_IN_SECS = "60";
    private static final Long TOKEN_EXPIRES_AT = 60100L;

    private static final Uri CODE_URI = new Uri.Builder()
            .scheme(TEST_APP_SCHEME)
            .appendQueryParameter(AuthorizationResponse.KEY_STATE, TEST_STATE)
            .appendQueryParameter(AuthorizationResponse.KEY_AUTHORIZATION_CODE, TEST_AUTH_CODE)
            .build();
    private static final Uri IMPLICIT_URI = new Uri.Builder()
            .scheme(TEST_APP_SCHEME)
            .appendQueryParameter(AuthorizationResponse.KEY_STATE, TEST_STATE)
            .appendQueryParameter(AuthorizationResponse.KEY_ACCESS_TOKEN, TEST_ACCESS_TOKEN)
            .appendQueryParameter(AuthorizationResponse.KEY_ID_TOKEN, TEST_ID_TOKEN)
            .appendQueryParameter(AuthorizationResponse.KEY_EXPIRES_IN, TOKEN_EXPIRES_IN_SECS)
            .appendQueryParameter(AuthorizationResponse.KEY_TOKEN_TYPE,
                    AuthorizationResponse.TOKEN_TYPE_BEARER)
            .build();


    private static final Intent CODE_INTENT;
    private static final Intent IMPLICIT_INTENT;

    static {
        CODE_INTENT = new Intent();
        CODE_INTENT.setData(CODE_URI);
        IMPLICIT_INTENT = new Intent();
        IMPLICIT_INTENT.setData(IMPLICIT_URI);
    }

    @Mock PendingIntent mPendingIntent;

    AuthorizationRequest mRequest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PendingIntentStore.getInstance().clearPendingIntents();
        mRequest = getTestAuthRequestBuilder()
                .setState(TEST_STATE)
                .build();
    }

    @After
    public void tearDown() {
        PendingIntentStore.getInstance().clearPendingIntents();
    }

    @Test
    public void testRedirectUriActivity_code() throws Exception {
        PendingIntentStore.getInstance().addPendingIntent(mRequest, mPendingIntent);
        RedirectUriReceiverActivity activity = Robolectric
                .buildActivity(RedirectUriReceiverActivity.class)
                .withIntent(CODE_INTENT)
                .create()
                .get();
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mPendingIntent).send(eq(activity), anyInt(), intentCaptor.capture());
        Intent resultIntent = intentCaptor.getValue();
        AuthorizationResponse response = AuthorizationResponse.fromIntent(resultIntent);
        assertEquals(TEST_STATE, response.state);
        assertEquals(TEST_AUTH_CODE, response.authorizationCode);
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testRedirectUriActivity_implicit() throws Exception {
        PendingIntentStore.getInstance().addPendingIntent(mRequest, mPendingIntent);
        ActivityController<RedirectUriReceiverActivity> controller =
                Robolectric.buildActivity(RedirectUriReceiverActivity.class)
                .withIntent(IMPLICIT_INTENT);
        RedirectUriReceiverActivity activity = controller.get();
        activity.setClock(new TestClock(TEST_START_TIME));
        controller.create();
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mPendingIntent).send(eq(activity), anyInt(), intentCaptor.capture());
        Intent resultIntent = intentCaptor.getValue();
        AuthorizationResponse response = AuthorizationResponse.fromIntent(resultIntent);
        assertEquals(TEST_STATE, response.state);
        assertEquals(TEST_ACCESS_TOKEN, response.accessToken);
        assertEquals(AuthorizationResponse.TOKEN_TYPE_BEARER, response.tokenType);
        assertEquals(TEST_ID_TOKEN, response.idToken);
        assertEquals(TOKEN_EXPIRES_AT, response.accessTokenExpirationTime);
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testRedirectUriActivity_missingPendingIntent() throws Exception {
        RedirectUriReceiverActivity activity = Robolectric
                .buildActivity(RedirectUriReceiverActivity.class)
                .withIntent(CODE_INTENT)
                .create()
                .get();
        // no pending intent found, redirect uri should be ignored and activity should finish
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testRedirectUriActivity_canceled() throws Exception {
        PendingIntentStore.getInstance().addPendingIntent(getTestAuthRequest(), mPendingIntent);
        doThrow(new PendingIntent.CanceledException()).when(mPendingIntent)
                .send(any(Context.class), anyInt(), any(Intent.class));
        RedirectUriReceiverActivity activity = Robolectric
                .buildActivity(RedirectUriReceiverActivity.class)
                .withIntent(CODE_INTENT)
                .create()
                .get();
        // exception thrown when trying to send pending intent, activity should finish
        assertTrue(activity.isFinishing());
    }
}
