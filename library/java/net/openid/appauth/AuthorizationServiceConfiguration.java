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
import static net.openid.appauth.Preconditions.checkNotNull;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.openid.appauth.AuthorizationException.GeneralErrors;

import net.openid.appauth.connectivity.ConnectionBuilder;
import net.openid.appauth.connectivity.DefaultConnectionBuilder;
import net.openid.appauth.internal.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Configuration details required to interact with an authorization service.
 */
public class AuthorizationServiceConfiguration {

    /**
     * The standard base path for well-known resources on domains.
     *
     * @see "Defining Well-Known Uniform Resource Identifiers (RFC 5785)
     * <https://tools.ietf.org/html/rfc5785>"
     */
    public static final String WELL_KNOWN_PATH =
            ".well-known";

    /**
     * The standard resource under {@link #WELL_KNOWN_PATH .well-known} at which an OpenID Connect
     * discovery document can be found under an issuer's base URI.
     *
     * @see "OpenID Connect discovery 1.0
     * <https://openid.net/specs/openid-connect-discovery-1_0.html>"
     */
    public static final String OPENID_CONFIGURATION_RESOURCE =
            "openid-configuration";

    private static final String KEY_AUTHORIZATION_ENDPOINT = "authorizationEndpoint";
    private static final String KEY_TOKEN_ENDPOINT = "tokenEndpoint";
    private static final String KEY_REGISTRATION_ENDPOINT = "registrationEndpoint";
    private static final String KEY_DISCOVERY_DOC = "discoveryDoc";

    /**
     * The authorization service's endpoint.
     */
    @NonNull
    public final Uri authorizationEndpoint;

    /**
     * The authorization service's token exchange and refresh endpoint.
     */
    @NonNull
    public final Uri tokenEndpoint;

    /**
     * The authorization service's client registration endpoint.
     */
    @Nullable
    public final Uri registrationEndpoint;


    /**
     * The discovery document describing the service, if it is an OpenID Connect provider.
     */
    @Nullable
    public final AuthorizationServiceDiscovery discoveryDoc;

    /**
     * Creates a service configuration for a basic OAuth2 provider.
     * @param authorizationEndpoint The
     *     [authorization endpoint URI](https://tools.ietf.org/html/rfc6749#section-3.1)
     *     for the service.
     * @param tokenEndpoint The
     *     [token endpoint URI](https://tools.ietf.org/html/rfc6749#section-3.2)
     *     for the service.
     */
    public AuthorizationServiceConfiguration(
            @NonNull Uri authorizationEndpoint,
            @NonNull Uri tokenEndpoint) {
        this(authorizationEndpoint, tokenEndpoint, null);
    }

    /**
     * Creates a service configuration for a basic OAuth2 provider.
     *
     * @param authorizationEndpoint The
     *     [authorization endpoint URI](https://tools.ietf.org/html/rfc6749#section-3.1)
     *     for the service.
     * @param tokenEndpoint The
     *     [token endpoint URI](https://tools.ietf.org/html/rfc6749#section-3.2)
     *     for the service.
     * @param registrationEndpoint The optional
     *     [client registration endpoint URI](https://tools.ietf.org/html/rfc7591#section-3)
     *     for the service.
     */
    public AuthorizationServiceConfiguration(
            @NonNull Uri authorizationEndpoint,
            @NonNull Uri tokenEndpoint,
            @Nullable Uri registrationEndpoint) {
        this.authorizationEndpoint = checkNotNull(authorizationEndpoint);
        this.tokenEndpoint = checkNotNull(tokenEndpoint);
        this.registrationEndpoint = registrationEndpoint;
        this.discoveryDoc = null;
    }

    /**
     * Creates an service configuration for an OpenID Connect provider, based on its
     * {@link AuthorizationServiceDiscovery discovery document}.
     *
     * @param discoveryDoc The OpenID Connect discovery document which describes this service.
     */
    public AuthorizationServiceConfiguration(
            @NonNull AuthorizationServiceDiscovery discoveryDoc) {
        checkNotNull(discoveryDoc, "docJson cannot be null");
        this.discoveryDoc = discoveryDoc;
        this.authorizationEndpoint = discoveryDoc.getAuthorizationEndpoint();
        this.tokenEndpoint = discoveryDoc.getTokenEndpoint();
        this.registrationEndpoint = discoveryDoc.getRegistrationEndpoint();
    }

    /**
     * Converts the authorization service configuration to JSON for storage or transmission.
     */
    @NonNull
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_AUTHORIZATION_ENDPOINT, authorizationEndpoint.toString());
        JsonUtil.put(json, KEY_TOKEN_ENDPOINT, tokenEndpoint.toString());
        if (registrationEndpoint != null) {
            JsonUtil.put(json, KEY_REGISTRATION_ENDPOINT, registrationEndpoint.toString());
        }
        if (discoveryDoc != null) {
            JsonUtil.put(json, KEY_DISCOVERY_DOC, discoveryDoc.docJson);
        }
        return json;
    }

    /**
     * Converts the authorization service configuration to a JSON string for storage or
     * transmission.
     */
    public String toJsonString() {
        return toJson().toString();
    }

    /**
     * Reads an Authorization service configuration from a JSON representation produced by the
     * {@link #toJson()} method or some other equivalent producer.
     *
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static AuthorizationServiceConfiguration fromJson(@NonNull JSONObject json)
            throws JSONException {
        checkNotNull(json, "json object cannot be null");

        if (json.has(KEY_DISCOVERY_DOC)) {
            try {
                AuthorizationServiceDiscovery discoveryDoc =
                        new AuthorizationServiceDiscovery(json.optJSONObject(KEY_DISCOVERY_DOC));
                return new AuthorizationServiceConfiguration(discoveryDoc);
            } catch (AuthorizationServiceDiscovery.MissingArgumentException ex) {
                throw new JSONException("Missing required field in discovery doc: "
                        + ex.getMissingField());
            }
        } else {
            checkArgument(json.has(KEY_AUTHORIZATION_ENDPOINT), "missing authorizationEndpoint");
            checkArgument(json.has(KEY_TOKEN_ENDPOINT), "missing tokenEndpoint");
            return new AuthorizationServiceConfiguration(
                    JsonUtil.getUri(json, KEY_AUTHORIZATION_ENDPOINT),
                    JsonUtil.getUri(json, KEY_TOKEN_ENDPOINT),
                    JsonUtil.getUriIfDefined(json, KEY_REGISTRATION_ENDPOINT));
        }
    }

    /**
     * Reads an Authorization service configuration from a JSON representation produced by the
     * {@link #toJson()} method or some other equivalent producer.
     *
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    public static AuthorizationServiceConfiguration fromJson(@NonNull String jsonStr)
            throws JSONException {
        checkNotNull(jsonStr, "json cannot be null");
        return AuthorizationServiceConfiguration.fromJson(new JSONObject(jsonStr));
    }

    /**
     * Fetch an AuthorizationServiceConfiguration from an OpenID Connect issuer URI.
     * This method is equivalent to {@link #fetchFromUrl(Uri, RetrieveConfigurationCallback)},
     * but automatically appends the OpenID connect well-known configuration path to the
     * URI.
     *
     * @param openIdConnectIssuerUri The issuer URI, e.g. "https://accounts.google.com"
     * @param callback The callback to invoke upon completion.
     *
     * @see "OpenID Connect discovery 1.0
     * <https://openid.net/specs/openid-connect-discovery-1_0.html>"
     */
    public static void fetchFromIssuer(@NonNull Uri openIdConnectIssuerUri,
            @NonNull RetrieveConfigurationCallback callback) {
        fetchFromUrl(buildConfigurationUriFromIssuer(openIdConnectIssuerUri), callback);
    }

    static Uri buildConfigurationUriFromIssuer(Uri openIdConnectIssuerUri) {
        return openIdConnectIssuerUri.buildUpon()
                .appendPath(WELL_KNOWN_PATH)
                .appendPath(OPENID_CONFIGURATION_RESOURCE)
                .build();
    }

    /**
     * Fetch a AuthorizationServiceConfiguration from an OpenID Connect discovery URI, using
     * the {@link DefaultConnectionBuilder default connection builder}.
     *
     * @param openIdConnectDiscoveryUri The OpenID Connect discovery URI
     * @param callback A callback to invoke upon completion
     *
     * @see "OpenID Connect discovery 1.0
     * <https://openid.net/specs/openid-connect-discovery-1_0.html>"
     */
    public static void fetchFromUrl(@NonNull Uri openIdConnectDiscoveryUri,
            @NonNull RetrieveConfigurationCallback callback) {
        fetchFromUrl(openIdConnectDiscoveryUri,
                callback,
                DefaultConnectionBuilder.INSTANCE);
    }

    /**
     * Fetch a AuthorizationServiceConfiguration from an OpenID Connect discovery URI.
     *
     * @param openIdConnectDiscoveryUri The OpenID Connect discovery URI
     * @param connectionBuilder The connection builder that is used to establish a connection
     *     to the resource server.
     * @param callback A callback to invoke upon completion
     *
     * @see "OpenID Connect discovery 1.0
     * <https://openid.net/specs/openid-connect-discovery-1_0.html>"
     */
    public static void fetchFromUrl(
            @NonNull Uri openIdConnectDiscoveryUri,
            @NonNull RetrieveConfigurationCallback callback,
            @NonNull ConnectionBuilder connectionBuilder) {
        checkNotNull(openIdConnectDiscoveryUri, "openIDConnectDiscoveryUri cannot be null");
        checkNotNull(callback, "callback cannot be null");
        checkNotNull(connectionBuilder, "connectionBuilder must not be null");
        new ConfigurationRetrievalAsyncTask(
                openIdConnectDiscoveryUri,
                connectionBuilder,
                callback)
                .execute();
    }

    /**
     * Callback interface for configuration retrieval.
     * @see AuthorizationServiceConfiguration#fetchFromUrl(Uri,RetrieveConfigurationCallback)
     */
    public interface RetrieveConfigurationCallback {
        /**
         * Invoked when the retrieval of the discovery doc completes successfully or fails.
         *
         * <p>Exactly one of `serviceConfiguration` or `ex` will be non-null. If
         * `serviceConfiguration` is `null`, a failure occurred during the request. This
         * can happen if a bad URL was provided, no connection to the server could be established,
         * or the retrieved JSON is incomplete or badly formatted.
         *
         * @param serviceConfiguration the service configuration that can be used to initialize
         *     the {@link AuthorizationService}, if retrieval was successful; `null` otherwise.
         * @param ex the exception that caused an error.
         */
        void onFetchConfigurationCompleted(
                @Nullable AuthorizationServiceConfiguration serviceConfiguration,
                @Nullable AuthorizationException ex);
    }

    /**
     * ASyncTask that tries to retrieve the discover document and gives the callback with the
     * values retrieved from the discovery document. In case of retrieval error, the exception
     * is handed back to the callback.
     */
    private static class ConfigurationRetrievalAsyncTask
            extends AsyncTask<Void, Void, AuthorizationServiceConfiguration> {

        private Uri mUri;
        private ConnectionBuilder mConnectionBuilder;
        private RetrieveConfigurationCallback mCallback;
        private AuthorizationException mException;

        ConfigurationRetrievalAsyncTask(
                Uri uri,
                ConnectionBuilder connectionBuilder,
                RetrieveConfigurationCallback callback) {
            mUri = uri;
            mConnectionBuilder = connectionBuilder;
            mCallback = callback;
            mException = null;
        }

        @Override
        protected AuthorizationServiceConfiguration doInBackground(Void... voids) {
            InputStream is = null;
            try {
                HttpURLConnection conn = mConnectionBuilder.openConnection(mUri);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();

                if (conn.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                    is = conn.getInputStream();
                } else {
                    is = conn.getErrorStream();
                    String errorString = Utils.readInputStream(is);
                    mException = AuthorizationException.fromHttpError(
                        conn.getResponseCode(),
                        conn.getResponseMessage(),
                        errorString);
                    return null;
                }

                JSONObject json = new JSONObject(Utils.readInputStream(is));

                AuthorizationServiceDiscovery discovery =
                        new AuthorizationServiceDiscovery(json);
                return new AuthorizationServiceConfiguration(discovery);
            } catch (IOException ex) {
                Logger.errorWithStack(ex, "Network error when retrieving discovery document");
                mException = AuthorizationException.fromTemplate(
                        GeneralErrors.NETWORK_ERROR,
                        ex);
            } catch (JSONException ex) {
                Logger.errorWithStack(ex, "Error parsing discovery document");
                mException = AuthorizationException.fromTemplate(
                        GeneralErrors.JSON_DESERIALIZATION_ERROR,
                        ex);
            } catch (AuthorizationServiceDiscovery.MissingArgumentException ex) {
                Logger.errorWithStack(ex, "Malformed discovery document");
                mException = AuthorizationException.fromTemplate(
                        GeneralErrors.INVALID_DISCOVERY_DOCUMENT,
                        ex);
            } finally {
                Utils.closeQuietly(is);
            }
            return null;
        }

        @Override
        protected void onPostExecute(AuthorizationServiceConfiguration configuration) {
            if (mException != null) {
                mCallback.onFetchConfigurationCompleted(null, mException);
            } else {
                mCallback.onFetchConfigurationCompleted(configuration, null);
            }
        }
    }
}
