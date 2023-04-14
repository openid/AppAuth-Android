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

import static net.openid.appauth.AdditionalParamsProcessor.checkAdditionalParams;
import static net.openid.appauth.AdditionalParamsProcessor.extractAdditionalParams;
import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;

import android.net.Uri;
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

public class RegistrationResponse {
    static final String PARAM_CLIENT_ID = "client_id";
    static final String PARAM_CLIENT_SECRET = "client_secret";
    static final String PARAM_CLIENT_SECRET_EXPIRES_AT = "client_secret_expires_at";
    static final String PARAM_REGISTRATION_ACCESS_TOKEN = "registration_access_token";
    static final String PARAM_REGISTRATION_CLIENT_URI = "registration_client_uri";
    static final String PARAM_CLIENT_ID_ISSUED_AT = "client_id_issued_at";
    static final String PARAM_TOKEN_ENDPOINT_AUTH_METHOD = "token_endpoint_auth_method";

    static final String KEY_REQUEST = "request";
    static final String KEY_ADDITIONAL_PARAMETERS = "additionalParameters";

    private static final Set<String> BUILT_IN_PARAMS = new HashSet<>(Arrays.asList(
            PARAM_CLIENT_ID,
            PARAM_CLIENT_SECRET,
            PARAM_CLIENT_SECRET_EXPIRES_AT,
            PARAM_REGISTRATION_ACCESS_TOKEN,
            PARAM_REGISTRATION_CLIENT_URI,
            PARAM_CLIENT_ID_ISSUED_AT,
            PARAM_TOKEN_ENDPOINT_AUTH_METHOD
    ));

    /**
     * The registration request associated with this response.
     */
    @NonNull
    public final RegistrationRequest request;

    /**
     * The registered client identifier.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4
     * <https://tools.ietf.org/html/rfc6749#section-4>"
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.1 <https://tools.ietf.org/html/rfc6749#section-4.1.1>"
     */
    @NonNull
    public final String clientId;

    /**
     * Timestamp of when the client identifier was issued, if provided.
     *
     * @see "OpenID Connect Dynamic Client Registration 1.0, Section 3.2
     * <https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.3.2>"
     */
    @Nullable
    public final Long clientIdIssuedAt;

    /**
     * The client secret, which is part of the client credentials, if provided.
     *
     * @see "OpenID Connect Dynamic Client Registration 1.0, Section 3.2
     * <https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.3.2>"
     */
    @Nullable
    public final String clientSecret;

    /**
     * Timestamp of when the client credentials expires, if provided.
     *
     * @see "OpenID Connect Dynamic Client Registration 1.0, Section 3.2
     * <https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.3.2>"
     */
    @Nullable
    public final Long clientSecretExpiresAt;

    /**
     * Client registration access token that can be used for subsequent operations upon the client
     * registration.
     *
     * @see "OpenID Connect Dynamic Client Registration 1.0, Section 3.2
     * <https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.3.2>"
     */
    @Nullable
    public final String registrationAccessToken;

    /**
     * Location of the client configuration endpoint, if provided.
     *
     * @see "OpenID Connect Dynamic Client Registration 1.0, Section 3.2
     * <https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.3.2>"
     */
    @Nullable
    public final Uri registrationClientUri;

    /**
     * Client authentication method to use at the token endpoint, if provided.
     *
     * @see "OpenID Connect Core 1.0, Section 9
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.9>"
     */
    @Nullable
    public final String tokenEndpointAuthMethod;

    /**
     * Additional, non-standard parameters in the response.
     */
    @NonNull
    public final Map<String, String> additionalParameters;

    /**
     * Thrown when a mandatory property is missing from the registration response.
     */
    public static class MissingArgumentException extends Exception {
        private String mMissingField;

        /**
         * Indicates that the specified mandatory field is missing from the registration response.
         */
        public MissingArgumentException(String field) {
            super("Missing mandatory registration field: " + field);
            mMissingField = field;
        }

        public String getMissingField() {
            return mMissingField;
        }
    }

    public static final class Builder {
        @NonNull
        private RegistrationRequest mRequest;
        @NonNull
        private String mClientId;

        @Nullable
        private Long mClientIdIssuedAt;
        @Nullable
        private String mClientSecret;
        @Nullable
        private Long mClientSecretExpiresAt;
        @Nullable
        private String mRegistrationAccessToken;
        @Nullable
        private Uri mRegistrationClientUri;
        @Nullable
        private String mTokenEndpointAuthMethod;

        @NonNull
        private Map<String, String> mAdditionalParameters = Collections.emptyMap();

        /**
         * Creates a token response associated with the specified request.
         */
        public Builder(@NonNull RegistrationRequest request) {
            setRequest(request);
        }

        /**
         * Specifies the request associated with this response. Must not be null.
         */
        @NonNull
        public Builder setRequest(@NonNull RegistrationRequest request) {
            mRequest = checkNotNull(request, "request cannot be null");
            return this;
        }

        /**
         * Specifies the client identifier.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4
         * <https://tools.ietf.org/html/rfc6749#section-4>"
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.1
         * <https://tools.ietf.org/html/rfc6749#section-4.1.1>"
         */
        public Builder setClientId(@NonNull String clientId) {
            checkNotEmpty(clientId, "client ID cannot be null or empty");
            mClientId = clientId;
            return this;
        }

        /**
         * Specifies the timestamp for when the client identifier was issued.
         *
         * @see "OpenID Connect Dynamic Client Registration 1.0, Section 3.2
         * <https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.3.2>"
         */
        public Builder setClientIdIssuedAt(@Nullable Long clientIdIssuedAt) {
            mClientIdIssuedAt = clientIdIssuedAt;
            return this;
        }

        /**
         * Specifies the client secret.
         *
         * @see "OpenID Connect Dynamic Client Registration 1.0, Section 3.2
         * <https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.3.2>"
         */
        public Builder setClientSecret(@Nullable String clientSecret) {
            mClientSecret = clientSecret;
            return this;
        }

        /**
         * Specifies the expiration time of the client secret.
         *
         * @see "OpenID Connect Dynamic Client Registration 1.0, Section 3.2
         * <https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.3.2>"
         */
        public Builder setClientSecretExpiresAt(@Nullable Long clientSecretExpiresAt) {
            mClientSecretExpiresAt = clientSecretExpiresAt;
            return this;
        }

        /**
         * Specifies the registration access token.
         *
         * @see "OpenID Connect Dynamic Client Registration 1.0, Section 3.2
         * <https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.3.2>"
         */
        public Builder setRegistrationAccessToken(@Nullable String registrationAccessToken) {
            mRegistrationAccessToken = registrationAccessToken;
            return this;
        }

        /**
         * Specifies the client authentication method to use at the token endpoint.
         */
        public Builder setTokenEndpointAuthMethod(@Nullable String tokenEndpointAuthMethod) {
            mTokenEndpointAuthMethod = tokenEndpointAuthMethod;
            return this;
        }

        /**
         * Specifies the client configuration endpoint.
         *
         * @see "OpenID Connect Dynamic Client Registration 1.0, Section 3.2
         * <https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.3.2>"
         */
        public Builder setRegistrationClientUri(@Nullable Uri registrationClientUri) {
            mRegistrationClientUri = registrationClientUri;
            return this;
        }

        /**
         * Specifies the additional, non-standard parameters received as part of the response.
         */
        public Builder setAdditionalParameters(Map<String, String> additionalParameters) {
            mAdditionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS);
            return this;
        }

        /**
         * Creates the token response instance.
         */
        public RegistrationResponse build() {
            return new RegistrationResponse(
                    mRequest,
                    mClientId,
                    mClientIdIssuedAt,
                    mClientSecret,
                    mClientSecretExpiresAt,
                    mRegistrationAccessToken,
                    mRegistrationClientUri,
                    mTokenEndpointAuthMethod,
                    mAdditionalParameters);
        }

        /**
         * Extracts registration response fields from a JSON string.
         *
         * @throws JSONException if the JSON is malformed or has incorrect value types for fields.
         * @throws MissingArgumentException if the JSON is missing fields required by the
         *     specification.
         */
        @NonNull
        public Builder fromResponseJsonString(@NonNull String jsonStr)
                throws JSONException, MissingArgumentException {
            checkNotEmpty(jsonStr, "json cannot be null or empty");
            return fromResponseJson(new JSONObject(jsonStr));
        }

        /**
         * Extracts token response fields from a JSON object.
         *
         * @throws JSONException if the JSON is malformed or has incorrect value types for fields.
         * @throws MissingArgumentException if the JSON is missing fields required by the
         *     specification.
         */
        @NonNull
        public Builder fromResponseJson(@NonNull JSONObject json)
                throws JSONException, MissingArgumentException {
            setClientId(JsonUtil.getString(json, PARAM_CLIENT_ID));
            setClientIdIssuedAt(JsonUtil.getLongIfDefined(json, PARAM_CLIENT_ID_ISSUED_AT));

            if (json.has(PARAM_CLIENT_SECRET)) {
                if (!json.has(PARAM_CLIENT_SECRET_EXPIRES_AT)) {
                    /*
                     * From OpenID Connect Dynamic Client Registration, Section 3.2:
                     * client_secret_expires_at: "REQUIRED if client_secret is issued"
                     */
                    throw new MissingArgumentException(PARAM_CLIENT_SECRET_EXPIRES_AT);
                }
                setClientSecret(json.getString(PARAM_CLIENT_SECRET));
                setClientSecretExpiresAt(json.getLong(PARAM_CLIENT_SECRET_EXPIRES_AT));
            }

            if (json.has(PARAM_REGISTRATION_ACCESS_TOKEN)
                    != json.has(PARAM_REGISTRATION_CLIENT_URI)) {
                /*
                 * From OpenID Connect Dynamic Client Registration, Section 3.2:
                 * "Implementations MUST either return both a Client Configuration Endpoint and a
                 * Registration Access Token or neither of them."
                 */
                String missingParameter = json.has(PARAM_REGISTRATION_ACCESS_TOKEN)
                        ? PARAM_REGISTRATION_CLIENT_URI : PARAM_REGISTRATION_ACCESS_TOKEN;
                throw new MissingArgumentException(missingParameter);
            }

            setRegistrationAccessToken(JsonUtil.getStringIfDefined(json,
                    PARAM_REGISTRATION_ACCESS_TOKEN));
            setRegistrationClientUri(JsonUtil.getUriIfDefined(json, PARAM_REGISTRATION_CLIENT_URI));
            setTokenEndpointAuthMethod(JsonUtil.getStringIfDefined(json,
                    PARAM_TOKEN_ENDPOINT_AUTH_METHOD));

            setAdditionalParameters(extractAdditionalParams(json, BUILT_IN_PARAMS));
            return this;
        }
    }

    private RegistrationResponse(
            @NonNull RegistrationRequest request,
            @NonNull String clientId,
            @Nullable Long clientIdIssuedAt,
            @Nullable String clientSecret,
            @Nullable Long clientSecretExpiresAt,
            @Nullable String registrationAccessToken,
            @Nullable Uri registrationClientUri,
            @Nullable String tokenEndpointAuthMethod,
            @NonNull Map<String, String> additionalParameters) {
        this.request = request;
        this.clientId = clientId;
        this.clientIdIssuedAt = clientIdIssuedAt;
        this.clientSecret = clientSecret;
        this.clientSecretExpiresAt = clientSecretExpiresAt;
        this.registrationAccessToken = registrationAccessToken;
        this.registrationClientUri = registrationClientUri;
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
        this.additionalParameters = additionalParameters;
    }

    /**
     * Reads a registration response JSON string received from an authorization server,
     * and associates it with the provided request.
     *
     * @throws JSONException if the JSON is malformed or missing required fields.
     * @throws MissingArgumentException if the JSON is missing fields required by the specification.
     */
    @NonNull
    public static RegistrationResponse fromJson(
            @NonNull RegistrationRequest request, @NonNull String jsonStr)
            throws JSONException, MissingArgumentException {
        checkNotEmpty(jsonStr, "jsonStr cannot be null or empty");
        return fromJson(request, new JSONObject(jsonStr));
    }

    /**
     * Reads a registration response JSON object received from an authorization server,
     * and associates it with the provided request.
     *
     * @throws JSONException if the JSON is malformed or missing required fields.
     * @throws MissingArgumentException if the JSON is missing fields required by the specification.
     */
    @NonNull
    public static RegistrationResponse fromJson(
            @NonNull RegistrationRequest request,
            @NonNull JSONObject json)
            throws JSONException, MissingArgumentException {
        checkNotNull(request, "registration request cannot be null");
        return new RegistrationResponse.Builder(request)
                .fromResponseJson(json)
                .build();
    }

    /**
     * Produces a JSON representation of the registration response for persistent storage or
     * local transmission (e.g. between activities).
     */
    @NonNull
    public JSONObject jsonSerialize() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_REQUEST, request.jsonSerialize());
        JsonUtil.put(json, PARAM_CLIENT_ID, clientId);
        JsonUtil.putIfNotNull(json, PARAM_CLIENT_ID_ISSUED_AT, clientIdIssuedAt);
        JsonUtil.putIfNotNull(json, PARAM_CLIENT_SECRET, clientSecret);
        JsonUtil.putIfNotNull(json, PARAM_CLIENT_SECRET_EXPIRES_AT, clientSecretExpiresAt);
        JsonUtil.putIfNotNull(json, PARAM_REGISTRATION_ACCESS_TOKEN, registrationAccessToken);
        JsonUtil.putIfNotNull(json, PARAM_REGISTRATION_CLIENT_URI, registrationClientUri);
        JsonUtil.putIfNotNull(json, PARAM_TOKEN_ENDPOINT_AUTH_METHOD, tokenEndpointAuthMethod);
        JsonUtil.put(json, KEY_ADDITIONAL_PARAMETERS,
                JsonUtil.mapToJsonObject(additionalParameters));
        return json;
    }

    /**
     * Produces a JSON string representation of the registration response for persistent storage or
     * local transmission (e.g. between activities). This method is just a convenience wrapper
     * for {@link #jsonSerialize()}, converting the JSON object to its string form.
     */
    @NonNull
    public String jsonSerializeString() {
        return jsonSerialize().toString();
    }

    /**
     * Reads a registration response from a JSON string representation produced by
     * {@link #jsonSerialize()}.
     *
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    public static RegistrationResponse jsonDeserialize(@NonNull JSONObject json)
            throws JSONException {
        checkNotNull(json, "json cannot be null");
        if (!json.has(KEY_REQUEST)) {
            throw new IllegalArgumentException("registration request not found in JSON");
        }

        return new RegistrationResponse(
                RegistrationRequest.jsonDeserialize(json.getJSONObject(KEY_REQUEST)),
                JsonUtil.getString(json, PARAM_CLIENT_ID),
                JsonUtil.getLongIfDefined(json, PARAM_CLIENT_ID_ISSUED_AT),
                JsonUtil.getStringIfDefined(json, PARAM_CLIENT_SECRET),
                JsonUtil.getLongIfDefined(json, PARAM_CLIENT_SECRET_EXPIRES_AT),
                JsonUtil.getStringIfDefined(json, PARAM_REGISTRATION_ACCESS_TOKEN),
                JsonUtil.getUriIfDefined(json, PARAM_REGISTRATION_CLIENT_URI),
                JsonUtil.getStringIfDefined(json, PARAM_TOKEN_ENDPOINT_AUTH_METHOD),
                JsonUtil.getStringMap(json, KEY_ADDITIONAL_PARAMETERS));
    }

    /**
     * Reads a registration response from a JSON string representation produced by
     * {@link #jsonSerializeString()}. This method is just a convenience wrapper for
     * {@link #jsonDeserialize(JSONObject)}, converting the JSON string to its JSON object form.
     *
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @NonNull
    public static RegistrationResponse jsonDeserialize(@NonNull String jsonStr)
            throws JSONException {
        checkNotEmpty(jsonStr, "jsonStr cannot be null or empty");
        return jsonDeserialize(new JSONObject(jsonStr));
    }

    /**
     * Determines whether the returned access token has expired.
     */
    public boolean hasClientSecretExpired() {
        return hasClientSecretExpired(SystemClock.INSTANCE);
    }

    @VisibleForTesting
    boolean hasClientSecretExpired(@NonNull Clock clock) {
        Long now = TimeUnit.MILLISECONDS.toSeconds(checkNotNull(clock).getCurrentTimeMillis());
        return clientSecretExpiresAt != null && now > clientSecretExpiresAt;

    }
}
