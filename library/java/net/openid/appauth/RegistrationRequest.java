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

import static net.openid.appauth.AdditionalParamsProcessor.builtInParams;
import static net.openid.appauth.AdditionalParamsProcessor.checkAdditionalParams;
import static net.openid.appauth.Preconditions.checkCollectionNotEmpty;
import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegistrationRequest {
    /**
     * OpenID Connect 'application_type'.
     */
    public static final String APPLICATION_TYPE_NATIVE = "native";

    static final String PARAM_REDIRECT_URIS = "redirect_uris";
    static final String PARAM_RESPONSE_TYPES = "response_types";
    static final String PARAM_GRANT_TYPES = "grant_types";
    static final String PARAM_APPLICATION_TYPE = "application_type";
    static final String PARAM_SUBJECT_TYPE = "subject_type";
    static final String PARAM_JWKS_URI = "jwks_uri";
    static final String PARAM_JWKS = "jwks";
    static final String PARAM_TOKEN_ENDPOINT_AUTHENTICATION_METHOD = "token_endpoint_auth_method";

    private static final Set<String> BUILT_IN_PARAMS = builtInParams(
            PARAM_REDIRECT_URIS,
            PARAM_RESPONSE_TYPES,
            PARAM_GRANT_TYPES,
            PARAM_APPLICATION_TYPE,
            PARAM_SUBJECT_TYPE,
            PARAM_JWKS_URI,
            PARAM_JWKS,
            PARAM_TOKEN_ENDPOINT_AUTHENTICATION_METHOD
    );

    static final String KEY_ADDITIONAL_PARAMETERS = "additionalParameters";
    static final String KEY_CONFIGURATION = "configuration";

    /**
     * Instructs the authorization server to generate a pairwise subject identifier.
     *
     * @see "OpenID Connect Core 1.0, Section 8
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.8>"
     */
    public static final String SUBJECT_TYPE_PAIRWISE = "pairwise";

    /**
     * Instructs the authorization server to generate a public subject identifier.
     *
     * @see "OpenID Connect Core 1.0, Section 8
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.8>"
     */
    public static final String SUBJECT_TYPE_PUBLIC = "public";

    /**
     * The service's {@link AuthorizationServiceConfiguration configuration}.
     * This configuration specifies how to connect to a particular OAuth provider.
     * Configurations may be
     * {@link
     * AuthorizationServiceConfiguration#AuthorizationServiceConfiguration(Uri, Uri, Uri, Uri)
     * created manually}, or
     * {@link AuthorizationServiceConfiguration#fetchFromUrl(Uri,
     * AuthorizationServiceConfiguration.RetrieveConfigurationCallback)
     * via an OpenID Connect Discovery Document}.
     */
    @NonNull
    public final AuthorizationServiceConfiguration configuration;

    /**
     * The client's redirect URI's.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.2
     * <https://tools.ietf.org/html/rfc6749#section-3.1.2>"
     */
    @NonNull
    public final List<Uri> redirectUris;

    /**
     * The application type to register, will always be 'native'.
     */
    @NonNull
    public final String applicationType;

    /**
     * The response types to use.
     *
     * @see "OpenID Connect Core 1.0, Section 3
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3>"
     */
    @Nullable
    public final List<String> responseTypes;

    /**
     * The grant types to use.
     *
     * @see "OpenID Connect Dynamic Client Registration 1.0, Section 2
     * <https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.2>"
     */
    @Nullable
    public final List<String> grantTypes;

    /**
     * The subject type to use.
     *
     * @see "OpenID Connect Core 1.0, Section 8 <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.8>"
     */
    @Nullable
    public final String subjectType;

    /**
     * URL for the Client's JSON Web Key Set [JWK] document.
     *
     * @see "OpenID Connect Dynamic Client Registration 1.0, Client Metadata
     * <https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata>"
     */
    @Nullable
    public final Uri jwksUri;

    /**
     * Client's JSON Web Key Set [JWK] document.
     *
     * @see "OpenID Connect Dynamic Client Registration 1.0, Client Metadata
     * <https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata>"
     */
    @Nullable
    public final JSONObject jwks;

    /**
     * The client authentication method to use at the token endpoint.
     *
     * @see "OpenID Connect Core 1.0, Section 9 <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.9>"
     */
    @Nullable
    public final String tokenEndpointAuthenticationMethod;

    /**
     * Additional parameters to be passed as part of the request.
     */
    @NonNull
    public final Map<String, String> additionalParameters;


    /**
     * Creates instances of {@link RegistrationRequest}.
     */
    public static final class Builder {
        @NonNull
        private AuthorizationServiceConfiguration mConfiguration;
        @NonNull
        private List<Uri> mRedirectUris = new ArrayList<>();

        @Nullable
        private List<String> mResponseTypes;

        @Nullable
        private List<String> mGrantTypes;

        @Nullable
        private String mSubjectType;

        @Nullable
        private Uri mJwksUri;

        @Nullable
        private JSONObject mJwks;

        @Nullable
        private String mTokenEndpointAuthenticationMethod;

        @NonNull
        private Map<String, String> mAdditionalParameters = Collections.emptyMap();


        /**
         * Creates a registration request builder with the specified mandatory properties.
         */
        public Builder(
                @NonNull AuthorizationServiceConfiguration configuration,
                @NonNull List<Uri> redirectUri) {
            setConfiguration(configuration);
            setRedirectUriValues(redirectUri);
        }

        /**
         * Specifies the authorization service configuration for the request, which must not
         * be null or empty.
         */
        @NonNull
        public Builder setConfiguration(@NonNull AuthorizationServiceConfiguration configuration) {
            mConfiguration = checkNotNull(configuration);
            return this;
        }

        /**
         * Specifies the redirect URI's.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1.2"> "The OAuth 2.0
         * Authorization Framework" (RFC 6749), Section 3.1.2</a>
         */
        @NonNull
        public Builder setRedirectUriValues(@NonNull Uri... redirectUriValues) {
            return setRedirectUriValues(Arrays.asList(redirectUriValues));
        }

        /**
         * Specifies the redirect URI's.
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.2
         * <https://tools.ietf.org/html/rfc6749#section-3.1.2>"
         */
        @NonNull
        public Builder setRedirectUriValues(@NonNull List<Uri> redirectUriValues) {
            checkCollectionNotEmpty(redirectUriValues, "redirectUriValues cannot be null");
            mRedirectUris = redirectUriValues;
            return this;
        }

        /**
         * Specifies the response types.
         *
         * @see "OpenID Connect Core 1.0, Section 3
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3>"
         */
        @NonNull
        public Builder setResponseTypeValues(@Nullable String... responseTypeValues) {
            return setResponseTypeValues(Arrays.asList(responseTypeValues));
        }

        /**
         * Specifies the response types.
         *
         * @see "OpenID Connect Core 1.0, Section X
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.X>"
         */
        @NonNull
        public Builder setResponseTypeValues(@Nullable List<String> responseTypeValues) {
            mResponseTypes = responseTypeValues;
            return this;
        }

        /**
         * Specifies the grant types.
         *
         * @see "OpenID Connect Dynamic Client Registration 1.0, Section 2
         * <https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.2>"
         */
        @NonNull
        public Builder setGrantTypeValues(@Nullable String... grantTypeValues) {
            return setGrantTypeValues(Arrays.asList(grantTypeValues));
        }

        /**
         * Specifies the grant types.
         *
         * @see "OpenID Connect Dynamic Client Registration 1.0, Section 2
         * <https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.2>"
         */
        @NonNull
        public Builder setGrantTypeValues(@Nullable List<String> grantTypeValues) {
            mGrantTypes = grantTypeValues;
            return this;
        }

        /**
         * Specifies the subject types.
         *
         * @see "OpenID Connect Core 1.0, Section 8
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.8>"
         */
        @NonNull
        public Builder setSubjectType(@Nullable String subjectType) {
            mSubjectType = subjectType;
            return this;
        }

        /**
         * Specifies the URL for the Client's JSON Web Key Set.
         *
         * @see "OpenID Connect Dynamic Client Registration 1.0, Client Metadata
         * <https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata>"
         */
        @NonNull
        public Builder setJwksUri(@Nullable Uri jwksUri) {
            mJwksUri = jwksUri;
            return this;
        }

        /**
         * Specifies the client's JSON Web Key Set.
         *
         * @see "OpenID Connect Dynamic Client Registration 1.0, Client Metadata
         * <https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata>"
         */
        @NonNull
        public Builder setJwks(@Nullable JSONObject jwks) {
            mJwks = jwks;
            return this;
        }

        /**
         * Specifies the client authentication method to use at the token endpoint.
         *
         * @see "OpenID Connect Core 1.0, Section 9
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.9>"
         */
        @NonNull
        public Builder setTokenEndpointAuthenticationMethod(
                @Nullable String tokenEndpointAuthenticationMethod) {
            this.mTokenEndpointAuthenticationMethod = tokenEndpointAuthenticationMethod;
            return this;
        }

        /**
         * Specifies additional parameters. Replaces any previously provided set of parameters.
         * Parameter keys and values cannot be null or empty.
         */
        @NonNull
        public Builder setAdditionalParameters(@Nullable Map<String, String> additionalParameters) {
            mAdditionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS);
            return this;
        }

        /**
         * Constructs the registration request. At a minimum, the redirect URI must have been
         * set before calling this method.
         */
        @NonNull
        public RegistrationRequest build() {
            return new RegistrationRequest(
                    mConfiguration,
                    Collections.unmodifiableList(mRedirectUris),
                    mResponseTypes == null
                            ? mResponseTypes : Collections.unmodifiableList(mResponseTypes),
                    mGrantTypes == null ? mGrantTypes : Collections.unmodifiableList(mGrantTypes),
                    mSubjectType,
                    mJwksUri,
                    mJwks,
                    mTokenEndpointAuthenticationMethod,
                    Collections.unmodifiableMap(mAdditionalParameters));
        }
    }

    private RegistrationRequest(
            @NonNull AuthorizationServiceConfiguration configuration,
            @NonNull List<Uri> redirectUris,
            @Nullable List<String> responseTypes,
            @Nullable List<String> grantTypes,
            @Nullable String subjectType,
            @Nullable Uri jwksUri,
            @Nullable JSONObject jwks,
            @Nullable String tokenEndpointAuthenticationMethod,
            @NonNull Map<String, String> additionalParameters) {
        this.configuration = configuration;
        this.redirectUris = redirectUris;
        this.responseTypes = responseTypes;
        this.grantTypes = grantTypes;
        this.subjectType = subjectType;
        this.jwksUri = jwksUri;
        this.jwks = jwks;
        this.tokenEndpointAuthenticationMethod = tokenEndpointAuthenticationMethod;
        this.additionalParameters = additionalParameters;
        this.applicationType = APPLICATION_TYPE_NATIVE;
    }

    /**
     * Converts the registration request to JSON for transmission to an authorization service.
     * For local persistence and transmission, use {@link #jsonSerialize()}.
     */
    @NonNull
    public String toJsonString() {
        JSONObject json = jsonSerializeParams();
        for (Map.Entry<String, String> param : additionalParameters.entrySet()) {
            JsonUtil.put(json, param.getKey(), param.getValue());
        }
        return json.toString();
    }

    /**
     * Produces a JSON representation of the registration request for persistent storage or
     * local transmission (e.g. between activities).
     */
    @NonNull
    public JSONObject jsonSerialize() {
        JSONObject json = jsonSerializeParams();
        JsonUtil.put(json, KEY_CONFIGURATION, configuration.toJson());
        JsonUtil.put(json, KEY_ADDITIONAL_PARAMETERS,
                JsonUtil.mapToJsonObject(additionalParameters));
        return json;
    }

    /**
     * Produces a JSON string representation of the registration request for persistent storage or
     * local transmission (e.g. between activities). This method is just a convenience wrapper
     * for {@link #jsonSerialize()}, converting the JSON object to its string form.
     */
    @NonNull
    public String jsonSerializeString() {
        return jsonSerialize().toString();
    }

    private JSONObject jsonSerializeParams() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, PARAM_REDIRECT_URIS, JsonUtil.toJsonArray(redirectUris));
        JsonUtil.put(json, PARAM_APPLICATION_TYPE, applicationType);

        if (responseTypes != null) {
            JsonUtil.put(json, PARAM_RESPONSE_TYPES, JsonUtil.toJsonArray(responseTypes));
        }
        if (grantTypes != null) {
            JsonUtil.put(json, PARAM_GRANT_TYPES, JsonUtil.toJsonArray(grantTypes));
        }
        JsonUtil.putIfNotNull(json, PARAM_SUBJECT_TYPE, subjectType);

        JsonUtil.putIfNotNull(json, PARAM_JWKS_URI, jwksUri);
        JsonUtil.putIfNotNull(json, PARAM_JWKS, jwks);

        JsonUtil.putIfNotNull(json, PARAM_TOKEN_ENDPOINT_AUTHENTICATION_METHOD,
                tokenEndpointAuthenticationMethod);
        return json;
    }

    /**
     * Reads a registration request from a JSON string representation produced by
     * {@link #jsonSerialize()}.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    public static RegistrationRequest jsonDeserialize(@NonNull JSONObject json)
            throws JSONException {
        checkNotNull(json, "json must not be null");

        return new RegistrationRequest(
            AuthorizationServiceConfiguration.fromJson(json.getJSONObject(KEY_CONFIGURATION)),
            JsonUtil.getUriList(json, PARAM_REDIRECT_URIS),
            JsonUtil.getStringListIfDefined(json, PARAM_RESPONSE_TYPES),
            JsonUtil.getStringListIfDefined(json, PARAM_GRANT_TYPES),
            JsonUtil.getStringIfDefined(json, PARAM_SUBJECT_TYPE),
            JsonUtil.getUriIfDefined(json, PARAM_JWKS_URI),
            JsonUtil.getJsonObjectIfDefined(json, PARAM_JWKS),
            JsonUtil.getStringIfDefined(json, PARAM_TOKEN_ENDPOINT_AUTHENTICATION_METHOD),
            JsonUtil.getStringMap(json, KEY_ADDITIONAL_PARAMETERS));
    }

    /**
     * Reads a registration request from a JSON string representation produced by
     * {@link #jsonSerializeString()}. This method is just a convenience wrapper for
     * {@link #jsonDeserialize(JSONObject)}, converting the JSON string to its JSON object form.
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    public static RegistrationRequest jsonDeserialize(@NonNull String jsonStr)
            throws JSONException {
        checkNotEmpty(jsonStr, "jsonStr must not be empty or null");
        return jsonDeserialize(new JSONObject(jsonStr));
    }
}
