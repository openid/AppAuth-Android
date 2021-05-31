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
import static androidx.test.ext.truth.content.IntentSubject.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import net.openid.appauth.AuthorizationException.AuthorizationRequestErrors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class AuthorizationManagementActivityTest {

    private Context mContext;
    private AuthorizationRequest mAuthRequest;
    private EndSessionRequest mEndSessionRequest;
    private Intent mAuthIntent;
    private Intent mCompleteIntent;
    private PendingIntent mCompletePendingIntent;
    private Intent mCancelIntent;
    private PendingIntent mCancelPendingIntent;
    private Intent mStartAuthIntentWithPendings;
    private Intent mStartAuthIntentWithPendingsWithoutCancel;
    private Intent mStartAuthForResultIntent;
    private Intent mEndSessionIntentWithPendings;
    private Intent mEndSessionIntentWithPendingsWithoutCancel;
    private Intent mEndSessionForResultIntent;
    private ActivityController<AuthorizationManagementActivity> mController;
    private AuthorizationManagementActivity mActivity;
    private ShadowActivity mActivityShadow;
    private Uri mSuccessAuthRedirect;
    private Uri mErrorAuthRedirect;
    private Uri mSuccessEndSessionRedirect;
    private Uri mErrorEndSessionRedirect;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();

        mAuthRequest = TestValues.getTestAuthRequest();
        mEndSessionRequest = TestValues.getTestEndSessionRequest();

        mAuthIntent = new Intent("AUTH");
        mCompleteIntent = new Intent("COMPLETE");
        mCompletePendingIntent =
                PendingIntent.getActivity(mContext, 0, mCompleteIntent, 0);
        mCancelIntent = new Intent("CANCEL");
        mCancelPendingIntent =
                PendingIntent.getActivity(mContext, 0, mCancelIntent, 0);

        mStartAuthIntentWithPendings =
                createStartIntentWithPendingIntents(mAuthRequest, mCancelPendingIntent);
        mStartAuthIntentWithPendingsWithoutCancel =
                createStartIntentWithPendingIntents(mAuthRequest, null);
        mStartAuthForResultIntent = createStartForResultIntent(mAuthRequest);

        mEndSessionIntentWithPendings =
            createStartIntentWithPendingIntents(mEndSessionRequest, mCancelPendingIntent);
        mEndSessionIntentWithPendingsWithoutCancel =
            createStartIntentWithPendingIntents(mEndSessionRequest, null);
        mEndSessionForResultIntent =
            createStartForResultIntent(mEndSessionRequest);

        mSuccessAuthRedirect = mAuthRequest.redirectUri.buildUpon()
                .appendQueryParameter(AuthorizationResponse.KEY_STATE, mAuthRequest.getState())
                .appendQueryParameter(AuthorizationResponse.KEY_AUTHORIZATION_CODE, "12345")
                .build();

        mSuccessEndSessionRedirect = mEndSessionRequest.postLogoutRedirectUri.buildUpon()
            .appendQueryParameter(EndSessionResponse.KEY_STATE, mEndSessionRequest.getState())
            .build();

        mErrorAuthRedirect = mAuthRequest.redirectUri.buildUpon()
                .appendQueryParameter(
                        AuthorizationException.PARAM_ERROR,
                        AuthorizationRequestErrors.ACCESS_DENIED.error)
                .appendQueryParameter(
                        AuthorizationException.PARAM_ERROR_DESCRIPTION,
                        AuthorizationRequestErrors.ACCESS_DENIED.errorDescription)
                .build();

        mErrorEndSessionRedirect = mEndSessionRequest.postLogoutRedirectUri.buildUpon()
            .appendQueryParameter(AuthorizationException.PARAM_ERROR,
                AuthorizationRequestErrors.ACCESS_DENIED.error)
            .appendQueryParameter(
                AuthorizationException.PARAM_ERROR_DESCRIPTION,
                AuthorizationRequestErrors.ACCESS_DENIED.errorDescription)
            .build();

        instantiateActivity(mStartAuthIntentWithPendings);
    }

    private Intent createStartIntentWithPendingIntents(
            AuthorizationManagementRequest authRequest,
            PendingIntent cancelIntent) {
        return AuthorizationManagementActivity.createStartIntent(
                mContext,
                authRequest,
                mAuthIntent,
                mCompletePendingIntent,
                cancelIntent);
    }

    private Intent createStartForResultIntent(
        AuthorizationManagementRequest authRequest) {
        return AuthorizationManagementActivity.createStartForResultIntent(
                mContext,
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
    public void testLoginSuccessFlow_withPendingIntentsAndWithoutDestroy_shouldSendCompleteIntent() {
        // start the flow
        instantiateActivity(mStartAuthIntentWithPendings);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // the management activity will be paused while the authorization flow is running.
        // if there is no memory pressure, the activity will remain in the paused state.
        mController.pause();

        // on completion of the authorization activity, the result will be forwarded to the
        // management activity via newIntent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                mContext,
                mSuccessAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // after which the completion intent should be fired
        Intent nextStartedActivity = mActivityShadow.getNextStartedActivity();
        assertThat(nextStartedActivity).hasAction("COMPLETE");
        assertThat(nextStartedActivity).hasData(mSuccessAuthRedirect);
        assertThat(nextStartedActivity).extras().containsKey(AuthorizationResponse.EXTRA_RESPONSE);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testEndOfSessionSuccessFlow_withPendingIntentsAndWithoutDestroy_shouldSendCompleteIntent() {
        // start the flow
        instantiateActivity(mEndSessionIntentWithPendings);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // the management activity will be paused while the authorization flow is running.
        // if there is no memory pressure, the activity will remain in the paused state.
        mController.pause();

        // on completion of the authorization activity, the result will be forwarded to the
        // management activity via newIntent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
            mContext,
            mSuccessEndSessionRedirect));

        // the management activity is then resumed
        mController.resume();

        // after which the completion intent should be fired
        Intent nextStartedActivity = mActivityShadow.getNextStartedActivity();
        assertThat(nextStartedActivity).hasAction("COMPLETE");
        assertThat(nextStartedActivity).hasData(mSuccessEndSessionRedirect);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testLoginSuccessFlow_withPendingIntentsAndNoState() {
        AuthorizationRequest request = TestValues.getTestAuthRequestBuilder()
            .setState(null)
            .build();

        emulateFlowToAuthorizationActivityLaunch(
            createStartIntentWithPendingIntents(request, mCancelPendingIntent));

        Uri successAuthRedirect = mAuthRequest.redirectUri.buildUpon()
            .appendQueryParameter(AuthorizationResponse.KEY_AUTHORIZATION_CODE, "12345")
            .build();

        Intent nextStartedActivity = emulateAuthorizationResponseReceived(
            AuthorizationManagementActivity.createResponseHandlingIntent(
                mContext,
                successAuthRedirect));

        // after which the completion intent should be fired
        assertThat(nextStartedActivity).hasAction("COMPLETE");
        assertThat(nextStartedActivity).hasData(successAuthRedirect);
        assertThat(nextStartedActivity).extras()
            .containsKey(AuthorizationResponse.EXTRA_RESPONSE);
        assertThat(nextStartedActivity).extras()
            .doesNotContainKey(AuthorizationException.EXTRA_EXCEPTION);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testEndSessionSuccessFlow_withPendingIntentsAndNoState() {
        EndSessionRequest request = TestValues.getTestEndSessionRequestBuilder()
            .setState(null)
            .build();

        emulateFlowToAuthorizationActivityLaunch(
            createStartIntentWithPendingIntents(request, mCancelPendingIntent));

        Uri authResponseUri = request.postLogoutRedirectUri;

        Intent nextStartedActivity = emulateAuthorizationResponseReceived(
            AuthorizationManagementActivity.createResponseHandlingIntent(
                mContext,
                authResponseUri));

        // after which the completion intent should be fired
        assertThat(nextStartedActivity).hasAction("COMPLETE");
        assertThat(nextStartedActivity).hasData(authResponseUri);
        assertThat(nextStartedActivity).extras()
                .doesNotContainKey(AuthorizationException.EXTRA_EXCEPTION);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testLoginSuccessFlow_withoutPendingIntentsAndWithoutDestroy_shouldSetResult() {
        // start the flow
        instantiateActivity(mStartAuthForResultIntent);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // the management activity will be paused while the authorization flow is running.
        // if there is no memory pressure, the activity will remain in the paused state.
        mController.pause();

        // on completion of the authorization activity, the result will be forwarded to the
        // management activity via newIntent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                mContext,
                mSuccessAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // and then sets a result before finishing as there is no completion intent
        assertThat(mActivityShadow.getResultCode()).isEqualTo(RESULT_OK);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testEndSessionSuccessFlow_withoutPendingIntentsAndWithoutDestroy_shouldSetResult() {
        // start the flow
        instantiateActivity(mEndSessionForResultIntent);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // the management activity will be paused while the authorization flow is running.
        // if there is no memory pressure, the activity will remain in the paused state.
        mController.pause();

        // on completion of the authorization activity, the result will be forwarded to the
        // management activity via newIntent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
            mContext,
            mSuccessEndSessionRedirect));

        // the management activity is then resumed
        mController.resume();

        // and then sets a result before finishing as there is no completion intent
        assertThat(mActivityShadow.getResultCode()).isEqualTo(RESULT_OK);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testLoginSuccessFlow_withPendingIntentsAndWithDestroy_shouldSendCompleteIntent() {
        // start the flow
        instantiateActivity(mStartAuthIntentWithPendings);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // on a device under memory pressure, the management activity will be destroyed, but
        // it will be able to save its state
        Bundle savedState = new Bundle();
        mController.pause().stop().saveInstanceState(savedState).destroy();

        // on completion of the authorization activity, a new management activity will be created
        instantiateActivity(mStartAuthIntentWithPendings);
        mController.create(savedState).start();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                mContext,
                mSuccessAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // after which the completion intent should be fired
        Intent nextStartedActivity = mActivityShadow.getNextStartedActivity();
        assertThat(nextStartedActivity).hasAction("COMPLETE");
        assertThat(nextStartedActivity).hasData(mSuccessAuthRedirect);
        assertThat(nextStartedActivity).extras().containsKey(AuthorizationResponse.EXTRA_RESPONSE);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testEndSessionSuccessFlow_withPendingIntentsAndWithDestroy_shouldSendCompleteIntent() {
        // start the flow
        instantiateActivity(mEndSessionIntentWithPendings);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // on a device under memory pressure, the management activity will be destroyed, but
        // it will be able to save its state
        Bundle savedState = new Bundle();
        mController.pause().stop().saveInstanceState(savedState).destroy();

        // on completion of the authorization activity, a new management activity will be created
        instantiateActivity(mStartAuthIntentWithPendings);
        mController.create(savedState).start();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
            mContext,
            mSuccessAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // after which the completion intent should be fired
        Intent nextStartedActivity = mActivityShadow.getNextStartedActivity();
        assertThat(nextStartedActivity).hasAction("COMPLETE");
        assertThat(nextStartedActivity).hasData(mSuccessAuthRedirect);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testLoginSuccessFlow_withoutPendingIntentsAndWithDestroy_shouldSetResult() {
        // start the flow
        instantiateActivity(mStartAuthForResultIntent);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // on a device under memory pressure, the management activity will be destroyed, but
        // it will be able to save its state
        Bundle savedState = new Bundle();
        mController.pause().stop().saveInstanceState(savedState).destroy();

        // on completion of the authorization activity, a new management activity will be created
        instantiateActivity(mStartAuthIntentWithPendings);
        mController.create(savedState).start();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                mContext,
                mSuccessAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // and then sets a result before finishing as there is no completion intent
        assertThat(mActivityShadow.getResultCode()).isEqualTo(RESULT_OK);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testEndSessionSuccessFlow_withoutPendingIntentsAndWithDestroy_shouldSetResult() {
        // start the flow
        instantiateActivity(mEndSessionForResultIntent);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // on a device under memory pressure, the management activity will be destroyed, but
        // it will be able to save its state
        Bundle savedState = new Bundle();
        mController.pause().stop().saveInstanceState(savedState).destroy();

        // on completion of the authorization activity, a new management activity will be created
        instantiateActivity(mStartAuthIntentWithPendings);
        mController.create(savedState).start();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
            mContext,
            mSuccessEndSessionRedirect));

        // the management activity is then resumed
        mController.resume();

        // and then sets a result before finishing as there is no completion intent
        assertThat(mActivityShadow.getResultCode()).isEqualTo(RESULT_OK);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testLoginFailureFlow_withPendingIntentsAndWithoutDestroy_shouldSendCompleteIntent() {
        // start the flow
        instantiateActivity(mStartAuthIntentWithPendings);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // the management activity will be paused while the authorization flow is running.
        // if there is no memory pressure, the activity will remain in the paused state.
        mController.pause();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                mContext,
                mErrorAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // after which the completion intent should be fired
        Intent nextStartedActivity = mActivityShadow.getNextStartedActivity();
        assertThat(nextStartedActivity).hasAction("COMPLETE");
        assertThat(nextStartedActivity).hasData(mErrorAuthRedirect);
        assertThat(nextStartedActivity).extras().containsKey(AuthorizationException.EXTRA_EXCEPTION);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testEndSessionFailureFlow_withPendingIntentsAndWithoutDestroy_shouldSendCompleteIntent() {
        // start the flow
        instantiateActivity(mEndSessionIntentWithPendings);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // the management activity will be paused while the authorization flow is running.
        // if there is no memory pressure, the activity will remain in the paused state.
        mController.pause();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
            mContext,
            mErrorEndSessionRedirect));

        // the management activity is then resumed
        mController.resume();

        // after which the completion intent should be fired
        Intent nextStartedActivity = mActivityShadow.getNextStartedActivity();
        assertThat(nextStartedActivity).hasAction("COMPLETE");
        assertThat(nextStartedActivity).hasData(mErrorEndSessionRedirect);
        assertThat(nextStartedActivity).extras().containsKey(AuthorizationException.EXTRA_EXCEPTION);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testLoginFailureFlow_withoutPendingIntentsAndWithoutDestroy_shouldSetResult() {
        // start the flow
        instantiateActivity(mStartAuthForResultIntent);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // the management activity will be paused while the authorization flow is running.
        // if there is no memory pressure, the activity will remain in the paused state.
        mController.pause();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                mContext,
                mErrorAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // after which the completion intent should be fired
        assertThat(mActivityShadow.getResultCode()).isEqualTo(RESULT_OK);
        Intent resultIntent = mActivityShadow.getResultIntent();
        assertThat(resultIntent.getData()).isEqualTo(mErrorAuthRedirect);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testEndSessionFailureFlow_withoutPendingIntentsAndWithoutDestroy_shouldSetResult() {
        // start the flow
        instantiateActivity(mEndSessionForResultIntent);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("AUTH");

        // the management activity will be paused while the authorization flow is running.
        // if there is no memory pressure, the activity will remain in the paused state.
        mController.pause();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
            mContext,
            mErrorAuthRedirect));

        // the management activity is then resumed
        mController.resume();

        // after which the completion intent should be fired
        assertThat(mActivityShadow.getResultCode()).isEqualTo(RESULT_OK);
        Intent resultIntent = mActivityShadow.getResultIntent();
        assertThat(resultIntent.getData()).isEqualTo(mErrorEndSessionRedirect);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testLoginMismatchedState_withPendingIntentsAndResponseDiffersFromRequest() {
        emulateFlowToAuthorizationActivityLaunch(mStartAuthIntentWithPendings);

        Uri authResponseUri = mAuthRequest.redirectUri.buildUpon()
                .appendQueryParameter(AuthorizationResponse.KEY_STATE, "differentState")
                .appendQueryParameter(AuthorizationResponse.KEY_AUTHORIZATION_CODE, "12345")
                .build();

        Intent nextStartedActivity = emulateAuthorizationResponseReceived(
                AuthorizationManagementActivity.createResponseHandlingIntent(
                        mContext,
                        authResponseUri));

        // the next activity should be from the completion intent, carrying an error
        assertThat(nextStartedActivity).hasAction("COMPLETE");
        assertThat(nextStartedActivity).hasData(authResponseUri);
        assertThat(nextStartedActivity).extras().containsKey(AuthorizationException.EXTRA_EXCEPTION);

        assertThat(AuthorizationException.fromIntent(nextStartedActivity))
                .isEqualTo(AuthorizationRequestErrors.STATE_MISMATCH);
    }

    @Test
    public void testEndSessionnMismatchedState_withPendingIntentsAndResponseDiffersFromRequest() {
        emulateFlowToAuthorizationActivityLaunch(mEndSessionIntentWithPendings);

        Uri authResponseUri = mEndSessionRequest.postLogoutRedirectUri.buildUpon()
            .appendQueryParameter(EndSessionResponse.KEY_STATE, "differentState")
            .build();

        Intent nextStartedActivity = emulateAuthorizationResponseReceived(
            AuthorizationManagementActivity.createResponseHandlingIntent(
                mContext,
                authResponseUri));

        // the next activity should be from the completion intent, carrying an error
        assertThat(nextStartedActivity).hasAction("COMPLETE");
        assertThat(nextStartedActivity).hasData(authResponseUri);
        assertThat(nextStartedActivity).extras().containsKey(AuthorizationException.EXTRA_EXCEPTION);

        assertThat(AuthorizationException.fromIntent(nextStartedActivity))
            .isEqualTo(AuthorizationRequestErrors.STATE_MISMATCH);
    }

    @Test
    public void testLoginMismatchedState_withoutPendingIntentsAndResponseDiffersFromRequest() {
        emulateFlowToAuthorizationActivityLaunch(mStartAuthForResultIntent);

        Uri authResponseUri = mAuthRequest.redirectUri.buildUpon()
                .appendQueryParameter(AuthorizationResponse.KEY_STATE, "differentState")
                .appendQueryParameter(AuthorizationResponse.KEY_AUTHORIZATION_CODE, "12345")
                .build();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
                mContext,
                authResponseUri));

        // the management activity is then resumed
        mController.resume();

        // no completion intent, so exception should be passed to calling activity
        // via the result intent supplied to setResult
        Intent resultIntent = mActivityShadow.getResultIntent();

        assertThat(resultIntent).hasData(authResponseUri);
        assertThat(resultIntent).extras().containsKey(AuthorizationException.EXTRA_EXCEPTION);

        assertThat(AuthorizationException.fromIntent(resultIntent))
                .isEqualTo(AuthorizationRequestErrors.STATE_MISMATCH);
    }

    @Test
    public void testEndSessionMismatchedState_withoutPendingIntentsAndResponseDiffersFromRequest() {
        emulateFlowToAuthorizationActivityLaunch(mEndSessionForResultIntent);

        Uri authResponseUri = mEndSessionRequest.postLogoutRedirectUri.buildUpon()
            .appendQueryParameter(AuthorizationResponse.KEY_STATE, "differentState")
            .build();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
            mContext,
            authResponseUri));

        // the management activity is then resumed
        mController.resume();

        // no completion intent, so exception should be passed to calling activity
        // via the result intent supplied to setResult
        Intent resultIntent = mActivityShadow.getResultIntent();

        assertThat(resultIntent).hasData(authResponseUri);
        assertThat(resultIntent).extras().containsKey(AuthorizationException.EXTRA_EXCEPTION);

        assertThat(AuthorizationException.fromIntent(resultIntent))
            .isEqualTo(AuthorizationRequestErrors.STATE_MISMATCH);
    }

    @Test
    public void testLoginMismatchedState_withPendingIntentsAndNoStateInRequestWithStateInResponse() {
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
                        mContext,
                        authResponseUri));

        assertThat(AuthorizationException.fromIntent(nextStartedActivity))
                .isEqualTo(AuthorizationRequestErrors.STATE_MISMATCH);
    }

    @Test
    public void testEndSessionMismatchedState_withPendingIntentsAndNoStateInRequestWithStateInResponse() {
        EndSessionRequest request = new EndSessionRequest.Builder(TestValues.getTestServiceConfig())
            .setIdTokenHint(TestValues.TEST_ID_TOKEN)
            .setPostLogoutRedirectUri(TestValues.TEST_APP_REDIRECT_URI)
            .setState("state")
            .build();

        Intent startIntent = createStartIntentWithPendingIntents(request, mCancelPendingIntent);
        emulateFlowToAuthorizationActivityLaunch(startIntent);

        Uri authResponseUri = mEndSessionRequest.postLogoutRedirectUri.buildUpon()
            .appendQueryParameter(AuthorizationResponse.KEY_STATE, "differentState")
            .build();

        // the next activity should be from the completion intent, carrying an error
        Intent nextStartedActivity = emulateAuthorizationResponseReceived(
            AuthorizationManagementActivity.createResponseHandlingIntent(
                mContext,
                authResponseUri));

        assertThat(AuthorizationException.fromIntent(nextStartedActivity))
            .isEqualTo(AuthorizationRequestErrors.STATE_MISMATCH);
    }

    @Test
    public void testLoginMismatchedState_withoutPendingIntentsAndNoStateInRequestWithStateInResponse() {
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
                mContext,
                authResponseUri));

        // the management activity is then resumed
        mController.resume();

        Intent resultIntent = mActivityShadow.getResultIntent();
        assertThat(AuthorizationException.fromIntent(resultIntent))
                .isEqualTo(AuthorizationRequestErrors.STATE_MISMATCH);
    }

    @Test
    public void testEndSessionMismatchedState_withoutPendingIntentsAndNoStateInRequestWithStateInResponse() {
        EndSessionRequest request = new EndSessionRequest.Builder(TestValues.getTestServiceConfig())
            .setIdTokenHint(TestValues.TEST_ID_TOKEN)
            .setPostLogoutRedirectUri(TestValues.TEST_APP_REDIRECT_URI)
            .setState("state")
            .build();

        Intent startIntent = createStartForResultIntent(request);
        emulateFlowToAuthorizationActivityLaunch(startIntent);

        Uri authResponseUri = mEndSessionRequest.postLogoutRedirectUri.buildUpon()
            .appendQueryParameter(AuthorizationResponse.KEY_STATE, "differentState")
            .build();

        // the authorization redirect will be forwarded via a new intent
        mController.newIntent(AuthorizationManagementActivity.createResponseHandlingIntent(
            mContext,
            authResponseUri));

        // the management activity is then resumed
        mController.resume();

        Intent resultIntent = mActivityShadow.getResultIntent();
        assertThat(AuthorizationException.fromIntent(resultIntent))
            .isEqualTo(AuthorizationRequestErrors.STATE_MISMATCH);
    }

    @Test
    public void testLoginCancelFlow_withPendingIntentsAndWithoutDestroy() {
        // start the flow
        instantiateActivity(mStartAuthIntentWithPendings);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).isNotNull();

        // the management activity will be paused while this auth intent is running
        mController.pause();

        // when the user cancels the auth intent, the management activity will be resumed
        mController.resume();

        // at which point the cancel intent should be fired
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("CANCEL");
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testEndSessionCancelFlow_withPendingIntentsAndWithoutDestroy() {
        // start the flow
        instantiateActivity(mEndSessionIntentWithPendings);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).isNotNull();

        // the management activity will be paused while this auth intent is running
        mController.pause();

        // when the user cancels the auth intent, the management activity will be resumed
        mController.resume();

        // at which point the cancel intent should be fired
        assertThat(mActivityShadow.getNextStartedActivity()).hasAction("CANCEL");
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testLoginCancelFlow_withoutPendingIntentsAndWithoutDestroy() {
        // start the flow
        instantiateActivity(mStartAuthForResultIntent);
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
        assertThat(resultIntent).extras().containsKey(AuthorizationException.EXTRA_EXCEPTION);

        assertThat(AuthorizationException.fromIntent(resultIntent))
            .isEqualTo(AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testEndSessionCancelFlow_withoutPendingIntentsAndWithoutDestroy() {
        // start the flow
        instantiateActivity(mEndSessionForResultIntent);
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
        assertThat(resultIntent).extras().containsKey(AuthorizationException.EXTRA_EXCEPTION);

        assertThat(AuthorizationException.fromIntent(resultIntent))
            .isEqualTo(AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testLoginCancelFlow_withCompletionIntentButNoCancelIntent() {
        // start the flow
        instantiateActivity(mStartAuthIntentWithPendingsWithoutCancel);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).isNotNull();

        // the management activity will be paused while this auth intent is running
        mController.pause();

        // when the user cancels the auth intent, the management activity will be resumed
        mController.resume();

        // as there is no cancel intent, the activity simply finishes
        assertThat(mActivityShadow.getNextStartedActivity()).isNull();
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testEndSessionCancelFlow_withCompletionIntentButNoCancelIntent() {
        // start the flow
        instantiateActivity(mEndSessionIntentWithPendingsWithoutCancel);
        mController.create().start().resume();

        // an activity should be started for auth
        assertThat(mActivityShadow.getNextStartedActivity()).isNotNull();

        // the management activity will be paused while this auth intent is running
        mController.pause();

        // when the user cancels the auth intent, the management activity will be resumed
        mController.resume();

        // as there is no cancel intent, the activity simply finishes
        assertThat(mActivityShadow.getNextStartedActivity()).isNull();
        assertThat(mActivity.isFinishing()).isTrue();
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
