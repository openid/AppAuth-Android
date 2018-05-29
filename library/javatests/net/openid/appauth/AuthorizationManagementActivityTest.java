/*
 * Copyright 2016 The AppAuth for Android Authors. All Rights Reserved.
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

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import net.openid.appauth.AuthorizationException.AuthorizationRequestErrors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=16)
public class AuthorizationManagementActivityTest {

    private AuthorizationRequest mAuthRequest;
    private Intent mAuthIntent;
    private Intent mCompleteIntent;
    private PendingIntent mCompletePendingIntent;
    private Intent mCancelIntent;
    private PendingIntent mCancelPendingIntent;
    private Intent mStartIntentWithPendings;
    private Intent mStartIntentWithPendingsWithoutCancel;
    private Intent mStartForResultIntent;
    private ActivityController<AuthorizationManagementActivity> mController;
    private AuthorizationManagementActivity mActivity;
    private ShadowActivity mActivityShadow;
    private Uri mSuccessAuthRedirect;
    private Uri mErrorAuthRedirect;

    @Before
    public void setUp() {
        mAuthRequest = TestValues.getTestAuthRequest();

        mAuthIntent = new Intent("AUTH");
        mCompleteIntent = new Intent("COMPLETE");
        mCompletePendingIntent =
                PendingIntent.getActivity(RuntimeEnvironment.application, 0, mCompleteIntent, 0);
        mCancelIntent = new Intent("CANCEL");
        mCancelPendingIntent =
                PendingIntent.getActivity(RuntimeEnvironment.application, 0, mCancelIntent, 0);

        mStartIntentWithPendings =
                createStartIntentWithPendingIntents(mAuthRequest, mCancelPendingIntent);
        mStartIntentWithPendingsWithoutCancel =
                createStartIntentWithPendingIntents(mAuthRequest, null);
        mStartForResultIntent = createStartForResultIntent(mAuthRequest);

        mSuccessAuthRedirect = mAuthRequest.redirectUri.buildUpon()
                .appendQueryParameter(AuthorizationResponse.KEY_STATE, mAuthRequest.state)
                .appendQueryParameter(AuthorizationResponse.KEY_AUTHORIZATION_CODE, "12345")
                .build();

        mErrorAuthRedirect = mAuthRequest.redirectUri.buildUpon()
                .appendQueryParameter(
                        AuthorizationException.PARAM_ERROR,
                        AuthorizationRequestErrors.ACCESS_DENIED.error)
                .appendQueryParameter(
                        AuthorizationException.PARAM_ERROR_DESCRIPTION,
                        AuthorizationRequestErrors.ACCESS_DENIED.errorDescription)
                .build();

        instantiateActivity(mStartIntentWithPendings);
    }

    private Intent createStartIntentWithPendingIntents(
            AuthorizationRequest authRequest,
            PendingIntent cancelIntent) {
        return AuthorizationManagementActivity.createStartIntent(
                RuntimeEnvironment.application,
                authRequest,
                mAuthIntent,
                mCompletePendingIntent,
                cancelIntent);
    }

    private Intent createStartForResultIntent(
            AuthorizationRequest authRequest) {
        return AuthorizationManagementActivity.createStartForResultIntent(
                RuntimeEnvironment.application,
                authRequest,
                mAuthIntent);
    }

    private void instantiateActivity(Intent managementIntent) {
        mController = Robolectric.buildActivity(
                AuthorizationManagementActivity.class,
                managementIntent);

        mActivity = mController.get();
        mActivityShadow = shadowOf(mActivity);
    }

    @Test
    public void testSuccessFlow_withPendingIntentsAndWithoutDestroy_shouldSendCompleteIntent() {
        // start the flow
        instantiateActivity(mStartIntentWithPendings);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // the management activity will be paused while the authorization flow is running.
        // if there is no memory pressure, the activity will remain in the paused state.
        mController.pause();

        // on completion of the authorization activity, the result will be forwarded to the
        // management activity via newIntent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                RuntimeEnvironment.application,
                mSuccessAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // after which the completion intent should be fired
        assertThat(mActivityShadow.getNextStartedActivity())
                .hasAction("COMPLETE")
                .hasData(mSuccessAuthRedirect)
                .hasExtra(AuthorizationResponse.EXTRA_RESPONSE);
        assertThat(mActivity).isFinishing();
    }

    @Test
    public void testSuccessFlow_withoutPendingIntentsAndWithoutDestroy_shouldSetResult() {
        // start the flow
        instantiateActivity(mStartForResultIntent);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // the management activity will be paused while the authorization flow is running.
        // if there is no memory pressure, the activity will remain in the paused state.
        mController.pause();

        // on completion of the authorization activity, the result will be forwarded to the
        // management activity via newIntent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                RuntimeEnvironment.application,
                mSuccessAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // and then sets a result before finishing as there is no completion intent
        assertThat(mActivityShadow.getResultCode()).isEqualTo(RESULT_OK);
        assertThat(mActivity).isFinishing();
    }

    @Test
    public void testSuccessFlow_withPendingIntentsAndWithDestroy_shouldSendCompleteIntent() {
        // start the flow
        instantiateActivity(mStartIntentWithPendings);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // on a device under memory pressure, the management activity will be destroyed, but
        // it will be able to save its state
        Bundle savedState = new Bundle();
        mController.pause().stop().saveInstanceState(savedState).destroy();

        // on completion of the authorization activity, a new management activity will be created
        instantiateActivity(mStartIntentWithPendings);
        mController.create(savedState).start();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                RuntimeEnvironment.application,
                mSuccessAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // after which the completion intent should be fired
        assertThat(mActivityShadow.getNextStartedActivity())
                .hasAction("COMPLETE")
                .hasData(mSuccessAuthRedirect)
                .hasExtra(AuthorizationResponse.EXTRA_RESPONSE);
        assertThat(mActivity).isFinishing();
    }

    @Test
    public void testSuccessFlow_withoutPendingIntentsAndWithDestroy_shouldSetResult() {
        // start the flow
        instantiateActivity(mStartForResultIntent);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // on a device under memory pressure, the management activity will be destroyed, but
        // it will be able to save its state
        Bundle savedState = new Bundle();
        mController.pause().stop().saveInstanceState(savedState).destroy();

        // on completion of the authorization activity, a new management activity will be created
        instantiateActivity(mStartIntentWithPendings);
        mController.create(savedState).start();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                RuntimeEnvironment.application,
                mSuccessAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // and then sets a result before finishing as there is no completion intent
        assertThat(mActivityShadow.getResultCode()).isEqualTo(RESULT_OK);
        assertThat(mActivity).isFinishing();
    }

    @Test
    public void testFailureFlow_withPendingIntentsAndWithoutDestroy_shouldSendCompleteIntent() {
        // start the flow
        instantiateActivity(mStartIntentWithPendings);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // the management activity will be paused while the authorization flow is running.
        // if there is no memory pressure, the activity will remain in the paused state.
        mController.pause();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                RuntimeEnvironment.application,
                mErrorAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // after which the completion intent should be fired
        assertThat(mActivityShadow.getNextStartedActivity())
                .hasAction("COMPLETE")
                .hasData(mErrorAuthRedirect)
                .hasExtra(AuthorizationException.EXTRA_EXCEPTION);
        assertThat(mActivity).isFinishing();
    }

    @Test
    public void testFailureFlow_withoutPendingIntentsAndWithoutDestroy_shouldSetResult() {
        // start the flow
        instantiateActivity(mStartForResultIntent);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // the management activity will be paused while the authorization flow is running.
        // if there is no memory pressure, the activity will remain in the paused state.
        mController.pause();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                RuntimeEnvironment.application,
                mErrorAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // after which the completion intent should be fired
        assertThat(mActivityShadow.getResultCode()).isEqualTo(RESULT_OK);
        Intent resultIntent = mActivityShadow.getResultIntent();
        assertThat(resultIntent.getData()).isEqualTo(mErrorAuthRedirect);
        assertThat(mActivity).isFinishing();
    }

    @Test
    public void testMismatchedState_withPendingIntentsAndResponseDiffersFromRequest() {
        emulateFlowToAuthorizationActivityLaunch(mStartIntentWithPendings);

        Uri authResponseUri = mAuthRequest.redirectUri.buildUpon()
                .appendQueryParameter(AuthorizationResponse.KEY_STATE, "differentState")
                .appendQueryParameter(AuthorizationResponse.KEY_AUTHORIZATION_CODE, "12345")
                .build();

        Intent nextStartedActivity = emulateAuthorizationResponseReceived(
                AuthorizationManagementActivity.createResponseHandlingIntent(
                        RuntimeEnvironment.application,
                        authResponseUri));

        // the next activity should be from the completion intent, carrying an error
        assertThat(nextStartedActivity)
                .hasAction("COMPLETE")
                .hasData(authResponseUri)
                .hasExtra(AuthorizationException.EXTRA_EXCEPTION);

        assertThat(AuthorizationException.fromIntent(nextStartedActivity))
                .isEqualTo(AuthorizationRequestErrors.STATE_MISMATCH);
    }

    @Test
    public void testMismatchedState_withoutPendingIntentsAndResponseDiffersFromRequest() {
        emulateFlowToAuthorizationActivityLaunch(mStartForResultIntent);

        Uri authResponseUri = mAuthRequest.redirectUri.buildUpon()
                .appendQueryParameter(AuthorizationResponse.KEY_STATE, "differentState")
                .appendQueryParameter(AuthorizationResponse.KEY_AUTHORIZATION_CODE, "12345")
                .build();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                RuntimeEnvironment.application,
                authResponseUri));

        // the management activity is then resumed
        mController.resume();

        // no completion intent, so exception should be passed to calling activity
        // via the result intent supplied to setResult
        Intent resultIntent = mActivityShadow.getResultIntent();

        assertThat(resultIntent)
                .hasData(authResponseUri)
                .hasExtra(AuthorizationException.EXTRA_EXCEPTION);

        assertThat(AuthorizationException.fromIntent(resultIntent))
                .isEqualTo(AuthorizationRequestErrors.STATE_MISMATCH);
    }

    @Test
    public void testMismatchedState_withPendingIntentsAndNoStateInRequestWithStateInResponse() {
        AuthorizationRequest request = new AuthorizationRequest.Builder(
                TestValues.getTestServiceConfig(),
                TestValues.TEST_CLIENT_ID,
                ResponseTypeValues.CODE,
                TestValues.TEST_APP_REDIRECT_URI)
                .setState(null)
                .build();

        Intent startIntent = createStartIntentWithPendingIntents(request, mCancelPendingIntent);
        emulateFlowToAuthorizationActivityLaunch(startIntent);

        Uri authResponseUri = mAuthRequest.redirectUri.buildUpon()
                .appendQueryParameter(AuthorizationResponse.KEY_STATE, "differentState")
                .appendQueryParameter(AuthorizationResponse.KEY_AUTHORIZATION_CODE, "12345")
                .build();

        // the next activity should be from the completion intent, carrying an error
        Intent nextStartedActivity = emulateAuthorizationResponseReceived(
                AuthorizationManagementActivity.createResponseHandlingIntent(
                        RuntimeEnvironment.application,
                        authResponseUri));

        assertThat(AuthorizationException.fromIntent(nextStartedActivity))
                .isEqualTo(AuthorizationRequestErrors.STATE_MISMATCH);
    }

    @Test
    public void testMismatchedState_withoutPendingIntentsAndNoStateInRequestWithStateInResponse() {
        AuthorizationRequest request = new AuthorizationRequest.Builder(
                TestValues.getTestServiceConfig(),
                TestValues.TEST_CLIENT_ID,
                ResponseTypeValues.CODE,
                TestValues.TEST_APP_REDIRECT_URI)
                .setState(null)
                .build();

        Intent startIntent = createStartForResultIntent(request);
        emulateFlowToAuthorizationActivityLaunch(startIntent);

        Uri authResponseUri = mAuthRequest.redirectUri.buildUpon()
                .appendQueryParameter(AuthorizationResponse.KEY_STATE, "differentState")
                .appendQueryParameter(AuthorizationResponse.KEY_AUTHORIZATION_CODE, "12345")
                .build();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                RuntimeEnvironment.application,
                authResponseUri));

        // the management activity is then resumed
        mController.resume();

        Intent resultIntent = mActivityShadow.getResultIntent();
        assertThat(AuthorizationException.fromIntent(resultIntent))
                .isEqualTo(AuthorizationRequestErrors.STATE_MISMATCH);
    }

    @Test
    public void testInvalidResponse() {
        emulateFlowToAuthorizationActivityLaunch(mStartIntentWithPendings);

        Uri authResponseUri = mAuthRequest.redirectUri.buildUpon()
                .appendQueryParameter(AuthorizationResponse.KEY_STATE, "differentState")
                .appendQueryParameter(AuthorizationResponse.KEY_AUTHORIZATION_CODE, "12345")
                .build();
    }

    @Test
    public void testCancelFlow_withPendingIntentsAndWithoutDestroy() {
        // start the flow
        instantiateActivity(mStartIntentWithPendings);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).isNotNull();

        // the management activity will be paused while this auth intent is running
        mController.pause();

        // when the user cancels the auth intent, the management activity will be resumed
        mController.resume();

        // at which point the cancel intent should be fired
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("CANCEL");
        assertThat(mActivity).isFinishing();
    }

    @Test
    public void testCancelFlow_withoutPendingIntentsAndWithoutDestroy() {
        // start the flow
        instantiateActivity(mStartForResultIntent);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).isNotNull();

        // the management activity will be paused while this auth intent is running
        mController.pause();

        // when the user cancels the auth intent, the management activity will be resumed
        mController.resume();

        // at which point the cancel intent should be fired
        assertThat(mActivityShadow.getResultCode()).isEqualTo(RESULT_CANCELED);

        Intent resultIntent = mActivityShadow.getResultIntent();
        assertThat(resultIntent).hasExtra(AuthorizationException.EXTRA_EXCEPTION);

        assertThat(AuthorizationException.fromIntent(resultIntent))
            .isEqualTo(AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW);

        assertThat(mActivity).isFinishing();
    }

    @Test
    public void testCancelFlow_withCompletionIntentButNoCancelIntent() {
        // start the flow
        instantiateActivity(mStartIntentWithPendingsWithoutCancel);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).isNotNull();

        // the management activity will be paused while this auth intent is running
        mController.pause();

        // when the user cancels the auth intent, the management activity will be resumed
        mController.resume();

        // as there is no cancel intent, the activity simply finishes
        assertThat(mActivityShadow.getNextStartedActivity()).isNull();
        assertThat(mActivity).isFinishing();
    }

    private void emulateFlowToAuthorizationActivityLaunch(Intent startIntent) {
        // start the flow
        instantiateActivity(startIntent);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // the management activity will be paused while the authorization flow is running.
        // if there is no memory pressure, the activity will remain in the paused state.
        mController.pause();
    }

    private Intent emulateAuthorizationResponseReceived(Intent relaunchIntent) {
        if (relaunchIntent != null) {
            // the authorization redirect will be forwarded via a new intent
            mController.newIntent(relaunchIntent);
        }

        // the management activity is then resumed
        mController.resume();

        return mActivityShadow.getNextStartedActivity();
    }
}
