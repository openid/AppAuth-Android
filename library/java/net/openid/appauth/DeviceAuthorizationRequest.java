/*
 * Copyright 2021 The AppAuth for Android Authors. All Rights Reserved.
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

import static net.openid.appauth.AdditionalParamsProcessor.builtInParams;
import static net.openid.appauth.AdditionalParamsProcessor.checkAdditionalParams;
import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;

import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An OAuth2 device authorization request.
 *
 * @see "OAuth 2.0 Device Grant (RFC 8628), Section 3
 * <https://tools.ietf.org/html/rfc8628#section-3>"
 * @see "OAuth 2.0 Device Grant (RFC 8628), Section 3.1
 * <https://tools.ietf.org/html/rfc8628#section-3.1>"
 */
public class DeviceAuthorizationRequest {

    @VisibleForTesting
    static final String PARAM_CLIENT_ID = "client_id";

    @VisibleForTesting
    static final String PARAM_SCOPE = "scope";

    private static final Set<String> BUILT_IN_PARAMS = builtInParams(
            PARAM_CLIENT_ID,
            PARAM_SCOPE);

    private static final String KEY_CONFIGURATION = "configuration";
    private static final String KEY_CLIENT_ID = "clientId";
    private static final String KEY_SCOPE = "scope";
    private static final String KEY_ADDITIONAL_PARAMETERS = "additionalParameters";

    /**
     * The service's {@link AuthorizationServiceConfiguration configuration}.
     * This configuration specifies how to connect to a particular OAuth provider.
     * Configurations may be
     * {@link
     * AuthorizationServiceConfiguration#AuthorizationServiceConfiguration(Uri, Uri, Uri, Uri, Uri)}
     * created manually}, or {@link AuthorizationServiceConfiguration#fetchFromUrl(Uri,
     * AuthorizationServiceConfiguration.RetrieveConfigurationCallback)} via an OpenID Connect
     * Discovery Document}.
     */
    @NonNull
    public final AuthorizationServiceConfiguration configuration;

    /**
     * The client identifier.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4
     * <https://tools.ietf.org/html/rfc6749#section-4>"
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.1
     * <https://tools.ietf.org/html/rfc6749#section-4.1.1>"
     */
    @NonNull
    public final String clientId;

    /**
     * The optional set of scopes expressed as a space-delimited, case-sensitive string.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.2
     * <https://tools.ietf.org/html/rfc6749#section-3.1.2>"
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.3
     * <https://tools.ietf.org/html/rfc6749#section-3.3>"
     */
    @Nullable
    public final String scope;

    /**
     * Additional parameters to be passed as part of the request.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1
     * <https://tools.ietf.org/html/rfc6749#section-3.1>"
     */
    @NonNull
    public final Map<String, String> additionalParameters;

    /**
     * Creates instances of {@link DeviceAuthorizationRequest}.
     */
    public static final class Builder {

        // SuppressWarnings justification: static analysis incorrectly determines that this field
        // is not initialized, as it is indirectly initialized by setConfiguration
        @NonNull
        @SuppressWarnings("NullableProblems")
        private AuthorizationServiceConfiguration mConfiguration;

        // SuppressWarnings justification: static analysis incorrectly determines that this field
        // is not initialized, as it is indirectly initialized by setClientId
        @NonNull
        @SuppressWarnings("NullableProblems")
        private String mClientId;

        @Nullable
        private String mScope;

        @NonNull
        private Map<String, String> mAdditionalParameters = new HashMap<>();

        /**
         * Creates an authorization request builder with the specified mandatory properties.
         */
        public Builder(
                @NonNull AuthorizationServiceConfiguration configuration,
                @NonNull String clientId) {
            setAuthorizationServiceConfiguration(configuration);
            setClientId(clientId);
        }

        /**
         * Specifies the service configuration to be used in dispatching this request.
         */
        public Builder setAuthorizationServiceConfiguration(
                @NonNull AuthorizationServiceConfiguration configuration) {
            mConfiguration = checkNotNull(configuration,
                    "configuration cannot be null");
            return this;
        }

        /**
         * Specifies the client ID. Cannot be null or empty.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4
         * <https://tools.ietf.org/html/rfc6749#section-4>"
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.1
         * <https://tools.ietf.org/html/rfc6749#section-4.1.1>"
         */
        @NonNull
        public Builder setClientId(@NonNull String clientId) {
            mClientId = checkNotEmpty(clientId, "client ID cannot be null or empty");
            return this;
        }

        /**
         * Specifies the encoded scope string, which is a space-delimited set of
         * case-sensitive scope identifiers. Replaces any previously specified scope.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.3
         * <https://tools.ietf.org/html/rfc6749#section-3.3>"
         */
        @NonNull
        public Builder setScope(@Nullable String scope) {
            if (TextUtils.isEmpty(scope)) {
                mScope = null;
            } else {
                setScopes(scope.split(" +"));
            }
            return this;
        }

        /**
         * Specifies the set of case-sensitive scopes. Replaces any previously specified set of
         * scopes. If no arguments are provided, the scope string will be set to `null`.
         * Individual scope strings cannot be null or empty.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.3
         * <https://tools.ietf.org/html/rfc6749#section-3.3>"
         */
        @NonNull
        public Builder setScopes(String... scopes) {
            if (scopes == null) {
                scopes = new String[0];
            }
            setScopes(Arrays.asList(scopes));
            return this;
        }

        /**
         * Specifies the set of case-sensitive scopes. Replaces any previously specified set of
         * scopes. If the iterable is empty, the scope string will be set to `null`.
         * Individual scope strings cannot be null or empty.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.3
         * <https://tools.ietf.org/html/rfc6749#section-3.3>"
         */
        @NonNull
        public Builder setScopes(@Nullable Iterable<String> scopes) {
            mScope = AsciiStringListUtil.iterableToString(scopes);
            return this;
        }

        /**
         * Specifies additional parameters. Replaces any previously provided set of parameters.
         * Parameter keys and values cannot be null or empty.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1
         * <https://tools.ietf.org/html/rfc6749#section-3.1>"
         */
        @NonNull
        public Builder setAdditionalParameters(@Nullable Map<String, String> additionalParameters) {
            mAdditionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS);
            return this;
        }

        /**
         * Constructs the device authorization request. At a minimum the following fields must have
         * been set:
         *
         * - The client ID
         *
         * Failure to specify any of these parameters will result in a runtime exception.
         */
        @NonNull
        public DeviceAuthorizationRequest build() {
            return new DeviceAuthorizationRequest(
                    mConfiguration,
                    mClientId,
                    mScope,
                    Collections.unmodifiableMap(new HashMap<>(mAdditionalParameters)));
        }
    }

    private DeviceAuthorizationRequest(
            @NonNull AuthorizationServiceConfiguration configuration,
            @NonNull String clientId,
            @Nullable String scope,
            @NonNull Map<String, String> additionalParameters) {
        // mandatory fields
        this.configuration = configuration;
        this.clientId = clientId;
        this.additionalParameters = additionalParameters;

        // optional fields
        this.scope = scope;
    }

    /**
     * Derives the set of scopes from the consolidated, space-delimited scopes in the
     * {@link #scope} field. If no scopes were specified for this request, the method will
     * return `null`.
     */
    @Nullable
    public Set<String> getScopeSet() {
        return AsciiStringListUtil.stringToSet(scope);
    }

    /**
     * Produces the set of request parameters for this query, which can be further
     * processed into a request body.
     */
    @NonNull
    public Map<String, String> getRequestParameters() {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_CLIENT_ID, clientId);
        putIfNotNull(params, PARAM_SCOPE, scope);

        for (Entry<String, String> param : additionalParameters.entrySet()) {
            params.put(param.getKey(), param.getValue());
        }

        return params;
    }

    private void putIfNotNull(Map<String, String> map, String key, Object value) {
        if (value != null) {
            map.put(key, value.toString());
        }
    }

    /**
     * Produces a JSON representation of the device authorization request for persistent storage or
     * local transmission (e.g. between activities).
     */
    @NonNull
    public JSONObject jsonSerialize() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_CONFIGURATION, configuration.toJson());
        JsonUtil.put(json, KEY_CLIENT_ID, clientId);
        JsonUtil.putIfNotNull(json, KEY_SCOPE, scope);
        JsonUtil.put(json, KEY_ADDITIONAL_PARAMETERS,
                JsonUtil.mapToJsonObject(additionalParameters));
        return json;
    }

    /**
     * Produces a JSON string representation of the device authorization request for persistent
     * storage or local transmission (e.g. between activities). This method is just a convenience
     * wrapper for {@link #jsonSerialize()}, converting the JSON object to its string form.
     */
    @NonNull
    public String jsonSerializeString() {
        return jsonSerialize().toString();
    }

    /**
     * Reads a device authorization request from a JSON string representation produced by
     * {@link #jsonSerialize()}.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static DeviceAuthorizationRequest jsonDeserialize(@NonNull JSONObject json)
            throws JSONException {
        checkNotNull(json, "json cannot be null");

        return new DeviceAuthorizationRequest(
                AuthorizationServiceConfiguration.fromJson(json.getJSONObject(KEY_CONFIGURATION)),
                JsonUtil.getString(json, KEY_CLIENT_ID),
                JsonUtil.getStringIfDefined(json, KEY_SCOPE),
                JsonUtil.getStringMap(json, KEY_ADDITIONAL_PARAMETERS));
    }

    /**
     * Reads a device authorization request from a JSON string representation produced by
     * {@link #jsonSerializeString()}. This method is just a convenience wrapper for
     * {@link #jsonDeserialize(JSONObject)}, converting the JSON string to its JSON object form.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static DeviceAuthorizationRequest jsonDeserialize(@NonNull String jsonStr)
            throws JSONException {
        checkNotNull(jsonStr, "json string cannot be null");
        return jsonDeserialize(new JSONObject(jsonStr));
    }

}
