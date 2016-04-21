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

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import net.openid.appauth.JsonUtil.BooleanField;
import net.openid.appauth.JsonUtil.Field;
import net.openid.appauth.JsonUtil.StringField;
import net.openid.appauth.JsonUtil.StringListField;
import net.openid.appauth.JsonUtil.UriField;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An OpenID Connect 1.0 Discovery Document.
 *
 * @see <a href=https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata>
 * "OpenID Provider Metadata", OpenID Connect Discovery 1.0, Section 3</a>
 */
public class AuthorizationServiceDiscovery {

    @VisibleForTesting
    static final StringField ISSUER = str("issuer");

    @VisibleForTesting
    static final UriField AUTHORIZATION_ENDPOINT = uri("authorization_endpoint");

    @VisibleForTesting
    static final UriField TOKEN_ENDPOINT = uri("token_endpoint");

    @VisibleForTesting
    static final UriField USERINFO_ENDPOINT = uri("userinfo_endpoint");

    @VisibleForTesting
    static final UriField JWKS_URI = uri("jwks_uri");

    @VisibleForTesting
    static final UriField REGISTRATION_ENDPOINT = uri("registration_endpoint");

    @VisibleForTesting
    static final StringListField SCOPES_SUPPORTED = strList("scopes_supported");

    @VisibleForTesting
    static final StringListField RESPONSE_TYPES_SUPPORTED = strList("response_types_supported");

    @VisibleForTesting
    static final StringListField RESPONSE_MODES_SUPPORTED = strList("response_modes_supported");

    @VisibleForTesting
    static final StringListField GRANT_TYPES_SUPPORTED =
            strList("grant_types_supported", Arrays.asList("authorization_code", "implicit"));

    @VisibleForTesting
    static final StringListField ACR_VALUES_SUPPORTED = strList("acr_values_supported");

    @VisibleForTesting
    static final StringListField SUBJECT_TYPES_SUPPORTED = strList("subject_types_supported");

    @VisibleForTesting
    static final StringListField ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED =
            strList("id_token_signing_alg_values_supported");

    @VisibleForTesting
    static final StringListField ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED =
            strList("id_token_encryption_enc_values_supported");

    @VisibleForTesting
    static final StringListField ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED =
            strList("id_token_encryption_enc_values_supported");

    @VisibleForTesting
    static final StringListField USERINFO_SIGNING_ALG_VALUES_SUPPORTED =
            strList("userinfo_signing_alg_values_supported");

    @VisibleForTesting
    static final StringListField USERINFO_ENCRYPTION_ALG_VALUES_SUPPORTED =
            strList("userinfo_encryption_alg_values_supported");

    @VisibleForTesting
    static final StringListField USERINFO_ENCRYPTION_ENC_VALUES_SUPPORTED =
            strList("userinfo_encryption_enc_values_supported");

    @VisibleForTesting
    static final StringListField REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED =
            strList("request_object_signing_alg_values_supported");

    @VisibleForTesting
    static final StringListField REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED =
            strList("request_object_encryption_alg_values_supported");

    @VisibleForTesting
    static final StringListField REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED =
            strList("request_object_encryption_enc_values_supported");

    @VisibleForTesting
    static final StringListField TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED =
            strList("token_endpoint_auth_methods_supported",
                    Collections.singletonList("client_secret_basic"));

    @VisibleForTesting
    static final StringListField TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED =
            strList("token_endpoint_auth_signing_alg_values_supported");

    @VisibleForTesting
    static final StringListField DISPLAY_VALUES_SUPPORTED = strList("display_values_supported");

    @VisibleForTesting
    static final StringListField CLAIM_TYPES_SUPPORTED =
            strList("claim_types_supported", Collections.singletonList("normal"));

    @VisibleForTesting
    static final StringListField CLAIMS_SUPPORTED = strList("claims_supported");

    @VisibleForTesting
    static final UriField SERVICE_DOCUMENTATION = uri("service_documentation");

    @VisibleForTesting
    static final StringListField CLAIMS_LOCALES_SUPPORTED = strList("claims_locales_supported");

    @VisibleForTesting
    static final StringListField UI_LOCALES_SUPPORTED = strList("ui_locales_supported");

    @VisibleForTesting
    static final BooleanField CLAIMS_PARAMETER_SUPPORTED =
            bool("claims_parameter_supported", false);

    @VisibleForTesting
    static final BooleanField REQUEST_PARAMETER_SUPPORTED =
            bool("request_parameter_supported", false);

    @VisibleForTesting
    static final BooleanField REQUEST_URI_PARAMETER_SUPPORTED =
            bool("request_uri_parameter_supported", true);

    @VisibleForTesting
    static final BooleanField REQUIRE_REQUEST_URI_REGISTRATION =
            bool("require_request_uri_registration", false);

    @VisibleForTesting
    static final UriField OP_POLICY_URI = uri("op_policy_uri");

    @VisibleForTesting
    static final UriField OP_TOS_URI = uri("op_tos_uri");

    /**
     * The fields which are marked as mandatory in the OpenID discovery spec.
     */
    private static final List<String> MANDATORY_METADATA = Arrays.asList(
            ISSUER.key,
            AUTHORIZATION_ENDPOINT.key,
            JWKS_URI.key,
            RESPONSE_TYPES_SUPPORTED.key,
            SUBJECT_TYPES_SUPPORTED.key,
            ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED.key);

    /**
     * The JSON representation of the discovery document.
     */
    @NonNull
    public final JSONObject docJson;

    /**
     * Extracts a discovery document from its standard JSON representation.
     * @throws JSONException if the provided JSON does not match the expected structure.
     * @throws MissingArgumentException if a mandatory property is missing from the discovery
     *     document.
     */
    public AuthorizationServiceDiscovery(@NonNull JSONObject discoveryDoc)
            throws JSONException, MissingArgumentException {
        this.docJson = checkNotNull(discoveryDoc);
        for (String mandatory : MANDATORY_METADATA) {
            if (!this.docJson.has(mandatory) || this.docJson.get(mandatory) == null) {
                throw new MissingArgumentException(mandatory);
            }
        }
    }

    /**
     * Thrown when a mandatory property is missing from the discovery document.
     */
    public static class MissingArgumentException extends Exception {
        private String mMissingField;

        /**
         * Indicates that the specified mandatory field is missing from the discovery document.
         */
        public MissingArgumentException(String field) {
            super("Missing mandatory configuration field: " + field);
            mMissingField = field;
        }

        public String getMissingField() {
            return mMissingField;
        }
    }

    /**
     * Retrieves a metadata value from the discovery document. This need only be used
     * for the retrieval of a non-standard metadata value. Convenience methods are defined on this
     * class for all standard metadata values.
     */
    private <T> T get(Field<T> field) {
        return JsonUtil.get(docJson, field);
    }

    /**
     * Retrieves a metadata value from the discovery document. This need only be used
     * for the retrieval of a non-standard metadata value. Convenience methods are defined on this
     * class for all standard metadata values.
     */
    private <T> List<T> get(JsonUtil.ListField<T> field) {
        return JsonUtil.get(docJson, field);
    }

    /**
     * The asserted issuer identifier.
     *
     * @see <a href="https://openid.net/specs/openid-connect-discovery-1_0.html#IssuerDiscovery">
     * "OpenID Connect Dynamic Client Registration 1.0"</a>
     */
    @NonNull
    public String getIssuer() {
        return get(ISSUER);
    }

    /**
     * The OAuth 2 authorization endpoint URI.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthorizationEndpoint">
     * "OpenID Connect Dynamic Client Registration 1.0"</a>
     */
    @NonNull
    public Uri getAuthorizationEndpoint() {
        return get(AUTHORIZATION_ENDPOINT);
    }

    /**
     * The OAuth 2 token endpoint URI. Not specified if only the implicit flow is used.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#TokenEndpoint">
     * "OpenID Connect Dynamic Client Registration 1.0"</a>
     */
    @Nullable
    public Uri getTokenEndpoint() {
        return get(TOKEN_ENDPOINT);
    }

    /**
     * The OpenID Connect UserInfo endpoint URI.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#UserInfo">
     * "OpenID Connect Dynamic Client Registration 1.0"</a>
     */
    @Nullable
    public Uri getUserinfoEndpoint() {
        return get(USERINFO_ENDPOINT);
    }

    /**
     * The JSON web key set document URI.
     *
     * @see <a href="http://tools.ietf.org/html/rfc7517">"JSON Web Key (JWK)"</a>
     */
    @NonNull
    public Uri getJwksUri() {
        return get(JWKS_URI);
    }

    /**
     * The dynamic client registration endpoint URI.
     *
     * @see <a href="http://openid.net/specs/openid-connect-registration-1_0.html">"OpenID Connect
     * Dynamic Client Registration 1.0"</a>
     */
    @Nullable
    public Uri getRegistrationEndpoint() {
        return get(REGISTRATION_ENDPOINT);
    }

    /**
     * The OAuth 2 scope values supported.
     *
     * @see <a href="http://tools.ietf.org/html/rfc6749#section-3.3">"The OAuth 2.0 Authorization
     * Framework"</a>
     */

    public List<String> getScopesSupported() {
        return get(SCOPES_SUPPORTED);
    }

    /**
     * The OAuth 2 {@code response_type} values supported.
     */
    @NonNull
    public List<String> getResponseTypesSupported() {
        return get(RESPONSE_TYPES_SUPPORTED);
    }

    /**
     * The OAuth 2 {@code response_mode} values supported.
     *
     * @see <a href="http://openid.net/specs/oauth-v2-multiple-response-types-1_0.html">
     * "OAuth 2.0 Multiple Response Type Encoding Practices"</a>
     */
    @Nullable
    public List<String> getResponseModesSupported() {
        return get(RESPONSE_MODES_SUPPORTED);
    }

    /**
     * The OAuth 2 {@code grant_type} values supported. Defaults to
     * {@code authorization_code} and {@code implicit} if not specified in the discovery document,
     * as suggested by the discovery specification.
     */
    @NonNull
    public List<String> getGrantTypesSupported() {
        return get(GRANT_TYPES_SUPPORTED);
    }

    /**
     * The authentication context class references supported.
     */
    public List<String> getAcrValuesSupported() {
        return get(ACR_VALUES_SUPPORTED);
    }

    /**
     * The subject identifier types supported.
     */
    @NonNull
    public List<String> getSubjectTypesSupported() {
        return get(SUBJECT_TYPES_SUPPORTED);
    }

    /**
     * The JWS signing algorithms (alg values) supported for encoding ID token claims.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7519">"JSON Web Token (JWT)"</a>
     */
    @NonNull
    public List<String> getIdTokenSigningAlgorithmValuesSupported() {
        return get(ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);
    }

    /**
     * The JWE encryption algorithms (alg values) supported for encoding ID token claims.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7519">"JSON Web Token (JWT)"</a>
     */
    @Nullable
    public List<String> getIdTokenEncryptionAlgorithmValuesSupported() {
        return get(ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED);
    }

    /**
     * The JWE encryption encodings (enc values) supported for encoding ID token claims.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7519">"JSON Web Token (JWT)"</a>
     */
    @Nullable
    public List<String> getIdTokenEncryptionEncodingValuesSupported() {
        return get(ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED);
    }

    /**
     * The JWS signing algorithms (alg values) supported by the UserInfo Endpoint
     * for encoding ID token claims.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7515">"JSON Web Signature (JWS)"</a>
     * @see <a href="https://tools.ietf.org/html/rfc7518">"JSON Web Algorithms (JWA)"</a>
     * @see <a href="https://tools.ietf.org/html/rfc7519">"JSON Web Token (JWT)"</a>
     */
    @Nullable
    public List<String> getUserinfoSigningAlgorithmValuesSupported() {
        return get(USERINFO_SIGNING_ALG_VALUES_SUPPORTED);
    }

    /**
     * The JWE encryption algorithms (alg values) supported by the UserInfo Endpoint
     * for encoding ID token claims.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7515">"JSON Web Signature (JWS)"</a>
     * @see <a href="https://tools.ietf.org/html/rfc7518">"JSON Web Algorithms (JWA)"</a>
     * @see <a href="https://tools.ietf.org/html/rfc7519">"JSON Web Token (JWT)"</a>
     */
    @Nullable
    public List<String> getUserinfoEncryptionAlgorithmValuesSupported() {
        return get(USERINFO_ENCRYPTION_ALG_VALUES_SUPPORTED);
    }

    /**
     * The JWE encryption encodings (enc values) supported by the UserInfo Endpoint
     * for encoding ID token claims.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7519">"JSON Web Token (JWT)"</a>
     */
    @Nullable
    public List<String> getUserinfoEncryptionEncodingValuesSupported() {
        return get(USERINFO_ENCRYPTION_ENC_VALUES_SUPPORTED);
    }

    /**
     * The JWS signing algorithms (alg values) supported for Request Objects.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#RequestObject">
     * "OpenID Connect Core 1.0", Section 6.1</a>
     */
    public List<String> getRequestObjectSigningAlgorithmValuesSupported() {
        return get(REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED);
    }

    /**
     * The JWE encryption algorithms (alg values) supported for Request Objects.
     */
    @Nullable
    public List<String> getRequestObjectEncryptionAlgorithmValuesSupported() {
        return get(REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED);
    }

    /**
     * The JWE encryption encodings (enc values) supported for Request Objects.
     */
    @Nullable
    public List<String> getRequestObjectEncryptionEncodingValuesSupported() {
        return get(REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED);
    }

    /**
     * The client authentication methods supported by the token endpoint. Defaults to
     * {@code client_secret_basic} if the discovery document does not specify a value, as suggested
     * by the discovery specification.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html">
     * "OpenID Connect Core 1.0"</a>
     * @see <a href="http://tools.ietf.org/html/rfc6749#section-2.3.1">
     * "The OAuth 2.0 Authorization Framework"</a>
     */
    @NonNull
    public List<String> getTokenEndpointAuthMethodsSupported() {
        return get(TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED);
    }

    /**
     * The JWS signing algorithms (alg values) supported by the token endpoint for the signature on
     * the JWT used to authenticate the client for the {@code private_key_jwt} and
     * {@code client_secret_jwt} authentication methods.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7519">"JSON Web Token (JWT)"</a>
     */
    @Nullable
    public List<String> getTokenEndpointAuthSigningAlgorithmValuesSupported() {
        return get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED);
    }

    /**
     * The {@code display} parameter values supported.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthRequest">
     * "OpenID Connect Core 1.0"</a>
     */
    @Nullable
    public List<String> getDisplayValuesSupported() {
        return get(DISPLAY_VALUES_SUPPORTED);
    }

    /**
     * The claim types supported. Defaults to {@code normal} if not specified by the discovery
     * document JSON, as suggested by the discovery specification.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClaimTypes">
     * "OpenID Connect Core 1.0", Section 5.6</a>
     */
    public List<String> getClaimTypesSupported() {
        return get(CLAIM_TYPES_SUPPORTED);
    }

    /**
     * The claim names of the claims that the provider <em>may</em> be able to supply values for.
     */
    @Nullable
    public List<String> getClaimsSupported() {
        return get(CLAIMS_SUPPORTED);
    }

    /**
     * A page containing human-readable information that developers might want or need to know when
     * using this provider.
     */
    @Nullable
    public Uri getServiceDocumentation() {
        return get(SERVICE_DOCUMENTATION);
    }

    /**
     * Languages and scripts supported for values in claims being returned.
     * Represented as a list of BCP47 language tag values.
     *
     * @see <a href="http://tools.ietf.org/html/rfc5646">"Tags for Identifying Languages"</a>
     */
    @Nullable
    public List<String> getClaimsLocalesSupported() {
        return get(CLAIMS_LOCALES_SUPPORTED);
    }

    /**
     * Languages and scripts supported for the user interface.
     * Represented as a list of BCP47 language tag values.
     *
     * @see <a href="http://tools.ietf.org/html/rfc5646">"Tags for Identifying Languages"</a>
     */
    @Nullable
    public List<String> getUiLocalesSupported() {
        return get(UI_LOCALES_SUPPORTED);
    }

    /**
     * Specifies whether the {@code claims} parameter is supported for authorization requests.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter">
     * "OpenID Connect Core 1.0", Section 5.5</a>
     */
    public boolean isClaimsParameterSupported() {
        return get(CLAIMS_PARAMETER_SUPPORTED);
    }

    /**
     * Specifies whether the {@code request} parameter is supported for authorization requests.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#RequestObject">
     * "OpenID Connect Core 1.0", Section 6.1</a>
     */
    public boolean isRequestParameterSupported() {
        return get(REQUEST_PARAMETER_SUPPORTED);
    }

    /**
     * Specifies whether the {@code request_uri} is supported for authorization requests.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#RequestUriParameter">
     * "OpenID Connect Core 1.0", Section 6.2</a>
     */
    public boolean isRequestUriParameterSupported() {
        return get(REQUEST_URI_PARAMETER_SUPPORTED);
    }

    /**
     * Specifies whether {@code request_uri} values are required to be pre-registered before use.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#RequestUriParameter">
     * "OpenID Connect Core 1.0", Section 6.2</a>
     */
    public boolean requireRequestUriRegistration() {
        return get(REQUIRE_REQUEST_URI_REGISTRATION);
    }

    /**
     * A page articulating the policy regarding the use of data provided by the provider.
     */
    @Nullable
    public Uri getOpPolicyUri() {
        return get(OP_POLICY_URI);
    }

    /**
     * A page articulating the terms of service for the provider.
     */
    @Nullable
    public Uri getOpTosUri() {
        return get(OP_TOS_URI);
    }

    /**
     * Shorthand method for creating a string metadata extractor.
     */
    private static StringField str(String key) {
        return new StringField(key);
    }

    /**
     * Shorthand method for creating a URI metadata extractor.
     */
    private static UriField uri(String key) {
        return new UriField(key);
    }

    /**
     * Shorthand method for creating a string list metadata extractor.
     */
    private static StringListField strList(String key) {
        return new StringListField(key);
    }

    /**
     * Shorthand method for creating a string list metadata extractor, with a default value.
     */
    private static StringListField strList(String key, List<String> defaults) {
        return new StringListField(key, defaults);
    }

    /**
     * Shorthand method for creating a boolean metadata extractor.
     */
    private static BooleanField bool(String key, boolean defaultValue) {
        return new BooleanField(key, defaultValue);
    }
}
