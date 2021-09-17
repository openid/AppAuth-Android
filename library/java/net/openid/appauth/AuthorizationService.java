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

import static net.openid.appauth.Preconditions.checkNotNull;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.browser.customtabs.CustomTabsIntent;

import net.openid.appauth.AuthorizationException.GeneralErrors;
import net.openid.appauth.AuthorizationException.RegistrationRequestErrors;
import net.openid.appauth.AuthorizationException.TokenRequestErrors;
import net.openid.appauth.IdToken.IdTokenException;
import net.openid.appauth.browser.BrowserDescriptor;
import net.openid.appauth.browser.BrowserSelector;
import net.openid.appauth.browser.CustomTabManager;
import net.openid.appauth.connectivity.ConnectionBuilder;
import net.openid.appauth.internal.Logger;
import net.openid.appauth.internal.UriUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Map;


/**
 * Dispatches requests to an OAuth2 authorization service. Note that instances of this class
 * _must be manually disposed_ when no longer required, to avoid leaks
 * (see {@link #dispose()}.
 */
public class AuthorizationService {

    @VisibleForTesting
    Context mContext;

    @NonNull
    private final AppAuthConfiguration mClientConfiguration;

    @NonNull
    private final CustomTabManager mCustomTabManager;

    @Nullable
    private final BrowserDescriptor mBrowser;

    private boolean mDisposed = false;

    /**
     * Creates an AuthorizationService instance, using the
     * {@link AppAuthConfiguration#DEFAULT default configuration}. Note that
     * instances of this class must be manually disposed when no longer required, to avoid
     * leaks (see {@link #dispose()}.
     */
    public AuthorizationService(@NonNull Context context) {
        this(context, AppAuthConfiguration.DEFAULT);
    }

    /**
     * Creates an AuthorizationService instance, using the specified configuration. Note that
     * instances of this class must be manually disposed when no longer required, to avoid
     * leaks (see {@link #dispose()}.
     */
    public AuthorizationService(
            @NonNull Context context,
            @NonNull AppAuthConfiguration clientConfiguration) {
        this(context,
                clientConfiguration,
                BrowserSelector.select(
                        context,
                        clientConfiguration.getBrowserMatcher()),
                new CustomTabManager(context));
    }

    /**
     * Constructor that injects a url builder into the service for testing.
     */
    @VisibleForTesting
    AuthorizationService(@NonNull Context context,
                         @NonNull AppAuthConfiguration clientConfiguration,
                         @Nullable BrowserDescriptor browser,
                         @NonNull CustomTabManager customTabManager) {
        mContext = checkNotNull(context);
        mClientConfiguration = clientConfiguration;
        mCustomTabManager = customTabManager;
        mBrowser = browser;

        if (browser != null && browser.useCustomTab) {
            mCustomTabManager.bind(browser.packageName);
        }
    }

    public CustomTabManager getCustomTabManager() {
        return mCustomTabManager;
    }

    /**
     * Returns the BrowserDescriptor of the chosen browser.
     * Can for example be used to set the browsers package name to a CustomTabsIntent.
     */
    public BrowserDescriptor getBrowserDescriptor() {
        return mBrowser;
    }

    /**
     * Creates a custom tab builder, that will use a tab session from an existing connection to
     * a web browser, if available.
     */
    public CustomTabsIntent.Builder createCustomTabsIntentBuilder(Uri... possibleUris) {
        checkNotDisposed();
        return mCustomTabManager.createTabBuilder(possibleUris);
    }

    /**
     * Sends an authorization request to the authorization service, using a
     * [custom tab](https://developer.chrome.com/multidevice/android/customtabs)
     * if available, or a browser instance.
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link AuthorizationRequest request object}. Upon completion
     * of this request, the provided {@link PendingIntent completion PendingIntent} will be invoked.
     * If the user cancels the authorization request, the current activity will regain control.
     */
    public void performAuthorizationRequest(
            @NonNull AuthorizationRequest request,
            @NonNull PendingIntent completedIntent) {
        performAuthorizationRequest(
                request,
                completedIntent,
                null,
                createCustomTabsIntentBuilder().build());
    }

    /**
     * Sends an authorization request to the authorization service, using a
     * [custom tab](https://developer.chrome.com/multidevice/android/customtabs)
     * if available, or a browser instance.
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link AuthorizationRequest request object}. Upon completion
     * of this request, the provided {@link PendingIntent completion PendingIntent} will be invoked.
     * If the user cancels the authorization request, the provided
     * {@link PendingIntent cancel PendingIntent} will be invoked.
     */
    public void performAuthorizationRequest(
            @NonNull AuthorizationRequest request,
            @NonNull PendingIntent completedIntent,
            @NonNull PendingIntent canceledIntent) {
        performAuthorizationRequest(
                request,
                completedIntent,
                canceledIntent,
                createCustomTabsIntentBuilder().build());
    }

    /**
     * Sends an authorization request to the authorization service, using a
     * [custom tab](https://developer.chrome.com/multidevice/android/customtabs).
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link AuthorizationRequest request object}. Upon completion
     * of this request, the provided {@link PendingIntent completion PendingIntent} will be invoked.
     * If the user cancels the authorization request, the current activity will regain control.
     *
     * @param customTabsIntent
     *     The intent that will be used to start the custom tab. It is recommended that this intent
     *     be created with the help of {@link #createCustomTabsIntentBuilder(Uri[])}, which will
     *     ensure that a warmed-up version of the browser will be used, minimizing latency.
     */
    public void performAuthorizationRequest(
            @NonNull AuthorizationRequest request,
            @NonNull PendingIntent completedIntent,
            @NonNull CustomTabsIntent customTabsIntent) {
        performAuthorizationRequest(
                request,
                completedIntent,
                null,
                customTabsIntent);
    }

    /**
     * Sends an authorization request to the authorization service, using a
     * [custom tab](https://developer.chrome.com/multidevice/android/customtabs).
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link AuthorizationRequest request object}. Upon completion
     * of this request, the provided {@link PendingIntent completion PendingIntent} will be invoked.
     * If the user cancels the authorization request, the provided
     * {@link PendingIntent cancel PendingIntent} will be invoked.
     *
     * @param customTabsIntent
     *     The intent that will be used to start the custom tab. It is recommended that this intent
     *     be created with the help of {@link #createCustomTabsIntentBuilder(Uri[])}, which will
     *     ensure that a warmed-up version of the browser will be used, minimizing latency.
     *
     * @throws android.content.ActivityNotFoundException if no suitable browser is available to
     *     perform the authorization flow.
     */
    public void performAuthorizationRequest(
            @NonNull AuthorizationRequest request,
            @NonNull PendingIntent completedIntent,
            @Nullable PendingIntent canceledIntent,
            @NonNull CustomTabsIntent customTabsIntent) {
        performAuthManagementRequest(
                request,
                completedIntent,
                canceledIntent,
                customTabsIntent);
    }

    /**
     * Sends an end session request to the authorization service, using a
     * [custom tab](https://developer.chrome.com/multidevice/android/customtabs)
     * if available, or a browser instance.
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link EndSessionRequest request object}. Upon completion
     * of this request, the provided {@link PendingIntent completion PendingIntent} will be invoked.
     * If the user cancels the authorization request, the current activity will regain control.
     */
    public void performEndSessionRequest(
            @NonNull EndSessionRequest request,
            @NonNull PendingIntent completedIntent) {
        performEndSessionRequest(
                request,
                completedIntent,
                null,
                createCustomTabsIntentBuilder().build());
    }

    /**
     * Sends an end session request to the authorization service, using a
     * [custom tab](https://developer.chrome.com/multidevice/android/customtabs)
     * if available, or a browser instance.
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link EndSessionRequest request object}. Upon completion
     * of this request, the provided {@link PendingIntent completion PendingIntent} will be invoked.
     * If the user cancels the authorization request, the provided
     * {@link PendingIntent cancel PendingIntent} will be invoked.
     */
    public void performEndSessionRequest(
            @NonNull EndSessionRequest request,
            @NonNull PendingIntent completedIntent,
            @NonNull PendingIntent canceledIntent) {
        performEndSessionRequest(
                request,
                completedIntent,
                canceledIntent,
                createCustomTabsIntentBuilder().build());
    }

    /**
     * Sends an end session request to the authorization service, using a
     * [custom tab](https://developer.chrome.com/multidevice/android/customtabs).
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link EndSessionRequest request object}. Upon completion
     * of this request, the provided {@link PendingIntent completion PendingIntent} will be invoked.
     * If the user cancels the authorization request, the current activity will regain control.
     *
     * @param customTabsIntent
     *     The intent that will be used to start the custom tab. It is recommended that this intent
     *     be created with the help of {@link #createCustomTabsIntentBuilder(Uri[])}, which will
     *     ensure that a warmed-up version of the browser will be used, minimizing latency.
     */
    public void performEndSessionRequest(
            @NonNull EndSessionRequest request,
            @NonNull PendingIntent completedIntent,
            @NonNull CustomTabsIntent customTabsIntent) {
        performEndSessionRequest(
                request,
                completedIntent,
                null,
                customTabsIntent);
    }

    /**
     * Sends an end session request to the authorization service, using a
     * [custom tab](https://developer.chrome.com/multidevice/android/customtabs).
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link EndSessionRequest request object}. Upon completion
     * of this request, the provided {@link PendingIntent completion PendingIntent} will be invoked.
     * If the user cancels the authorization request, the provided
     * {@link PendingIntent cancel PendingIntent} will be invoked.
     *
     * @param customTabsIntent
     *     The intent that will be used to start the custom tab. It is recommended that this intent
     *     be created with the help of {@link #createCustomTabsIntentBuilder(Uri[])}, which will
     *     ensure that a warmed-up version of the browser will be used, minimizing latency.
     *
     * @throws android.content.ActivityNotFoundException if no suitable browser is available to
     *     perform the authorization flow.
     */
    public void performEndSessionRequest(
            @NonNull EndSessionRequest request,
            @NonNull PendingIntent completedIntent,
            @Nullable PendingIntent canceledIntent,
            @NonNull CustomTabsIntent customTabsIntent) {
        performAuthManagementRequest(
                request,
                completedIntent,
                canceledIntent,
                customTabsIntent);
    }

    private void performAuthManagementRequest(
            @NonNull AuthorizationManagementRequest request,
            @NonNull PendingIntent completedIntent,
            @Nullable PendingIntent canceledIntent,
            @NonNull CustomTabsIntent customTabsIntent) {

        checkNotDisposed();
        checkNotNull(request);
        checkNotNull(completedIntent);
        checkNotNull(customTabsIntent);

        Intent authIntent = prepareAuthorizationRequestIntent(request, customTabsIntent);
        Intent startIntent = AuthorizationManagementActivity.createStartIntent(
                mContext,
                request,
                authIntent,
                completedIntent,
                canceledIntent);

        // Calling start activity from outside an activity requires FLAG_ACTIVITY_NEW_TASK.
        if (!isActivity(mContext)) {
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        mContext.startActivity(startIntent);
    }

    private boolean isActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return true;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return false;
    }

    /**
     * Constructs an intent that encapsulates the provided request and custom tabs intent,
     * and is intended to be launched via {@link Activity#startActivityForResult}.
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link AuthorizationRequest request object}. Upon completion
     * of this request, the activity that gets launched will call {@link Activity#setResult} with
     * {@link Activity#RESULT_OK} and an {@link Intent} containing authorization completion
     * information. If the user presses the back button or closes the browser tab, the launched
     * activity will call {@link Activity#setResult} with
     * {@link Activity#RESULT_CANCELED} without a data {@link Intent}. Note that
     * {@link Activity#RESULT_OK} indicates the authorization request completed,
     * not necessarily that it was a successful authorization.
     *
     * @param customTabsIntent
     *     The intent that will be used to start the custom tab. It is recommended that this intent
     *     be created with the help of {@link #createCustomTabsIntentBuilder(Uri[])}, which will
     *     ensure that a warmed-up version of the browser will be used, minimizing latency.
     *
     * @throws android.content.ActivityNotFoundException if no suitable browser is available to
     *     perform the authorization flow.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Intent getAuthorizationRequestIntent(
            @NonNull AuthorizationRequest request,
            @NonNull CustomTabsIntent customTabsIntent) {

        Intent authIntent = prepareAuthorizationRequestIntent(request, customTabsIntent);
        return AuthorizationManagementActivity.createStartForResultIntent(
                mContext,
                request,
                authIntent);
    }

    /**
     * Constructs an intent that encapsulates the provided request and a default custom tabs intent,
     * and is intended to be launched via {@link Activity#startActivityForResult}
     * When started, the intent launches an {@link Activity} that sends an authorization request
     * to the authorization service, using a
     * [custom tab](https://developer.chrome.com/multidevice/android/customtabs).
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link AuthorizationRequest request object}. Upon completion
     * of this request, the activity that gets launched will call {@link Activity#setResult} with
     * {@link Activity#RESULT_OK} and an {@link Intent} containing authorization completion
     * information. If the user presses the back button or closes the browser tab, the launched
     * activity will call {@link Activity#setResult} with
     * {@link Activity#RESULT_CANCELED} without a data {@link Intent}. Note that
     * {@link Activity#RESULT_OK} indicates the authorization request completed,
     * not necessarily that it was a successful authorization.
     *
     * @throws android.content.ActivityNotFoundException if no suitable browser is available to
     *     perform the authorization flow.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Intent getAuthorizationRequestIntent(
            @NonNull AuthorizationRequest request) {
        return getAuthorizationRequestIntent(request, createCustomTabsIntentBuilder().build());
    }

    /**
     * Constructs an intent that encapsulates the provided request and custom tabs intent,
     * and is intended to be launched via {@link Activity#startActivityForResult}.
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link AuthorizationRequest request object}. Upon completion
     * of this request, the activity that gets launched will call {@link Activity#setResult} with
     * {@link Activity#RESULT_OK} and an {@link Intent} containing authorization completion
     * information. If the user presses the back button or closes the browser tab, the launched
     * activity will call {@link Activity#setResult} with
     * {@link Activity#RESULT_CANCELED} without a data {@link Intent}. Note that
     * {@link Activity#RESULT_OK} indicates the authorization request completed,
     * not necessarily that it was a successful authorization.
     *
     * @param customTabsIntent
     *     The intent that will be used to start the custom tab. It is recommended that this intent
     *     be created with the help of {@link #createCustomTabsIntentBuilder(Uri[])}, which will
     *     ensure that a warmed-up version of the browser will be used, minimizing latency.
     *
     * @throws android.content.ActivityNotFoundException if no suitable browser is available to
     *     perform the authorization flow.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Intent getEndSessionRequestIntent(
            @NonNull EndSessionRequest request,
            @NonNull CustomTabsIntent customTabsIntent) {

        Intent authIntent = prepareAuthorizationRequestIntent(request, customTabsIntent);
        return AuthorizationManagementActivity.createStartForResultIntent(
            mContext,
            request,
            authIntent);
    }

    /**
     * Constructs an intent that encapsulates the provided request and a default custom tabs intent,
     * and is intended to be launched via {@link Activity#startActivityForResult}
     * When started, the intent launches an {@link Activity} that sends an authorization request
     * to the authorization service, using a
     * [custom tab](https://developer.chrome.com/multidevice/android/customtabs).
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link EndSessionRequest request object}. Upon completion
     * of this request, the activity that gets launched will call {@link Activity#setResult} with
     * {@link Activity#RESULT_OK} and an {@link Intent} containing authorization completion
     * information. If the user presses the back button or closes the browser tab, the launched
     * activity will call {@link Activity#setResult} with
     * {@link Activity#RESULT_CANCELED} without a data {@link Intent}. Note that
     * {@link Activity#RESULT_OK} indicates the authorization request completed,
     * not necessarily that it was a successful authorization.
     *
     * @throws android.content.ActivityNotFoundException if no suitable browser is available to
     *     perform the authorization flow.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Intent getEndSessionRequestIntent(
            @NonNull EndSessionRequest request) {
        return getEndSessionRequestIntent(request, createCustomTabsIntentBuilder().build());
    }

    /**
     * Sends a request to the authorization service to exchange a code granted as part of an
     * authorization request for a token. The result of this request will be sent to the provided
     * callback handler.
     */
    public void performTokenRequest(
            @NonNull TokenRequest request,
            @NonNull TokenResponseCallback callback) {
        performTokenRequest(request, NoClientAuthentication.INSTANCE, callback);
    }

    /**
     * Sends a request to the authorization service to exchange a code granted as part of an
     * authorization request for a token. The result of this request will be sent to the provided
     * callback handler.
     */
    public void performTokenRequest(
            @NonNull TokenRequest request,
            @NonNull ClientAuthentication clientAuthentication,
            @NonNull TokenResponseCallback callback) {
        checkNotDisposed();
        Logger.debug("Initiating code exchange request to %s",
                request.configuration.tokenEndpoint);
        new TokenRequestTask(
                request,
                clientAuthentication,
                mClientConfiguration.getConnectionBuilder(),
                SystemClock.INSTANCE,
                callback,
                mClientConfiguration.getSkipIssuerHttpsCheck())
                .execute();
    }

    /**
     * Sends a request to the authorization service to dynamically register a client.
     * The result of this request will be sent to the provided callback handler.
     */
    public void performRegistrationRequest(
            @NonNull RegistrationRequest request,
            @NonNull RegistrationResponseCallback callback) {
        checkNotDisposed();
        Logger.debug("Initiating dynamic client registration %s",
                request.configuration.registrationEndpoint.toString());
        new RegistrationRequestTask(
                request,
                mClientConfiguration.getConnectionBuilder(),
                callback)
                .execute();
    }

    /**
     * Disposes state that will not normally be handled by garbage collection. This should be
     * called when the authorization service is no longer required, including when any owning
     * activity is paused or destroyed (i.e. in {@link android.app.Activity#onStop()}).
     */
    public void dispose() {
        if (mDisposed) {
            return;
        }
        mCustomTabManager.dispose();
        mDisposed = true;
    }

    private void checkNotDisposed() {
        if (mDisposed) {
            throw new IllegalStateException("Service has been disposed and rendered inoperable");
        }
    }

    private Intent prepareAuthorizationRequestIntent(
            AuthorizationManagementRequest request,
            CustomTabsIntent customTabsIntent) {
        checkNotDisposed();

        if (mBrowser == null) {
            throw new ActivityNotFoundException();
        }

        Uri requestUri = request.toUri();
        Intent intent;
        if (mBrowser.useCustomTab) {
            intent = customTabsIntent.intent;
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
        }
        intent.setPackage(mBrowser.packageName);
        intent.setData(requestUri);

        Logger.debug("Using %s as browser for auth, custom tab = %s",
                intent.getPackage(),
                mBrowser.useCustomTab.toString());

        //TODO fix logger for configuration
        //Logger.debug("Initiating authorization request to %s"
        //request.configuration.authorizationEndpoint);

        return intent;
    }

    private static class TokenRequestTask
            extends AsyncTask<Void, Void, JSONObject> {

        private TokenRequest mRequest;
        private ClientAuthentication mClientAuthentication;
        private final ConnectionBuilder mConnectionBuilder;
        private TokenResponseCallback mCallback;
        private Clock mClock;
        private boolean mSkipIssuerHttpsCheck;

        private AuthorizationException mException;

        TokenRequestTask(TokenRequest request,
                         @NonNull ClientAuthentication clientAuthentication,
                         @NonNull ConnectionBuilder connectionBuilder,
                         Clock clock,
                         TokenResponseCallback callback,
                         Boolean skipIssuerHttpsCheck) {
            mRequest = request;
            mClientAuthentication = clientAuthentication;
            mConnectionBuilder = connectionBuilder;
            mClock = clock;
            mCallback = callback;
            mSkipIssuerHttpsCheck = skipIssuerHttpsCheck;
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            InputStream is = null;
            try {
                HttpURLConnection conn = mConnectionBuilder.openConnection(
                        mRequest.configuration.tokenEndpoint);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                addJsonToAcceptHeader(conn);
                conn.setDoOutput(true);

                Map<String, String> headers = mClientAuthentication
                        .getRequestHeaders(mRequest.clientId);
                if (headers != null) {
                    for (Map.Entry<String,String> header : headers.entrySet()) {
                        conn.setRequestProperty(header.getKey(), header.getValue());
                    }
                }

                Map<String, String> parameters = mRequest.getRequestParameters();
                Map<String, String> clientAuthParams = mClientAuthentication
                        .getRequestParameters(mRequest.clientId);
                if (clientAuthParams != null) {
                    parameters.putAll(clientAuthParams);
                }

                String queryData = UriUtil.formUrlEncode(parameters);
                conn.setRequestProperty("Content-Length", String.valueOf(queryData.length()));
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

                wr.write(queryData);
                wr.flush();

                if (conn.getResponseCode() >= HttpURLConnection.HTTP_OK
                        && conn.getResponseCode() < HttpURLConnection.HTTP_MULT_CHOICE) {
                    is = conn.getInputStream();
                } else {
                    is = conn.getErrorStream();
                }
                String response = Utils.readInputStream(is);
                return new JSONObject(response);
            } catch (IOException ex) {
                Logger.debugWithStack(ex, "Failed to complete exchange request");
                mException = AuthorizationException.fromTemplate(
                        GeneralErrors.NETWORK_ERROR, ex);
            } catch (JSONException ex) {
                Logger.debugWithStack(ex, "Failed to complete exchange request");
                mException = AuthorizationException.fromTemplate(
                        GeneralErrors.JSON_DESERIALIZATION_ERROR, ex);
            } finally {
                Utils.closeQuietly(is);
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            if (mException != null) {
                mCallback.onTokenRequestCompleted(null, mException);
                return;
            }

            if (json.has(AuthorizationException.PARAM_ERROR)) {
                AuthorizationException ex;
                try {
                    String error = json.getString(AuthorizationException.PARAM_ERROR);
                    ex = AuthorizationException.fromOAuthTemplate(
                            TokenRequestErrors.byString(error),
                            error,
                            json.optString(AuthorizationException.PARAM_ERROR_DESCRIPTION, null),
                            UriUtil.parseUriIfAvailable(
                                    json.optString(AuthorizationException.PARAM_ERROR_URI)));
                } catch (JSONException jsonEx) {
                    ex = AuthorizationException.fromTemplate(
                            GeneralErrors.JSON_DESERIALIZATION_ERROR,
                            jsonEx);
                }
                mCallback.onTokenRequestCompleted(null, ex);
                return;
            }

            TokenResponse response;
            try {
                response = new TokenResponse.Builder(mRequest).fromResponseJson(json).build();
            } catch (JSONException jsonEx) {
                mCallback.onTokenRequestCompleted(null,
                        AuthorizationException.fromTemplate(
                                GeneralErrors.JSON_DESERIALIZATION_ERROR,
                                jsonEx));
                return;
            }

            if (response.idToken != null) {
                IdToken idToken;
                try {
                    idToken = IdToken.from(response.idToken);
                } catch (IdTokenException | JSONException ex) {
                    mCallback.onTokenRequestCompleted(null,
                            AuthorizationException.fromTemplate(
                                    GeneralErrors.ID_TOKEN_PARSING_ERROR,
                                    ex));
                    return;
                }

                try {
                    idToken.validate(
                            mRequest,
                            mClock,
                            mSkipIssuerHttpsCheck
                    );
                } catch (AuthorizationException ex) {
                    mCallback.onTokenRequestCompleted(null, ex);
                    return;
                }
            }
            Logger.debug("Token exchange with %s completed",
                    mRequest.configuration.tokenEndpoint);
            mCallback.onTokenRequestCompleted(response, null);
        }

        /**
         * GitHub will only return a spec-compliant response if JSON is explicitly defined
         * as an acceptable response type. As this is essentially harmless for all other
         * spec-compliant IDPs, we add this header if no existing Accept header has been set
         * by the connection builder.
         */
        private void addJsonToAcceptHeader(URLConnection conn) {
            if (TextUtils.isEmpty(conn.getRequestProperty("Accept"))) {
                conn.setRequestProperty("Accept", "application/json");
            }
        }
    }

    /**
     * Callback interface for token endpoint requests.
     * @see AuthorizationService#performTokenRequest
     */
    public interface TokenResponseCallback {
        /**
         * Invoked when the request completes successfully or fails.
         *
         * Exactly one of `response` or `ex` will be non-null. If `response` is `null`, a failure
         * occurred during the request. This can happen if a bad URI was provided, no connection
         * to the server could be established, or the response JSON was incomplete or incorrectly
         * formatted.
         *
         * @param response the retrieved token response, if successful; `null` otherwise.
         * @param ex a description of the failure, if one occurred: `null` otherwise.
         *
         * @see AuthorizationException.TokenRequestErrors
         */
        void onTokenRequestCompleted(@Nullable TokenResponse response,
                @Nullable AuthorizationException ex);
    }

    private static class RegistrationRequestTask
            extends AsyncTask<Void, Void, JSONObject> {
        private RegistrationRequest mRequest;
        private final ConnectionBuilder mConnectionBuilder;
        private RegistrationResponseCallback mCallback;

        private AuthorizationException mException;

        RegistrationRequestTask(RegistrationRequest request,
                ConnectionBuilder connectionBuilder,
                RegistrationResponseCallback callback) {
            mRequest = request;
            mConnectionBuilder = connectionBuilder;
            mCallback = callback;
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            InputStream is = null;
            String postData = mRequest.toJsonString();
            try {
                HttpURLConnection conn = mConnectionBuilder.openConnection(
                        mRequest.configuration.registrationEndpoint);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Length", String.valueOf(postData.length()));
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(postData);
                wr.flush();

                is = conn.getInputStream();
                String response = Utils.readInputStream(is);
                return new JSONObject(response);
            } catch (IOException ex) {
                Logger.debugWithStack(ex, "Failed to complete registration request");
                mException = AuthorizationException.fromTemplate(
                        GeneralErrors.NETWORK_ERROR, ex);
            } catch (JSONException ex) {
                Logger.debugWithStack(ex, "Failed to complete registration request");
                mException = AuthorizationException.fromTemplate(
                        GeneralErrors.JSON_DESERIALIZATION_ERROR, ex);
            } finally {
                Utils.closeQuietly(is);
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            if (mException != null) {
                mCallback.onRegistrationRequestCompleted(null, mException);
                return;
            }

            if (json.has(AuthorizationException.PARAM_ERROR)) {
                AuthorizationException ex;
                try {
                    String error = json.getString(AuthorizationException.PARAM_ERROR);
                    ex = AuthorizationException.fromOAuthTemplate(
                            RegistrationRequestErrors.byString(error),
                            error,
                            json.getString(AuthorizationException.PARAM_ERROR_DESCRIPTION),
                            UriUtil.parseUriIfAvailable(
                                    json.getString(AuthorizationException.PARAM_ERROR_URI)));
                } catch (JSONException jsonEx) {
                    ex = AuthorizationException.fromTemplate(
                            GeneralErrors.JSON_DESERIALIZATION_ERROR,
                            jsonEx);
                }
                mCallback.onRegistrationRequestCompleted(null, ex);
                return;
            }

            RegistrationResponse response;
            try {
                response = new RegistrationResponse.Builder(mRequest)
                        .fromResponseJson(json).build();
            } catch (JSONException jsonEx) {
                mCallback.onRegistrationRequestCompleted(null,
                        AuthorizationException.fromTemplate(
                                GeneralErrors.JSON_DESERIALIZATION_ERROR,
                                jsonEx));
                return;
            } catch (RegistrationResponse.MissingArgumentException ex) {
                Logger.errorWithStack(ex, "Malformed registration response");
                mException = AuthorizationException.fromTemplate(
                        GeneralErrors.INVALID_REGISTRATION_RESPONSE,
                        ex);
                return;
            }
            Logger.debug("Dynamic registration with %s completed",
                    mRequest.configuration.registrationEndpoint);
            mCallback.onRegistrationRequestCompleted(response, null);
        }
    }

    /**
     * Callback interface for token endpoint requests.
     *
     * @see AuthorizationService#performTokenRequest
     */
    public interface RegistrationResponseCallback {
        /**
         * Invoked when the request completes successfully or fails.
         *
         * Exactly one of `response` or `ex` will be non-null. If `response` is `null`, a failure
         * occurred during the request. This can happen if an invalid URI was provided, no
         * connection to the server could be established, or the response JSON was incomplete or
         * incorrectly formatted.
         *
         * @param response the retrieved registration response, if successful; `null` otherwise.
         * @param ex a description of the failure, if one occurred: `null` otherwise.
         * @see AuthorizationException.RegistrationRequestErrors
         */
        void onRegistrationRequestCompleted(@Nullable RegistrationResponse response,
                                            @Nullable AuthorizationException ex);
    }
}
