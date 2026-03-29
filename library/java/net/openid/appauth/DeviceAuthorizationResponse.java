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

import static net.openid.appauth.AdditionalParamsProcessor.checkAdditionalParams;
import static net.openid.appauth.AdditionalParamsProcessor.extractAdditionalParams;
import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;
import static net.openid.appauth.Preconditions.checkNullOrNotEmpty;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A response to a device authorization request.
 *
 * @see DeviceAuthorizationRequest
 * @see "OAuth 2.0 Device Grant (RFC 8628), Section 3.2
 * <https://tools.ietf.org/html/rfc8628#section-3.2>"
 */
public class DeviceAuthorizationResponse {

    @VisibleForTesting
    static final String KEY_REQUEST = "request";

    @VisibleForTesting
    static final String KEY_DEVICE_CODE = "device_code";

    @VisibleForTesting
    static final String KEY_USER_CODE = "user_code";

    @VisibleForTesting
    static final String KEY_VERIFICATION_URI = "verification_uri";

    @VisibleForTesting
    static final String KEY_VERIFICATION_URI_COMPLETE = "verification_uri_complete";

    @VisibleForTesting
    static final String KEY_EXPIRES_IN = "expires_in";

    @VisibleForTesting
    static final String KEY_INTERVAL = "interval";

    @VisibleForTesting
    static final String KEY_ADDITIONAL_PARAMETERS = "additionalParameters";

    private static final Set<String> BUILT_IN_PARAMS = new HashSet<>(Arrays.asList(
            KEY_DEVICE_CODE,
            KEY_USER_CODE,
            KEY_VERIFICATION_URI,
            KEY_VERIFICATION_URI_COMPLETE,
            KEY_EXPIRES_IN,
            KEY_INTERVAL
    ));

    /**
     * The device authorization request associated with this response.
     */
    @NonNull
    public final DeviceAuthorizationRequest request;

    /**
     * The device verification code.
     *
     * @see "OAuth 2.0 Device Grant (RFC 8628), Section 3.2
     * <https://tools.ietf.org/html/rfc8628#section-3.2>"
     */
    @Nullable
    public final String deviceCode;

    /**
     * The end-user verification code.
     *
     * @see "OAuth 2.0 Device Grant (RFC 8628), Section 3.2
     * <https://tools.ietf.org/html/rfc8628#section-3.2>"
     */
    @Nullable
    public final String userCode;

    /**
     * The end-user verification URI on the authorization server.
     *
     * @see "OAuth 2.0 Device Grant (RFC 8628), Section 3.2
     * <https://tools.ietf.org/html/rfc8628#section-3.2>"
     */
    @Nullable
    public final String verificationUri;

    /**
     * A verification URI that includes the "user_code" (or other information with the same
     * function as the "user_code"), which is designed for non-textual transmission.
     *
     * @see "OAuth 2.0 Device Grant (RFC 8628), Section 3.2
     * <https://tools.ietf.org/html/rfc8628#section-3.2>"
     */
    @Nullable
    public final String verificationUriComplete;

    /**
     * The expiration time of the {@link #deviceCode} and {@link #userCode}.
     *
     * @see "OAuth 2.0 Device Grant (RFC 8628), Section 3.2
     * <https://tools.ietf.org/html/rfc8628#section-3.2>"
     */
    @Nullable
    public final Long codeExpirationTime;

    /**
     * The minimum amount of time in seconds that the client SHOULD wait between polling requests
     * to the token endpoint. If no value is provided, clients MUST use 5 as the default.
     *
     * @see "OAuth 2.0 Device Grant (RFC 8628), Section 3.2
     * <https://tools.ietf.org/html/rfc8628#section-3.2>"
     */
    @Nullable
    public final Long tokenPollingIntervalTime;

    /**
     * Additional, non-standard parameters in the response.
     */
    @NonNull
    public final Map<String, String> additionalParameters;

    /**
     * Creates instances of {@link DeviceAuthorizationResponse}.
     */
    public static final class Builder {
        @NonNull
        private DeviceAuthorizationRequest mRequest;

        @Nullable
        private String mDeviceCode;

        @Nullable
        private String mUserCode;

        @Nullable
        private String mVerificationUri;

        @Nullable
        private String mVerificationUriComplete;

        @Nullable
        private Long mCodeExpirationTime;

        @Nullable
        private Long mTokenPollingIntervalTime;

        @NonNull
        private Map<String, String> mAdditionalParameters;

        /**
         * Creates a device authorization response associated with the specified request.
         */
        public Builder(@NonNull DeviceAuthorizationRequest request) {
            setRequest(request);
            mAdditionalParameters = Collections.emptyMap();
        }

        /**
         * Extracts device authorization response fields from a JSON string.
         *
         * @throws JSONException if the JSON is malformed or has incorrect value types for fields.
         */
        @NonNull
        public Builder fromResponseJsonString(@NonNull String jsonStr) throws JSONException {
            checkNotEmpty(jsonStr, "json cannot be null or empty");
            return fromResponseJson(new JSONObject(jsonStr));
        }

        /**
         * Extracts device authorization response fields from a JSON object.
         *
         * @throws JSONException if the JSON is malformed or has incorrect value types for fields.
         */
        @NonNull
        public Builder fromResponseJson(@NonNull JSONObject json) throws JSONException {
            setDeviceCode(JsonUtil.getString(json, KEY_DEVICE_CODE));
            setUserCode(JsonUtil.getString(json, KEY_USER_CODE));
            setVerificationUri(JsonUtil.getString(json, KEY_VERIFICATION_URI));
            setVerificationUriComplete(
                    JsonUtil.getStringIfDefined(json, KEY_VERIFICATION_URI_COMPLETE));
            setCodeExpiresIn(json.getLong(KEY_EXPIRES_IN));
            if (json.has(KEY_INTERVAL)) {
                setTokenPollingIntervalTime(json.getLong(KEY_INTERVAL));
            }
            setAdditionalParameters(extractAdditionalParams(json, BUILT_IN_PARAMS));

            return this;
        }

        /**
         * Specifies the request associated with this response. Must not be null.
         */
        @NonNull
        public Builder setRequest(@NonNull DeviceAuthorizationRequest request) {
            mRequest = checkNotNull(request, "request cannot be null");
            return this;
        }

        /**
         * Specifies the device code associated with this response. If not null, the value must
         * be non-empty
         */
        @NonNull
        public Builder setDeviceCode(@Nullable String deviceCode) {
            mDeviceCode = checkNullOrNotEmpty(deviceCode,
                    "device code must not be empty if defined");
            return this;
        }

        /**
         * Specifies the user code associated with this response. If not null, the value must
         * be non-empty
         */
        @NonNull
        public Builder setUserCode(@Nullable String userCode) {
            mUserCode = checkNullOrNotEmpty(userCode, "user code must not be empty if defined");
            return this;
        }

        /**
         * Specifies the verification uri associated with this response. If not null, the value
         * must be non-empty
         */
        @NonNull
        public Builder setVerificationUri(@Nullable String verificationUri) {
            mVerificationUri = checkNullOrNotEmpty(verificationUri,
                    "verification uri must not be empty if defined");
            return this;
        }

        /**
         * Specifies the complete verification uri associated with this response. If not null, the
         * value must be non-empty
         */
        @NonNull
        public Builder setVerificationUriComplete(@Nullable String verificationUriComplete) {
            mVerificationUriComplete = checkNullOrNotEmpty(verificationUriComplete,
                    "complete verification uri must not be empty if defined");
            return this;
        }

        /**
         * Specifies the relative expiration time of the user code, in seconds, using the default
         * system clock as the source of the current time.
         */
        @NonNull
        public Builder setCodeExpiresIn(@Nullable Long expiresIn) {
            return setCodeExpiresIn(expiresIn, SystemClock.INSTANCE);
        }

        /**
         * Specifies the relative expiration time of the user code, in seconds, using the
         * provided clock as the source of the current time.
         */
        @NonNull
        @VisibleForTesting
        public Builder setCodeExpiresIn(@Nullable Long expiresIn, @NonNull Clock clock) {
            if (expiresIn == null) {
                mCodeExpirationTime = null;
            } else {
                mCodeExpirationTime = clock.getCurrentTimeMillis()
                    + TimeUnit.SECONDS.toMillis(expiresIn);
            }
            return this;
        }

        /**
         * Specifies the expiration time of the user code.
         */
        @NonNull
        public Builder setCodeExpirationTime(@Nullable Long expirationTime) {
            mCodeExpirationTime = expirationTime;
            return this;
        }

        /**
         * Sets the token polling interval time, in seconds.
         */
        @NonNull
        public Builder setTokenPollingIntervalTime(@Nullable Long interval) {
            mTokenPollingIntervalTime = interval;
            return this;
        }

        /**
         * Specifies the additional, non-standard parameters received as part of the response.
         */
        @NonNull
        public Builder setAdditionalParameters(@Nullable Map<String, String> additionalParameters) {
            mAdditionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS);
            return this;
        }

        /**
         * Creates the device authorization response.
         */
        public DeviceAuthorizationResponse build() {
            return new DeviceAuthorizationResponse(
                    mRequest,
                    mDeviceCode,
                    mUserCode,
                    mVerificationUri,
                    mVerificationUriComplete,
                    mCodeExpirationTime,
                    mTokenPollingIntervalTime,
                    mAdditionalParameters);
        }
    }

    DeviceAuthorizationResponse(
            @NonNull DeviceAuthorizationRequest request,
            @Nullable String deviceCode,
            @Nullable String userCode,
            @Nullable String verificationUri,
            @Nullable String verificationUriComplete,
            @Nullable Long codeExpirationTime,
            @Nullable Long tokenPollingIntervalTime,
            @NonNull Map<String, String> additionalParameters) {
        this.request = request;
        this.deviceCode = deviceCode;
        this.userCode = userCode;
        this.verificationUri = verificationUri;
        this.verificationUriComplete = verificationUriComplete;
        this.codeExpirationTime = codeExpirationTime;
        this.tokenPollingIntervalTime = tokenPollingIntervalTime;
        this.additionalParameters = additionalParameters;
    }

    /**
     * Determines whether the returned user code has expired.
     */
    public boolean hasCodeExpired() {
        return hasCodeExpired(SystemClock.INSTANCE);
    }

    @VisibleForTesting
    boolean hasCodeExpired(@NonNull Clock clock) {
        return codeExpirationTime != null
            && checkNotNull(clock).getCurrentTimeMillis() > codeExpirationTime;
    }

    /**
     * Creates a follow-up request to exchange a received authorization code for tokens.
     */
    @NonNull
    public TokenRequest createTokenExchangeRequest() {
        return createTokenExchangeRequest(Collections.<String, String>emptyMap());
    }

    /**
     * Creates a follow-up request to exchange a received authorization code for tokens, including
     * the provided additional parameters.
     */
    @NonNull
    public TokenRequest createTokenExchangeRequest(
                @NonNull Map<String, String> additionalExchangeParameters) {
        checkNotNull(additionalExchangeParameters,
                "additionalExchangeParameters cannot be null");

        if (deviceCode == null) {
            throw new IllegalStateException("deviceCode not available for exchange request");
        }

        return new TokenRequest.Builder(
            request.configuration,
            request.clientId)
            .setGrantType(GrantTypeValues.DEVICE_CODE)
            .setDeviceCode(deviceCode)
            .setAdditionalParameters(additionalExchangeParameters)
            .build();
    }

    /**
     * Produces a JSON string representation of the device authorization response for
     * persistent storage or local transmission (e.g. between activities).
     */
    public JSONObject jsonSerialize() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_REQUEST, request.jsonSerialize());
        JsonUtil.putIfNotNull(json, KEY_DEVICE_CODE, deviceCode);
        JsonUtil.putIfNotNull(json, KEY_USER_CODE, userCode);
        JsonUtil.putIfNotNull(json, KEY_VERIFICATION_URI, verificationUri);
        JsonUtil.putIfNotNull(json, KEY_VERIFICATION_URI_COMPLETE, verificationUriComplete);
        JsonUtil.putIfNotNull(json, KEY_EXPIRES_IN, codeExpirationTime);
        JsonUtil.putIfNotNull(json, KEY_INTERVAL, tokenPollingIntervalTime);
        JsonUtil.put(json, KEY_ADDITIONAL_PARAMETERS,
                JsonUtil.mapToJsonObject(additionalParameters));
        return json;
    }

    /**
     * Produces a JSON string representation of the device authorization response for persistent
     * storage or local transmission (e.g. between activities). This method is just a convenience
     * wrapper for {@link #jsonSerialize()}, converting the JSON object to its string form.
     */
    public String jsonSerializeString() {
        return jsonSerialize().toString();
    }

    /**
     * Reads a device authorization response from a JSON string, and associates it with the
     * provided request. If a request is not provided, its serialized form is expected to be found
     * in the JSON (as if produced by a prior call to {@link #jsonSerialize()}.
     * @throws JSONException if the JSON is malformed or missing required fields.
     */
    @NonNull
    public static DeviceAuthorizationResponse jsonDeserialize(
            @NonNull JSONObject json) throws JSONException {
        if (!json.has(KEY_REQUEST)) {
            throw new IllegalArgumentException(
                    "device authorization request not provided and not found in JSON");
        }
        return new DeviceAuthorizationResponse(
                DeviceAuthorizationRequest.jsonDeserialize(json.getJSONObject(KEY_REQUEST)),
                JsonUtil.getStringIfDefined(json, KEY_DEVICE_CODE),
                JsonUtil.getStringIfDefined(json, KEY_USER_CODE),
                JsonUtil.getStringIfDefined(json, KEY_VERIFICATION_URI),
                JsonUtil.getStringIfDefined(json, KEY_VERIFICATION_URI_COMPLETE),
                JsonUtil.getLongIfDefined(json, KEY_EXPIRES_IN),
                JsonUtil.getLongIfDefined(json, KEY_INTERVAL),
                JsonUtil.getStringMap(json, KEY_ADDITIONAL_PARAMETERS));
    }

    /**
     * Reads a device authorization response from a JSON string, and associates it with the
     * provided request. If a request is not provided, its serialized form is expected to be found
     * in the JSON (as if produced by a prior call to {@link #jsonSerialize()}.
     * @throws JSONException if the JSON is malformed or missing required fields.
     */
    @NonNull
    public static DeviceAuthorizationResponse jsonDeserialize(
            @NonNull String jsonStr) throws JSONException {
        checkNotEmpty(jsonStr, "jsonStr cannot be null or empty");
        return jsonDeserialize(new JSONObject(jsonStr));
    }
}
