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

import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArrayMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

/**
 * Returned as a response to OAuth2 requests if they fail. Specifically:
 *
 * - The {@link net.openid.appauth.AuthorizationService.TokenResponseCallback response} to
 * {@link AuthorizationService#performTokenRequest(net.openid.appauth.TokenRequest,
 * AuthorizationService.TokenResponseCallback) token requests},
 *
 * - The {@link net.openid.appauth.AuthorizationServiceConfiguration.RetrieveConfigurationCallback
 * response}
 * to
 * {@link AuthorizationServiceConfiguration#fetchFromUrl(android.net.Uri,
 * AuthorizationServiceConfiguration.RetrieveConfigurationCallback) configuration retrieval}.
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown", "ThrowableResultOfMethodCallIgnored"})
public final class AuthorizationException extends Exception {

    /**
     * The extra string that used to store an {@link AuthorizationException} in an intent by
     * {@link #toIntent()}.
     */
    public static final String EXTRA_EXCEPTION = "net.openid.appauth.AuthorizationException";

    /**
     * The OAuth2 parameter used to indicate the type of error during an authorization or
     * token request.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.2.1
     * <https://tools.ietf.org/html/rfc6749#section-4.1.2.1>"
     * @see "The OAuth 2.0 Authorization Framework" (RFC 6749), Section 5.2
     * <https://tools.ietf.org/html/rfc6749#section-5.2>"
     */
    public static final String PARAM_ERROR = "error";

    /**
     * The OAuth2 parameter used to provide a human readable description of the error which
     * occurred.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.2.1
     * <https://tools.ietf.org/html/rfc6749#section-4.1.2.1>"
     * @see "The OAuth 2.0 Authorization Framework" (RFC 6749), Section 5.2
     * <https://tools.ietf.org/html/rfc6749#section-5.2>"
     */
    public static final String PARAM_ERROR_DESCRIPTION = "error_description";

    /**
     * The OAuth2 parameter used to provide a URI to a human-readable page which describes the
     * error.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.2.1
     * <https://tools.ietf.org/html/rfc6749#section-4.1.2.1>"
     * @see "The OAuth 2.0 Authorization Framework" (RFC 6749), Section 5.2
     * <https://tools.ietf.org/html/rfc6749#section-5.2>"
     */
    public static final String PARAM_ERROR_URI = "error_uri";


    /**
     * The error type used for all errors that are not specific to OAuth related responses.
     */
    public static final int TYPE_GENERAL_ERROR = 0;

    /**
     * The error type for OAuth specific errors on the authorization endpoint. This error type is
     * used when the server responds to an authorization request with an explicit OAuth error, as
     * defined by [the OAuth2 specification, section 4.1.2.1](
     * https://tools.ietf.org/html/rfc6749#section-4.1.2.1). If the authorization response is
     * invalid and not explicitly an error response, another error type will be used.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.2.1
     * <https://tools.ietf.org/html/rfc6749#section-4.1.2.1>"
     */
    public static final int TYPE_OAUTH_AUTHORIZATION_ERROR = 1;

    /**
     * The error type for OAuth specific errors on the token endpoint. This error type is used when
     * the server responds with HTTP 400 and an OAuth error, as defined by
     * [the OAuth2 specification, section 5.2](https://tools.ietf.org/html/rfc6749#section-5.2).
     * If an HTTP 400 response does not parse as an OAuth error (i.e. no 'error' field is present
     * or the JSON is invalid), another error domain will be used.
     *
     * @see "The OAuth 2.0 Authorization Framework" (RFC 6749), Section 5.2
     * <https://tools.ietf.org/html/rfc6749#section-5.2>"
     */
    public static final int TYPE_OAUTH_TOKEN_ERROR = 2;

    /**
     * The error type for authorization errors encountered out of band on the resource server.
     */
    public static final int TYPE_RESOURCE_SERVER_AUTHORIZATION_ERROR = 3;

    /**
     * The error type for OAuth specific errors on the registration endpoint.
     */
    public static final int TYPE_OAUTH_REGISTRATION_ERROR = 4;

    /**
     * Error type when returning http error code >= 400
     */
    public static final int TYPE_HTTP_ERROR = 5;

    @VisibleForTesting
    static final String KEY_TYPE = "type";

    @VisibleForTesting
    static final String KEY_CODE = "code";

    @VisibleForTesting
    static final String KEY_ERROR = "error";

    @VisibleForTesting
    static final String KEY_ERROR_DESCRIPTION = "errorDescription";

    @VisibleForTesting
    static final String KEY_ERROR_URI = "errorUri";

    /**
     * Prime number multiplier used to produce a reasonable hash value distribution.
     */
    private static final int HASH_MULTIPLIER = 31;

    /**
     * Error codes specific to AppAuth for Android, rather than those defined in the OAuth2 and
     * OpenID specifications.
     */
    public static final class GeneralErrors {
        // codes in this group should be between 0-999

        /**
         * Indicates a problem parsing an OpenID Connect Service Discovery document.
         */
        public static final AuthorizationException INVALID_DISCOVERY_DOCUMENT =
                generalEx(0, "Invalid discovery document");

        /**
         * Indicates the user manually canceled the OAuth authorization code flow.
         */
        public static final AuthorizationException USER_CANCELED_AUTH_FLOW =
                generalEx(1, "User cancelled flow");

        /**
         * Indicates an OAuth authorization flow was programmatically cancelled.
         */
        public static final AuthorizationException PROGRAM_CANCELED_AUTH_FLOW =
                generalEx(2, "Flow cancelled programmatically");

        /**
         * Indicates a network error occurred.
         */
        public static final AuthorizationException NETWORK_ERROR =
                generalEx(3, "Network error");

        /**
         * Indicates a server error occurred.
         */
        public static final AuthorizationException SERVER_ERROR =
                generalEx(4, "Server error");

        /**
         * Indicates a problem occurred deserializing JSON.
         */
        public static final AuthorizationException JSON_DESERIALIZATION_ERROR =
                generalEx(5, "JSON deserialization error");

        /**
         * Indicates a problem occurred constructing a {@link TokenResponse token response} object
         * from the JSON provided by the server.
         */
        public static final AuthorizationException TOKEN_RESPONSE_CONSTRUCTION_ERROR =
                generalEx(6, "Token response construction error");

        /**
         * Indicates a problem parsing an OpenID Connect Registration Response.
         */
        public static final AuthorizationException INVALID_REGISTRATION_RESPONSE =
                generalEx(7, "Invalid registration response");

        /**
         * Indicates that a received ID token could not be parsed
         */
        public static final AuthorizationException ID_TOKEN_PARSING_ERROR =
                generalEx(8, "Unable to parse ID Token");

        /**
         * Indicates that a received ID token is invalid
         */
        public static final AuthorizationException ID_TOKEN_VALIDATION_ERROR =
                generalEx(9, "Invalid ID Token");
    }

    /**
     * Error codes related to failed authorization requests.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.2.1
     * <https://tools.ietf.org/html/rfc6749#section-4.1.2.1>"
     */
    public static final class AuthorizationRequestErrors {
        // codes in this group should be between 1000-1999

        /**
         * An `invalid_request` OAuth2 error response.
         */
        public static final AuthorizationException INVALID_REQUEST =
                authEx(1000, "invalid_request");

        /**
         * An `unauthorized_client` OAuth2 error response.
         */
        public static final AuthorizationException UNAUTHORIZED_CLIENT =
                authEx(1001, "unauthorized_client");

        /**
         * An `access_denied` OAuth2 error response.
         */
        public static final AuthorizationException ACCESS_DENIED =
                authEx(1002, "access_denied");

        /**
         * An `unsupported_response_type` OAuth2 error response.
         */
        public static final AuthorizationException UNSUPPORTED_RESPONSE_TYPE =
                authEx(1003, "unsupported_response_type");

        /**
         * An `invalid_scope` OAuth2 error response.
         */
        public static final AuthorizationException INVALID_SCOPE =
                authEx(1004, "invalid_scope");

        /**
         * An `server_error` OAuth2 error response, equivalent to an HTTP 500 error code, but
         * sent via redirect.
         */
        public static final AuthorizationException SERVER_ERROR =
                authEx(1005, "server_error");

        /**
         * A `temporarily_unavailable` OAuth2 error response, equivalent to an HTTP 503 error
         * code, but sent via redirect.
         */
        public static final AuthorizationException TEMPORARILY_UNAVAILABLE =
                authEx(1006, "temporarily_unavailable");

        /**
         * An authorization error occurring on the client rather than the server. For example,
         * due to client misconfiguration. This error should be treated as unrecoverable.
         */
        public static final AuthorizationException CLIENT_ERROR =
                authEx(1007, null);

        /**
         * Indicates an OAuth error as per RFC 6749, but the error code is not known to the
         * AppAuth for Android library. It could be a custom error or code, or one from an
         * OAuth extension. The {@link #error} field provides the exact error string returned by
         * the server.
         */
        public static final AuthorizationException OTHER =
                authEx(1008, null);

        /**
         * Indicates that the response state param did not match the request state param,
         * resulting in the response being discarded.
         */
        public static final AuthorizationException STATE_MISMATCH =
                generalEx(9, "Response state param did not match request state");

        private static final Map<String, AuthorizationException> STRING_TO_EXCEPTION =
                exceptionMapByString(
                        INVALID_REQUEST,
                        UNAUTHORIZED_CLIENT,
                        ACCESS_DENIED,
                        UNSUPPORTED_RESPONSE_TYPE,
                        INVALID_SCOPE,
                        SERVER_ERROR,
                        TEMPORARILY_UNAVAILABLE,
                        CLIENT_ERROR,
                        OTHER);

        /**
         * Returns the matching exception type for the provided OAuth2 error string, or
         * {@link #OTHER} if unknown.
         */
        @NonNull
        public static AuthorizationException byString(String error) {
            AuthorizationException ex = STRING_TO_EXCEPTION.get(error);
            if (ex != null) {
                return ex;
            }
            return OTHER;
        }
    }

    /**
     * Error codes related to failed token requests.
     *
     * @see "The OAuth 2.0 Authorization Framework" (RFC 6749), Section 5.2
     * <https://tools.ietf.org/html/rfc6749#section-5.2>"
     */
    public static final class TokenRequestErrors {
        // codes in this group should be between 2000-2999

        /**
         * An `invalid_request` OAuth2 error response.
         */
        public static final AuthorizationException INVALID_REQUEST =
                tokenEx(2000, "invalid_request");

        /**
         * An `invalid_client` OAuth2 error response.
         */
        public static final AuthorizationException INVALID_CLIENT =
                tokenEx(2001, "invalid_client");

        /**
         * An `invalid_grant` OAuth2 error response.
         */
        public static final AuthorizationException INVALID_GRANT =
                tokenEx(2002, "invalid_grant");

        /**
         * An `unauthorized_client` OAuth2 error response.
         */
        public static final AuthorizationException UNAUTHORIZED_CLIENT =
                tokenEx(2003, "unauthorized_client");

        /**
         * An `unsupported_grant_type` OAuth2 error response.
         */
        public static final AuthorizationException UNSUPPORTED_GRANT_TYPE =
                tokenEx(2004, "unsupported_grant_type");

        /**
         * An `invalid_scope` OAuth2 error response.
         */
        public static final AuthorizationException INVALID_SCOPE =
                tokenEx(2005, "invalid_scope");

        /**
         * An authorization error occurring on the client rather than the server. For example,
         * due to client misconfiguration. This error should be treated as unrecoverable.
         */
        public static final AuthorizationException CLIENT_ERROR =
                tokenEx(2006, null);

        /**
         * Indicates an OAuth error as per RFC 6749, but the error code is not known to the
         * AppAuth for Android library. It could be a custom error or code, or one from an
         * OAuth extension. The {@link #error} field provides the exact error string returned by
         * the server.
         */
        public static final AuthorizationException OTHER =
                tokenEx(2007, null);

        private static final Map<String, AuthorizationException> STRING_TO_EXCEPTION =
                exceptionMapByString(
                        INVALID_REQUEST,
                        INVALID_CLIENT,
                        INVALID_GRANT,
                        UNAUTHORIZED_CLIENT,
                        UNSUPPORTED_GRANT_TYPE,
                        INVALID_SCOPE,
                        CLIENT_ERROR,
                        OTHER);

        /**
         * Returns the matching exception type for the provided OAuth2 error string, or
         * {@link #OTHER} if unknown.
         */
        public static AuthorizationException byString(String error) {
            AuthorizationException ex = STRING_TO_EXCEPTION.get(error);
            if (ex != null) {
                return ex;
            }
            return OTHER;
        }
    }

    /**
     * Error codes related to failed registration requests.
     */
    public static final class RegistrationRequestErrors {
        // codes in this group should be between 4000-4999

        /**
         * An `invalid_request` OAuth2 error response.
         */
        public static final AuthorizationException INVALID_REQUEST =
                registrationEx(4000, "invalid_request");

        /**
         * An `invalid_client` OAuth2 error response.
         */
        public static final AuthorizationException INVALID_REDIRECT_URI =
                registrationEx(4001, "invalid_redirect_uri");

        /**
         * An `invalid_grant` OAuth2 error response.
         */
        public static final AuthorizationException INVALID_CLIENT_METADATA =
                registrationEx(4002, "invalid_client_metadata");

        /**
         * An authorization error occurring on the client rather than the server. For example,
         * due to client misconfiguration. This error should be treated as unrecoverable.
         */
        public static final AuthorizationException CLIENT_ERROR =
                registrationEx(4003, null);

        /**
         * Indicates an OAuth error as per RFC 6749, but the error code is not known to the
         * AppAuth for Android library. It could be a custom error or code, or one from an
         * OAuth extension. The {@link #error} field provides the exact error string returned by
         * the server.
         */
        public static final AuthorizationException OTHER =
                registrationEx(4004, null);

        private static final Map<String, AuthorizationException> STRING_TO_EXCEPTION =
                exceptionMapByString(
                        INVALID_REQUEST,
                        INVALID_REDIRECT_URI,
                        INVALID_CLIENT_METADATA,
                        CLIENT_ERROR,
                        OTHER);

        /**
         * Returns the matching exception type for the provided OAuth2 error string, or
         * {@link #OTHER} if unknown.
         */
        public static AuthorizationException byString(String error) {
            AuthorizationException ex = STRING_TO_EXCEPTION.get(error);
            if (ex != null) {
                return ex;
            }
            return OTHER;
        }
    }

    private static AuthorizationException generalEx(int code, @Nullable String errorDescription) {
        return new AuthorizationException(
                TYPE_GENERAL_ERROR, code, null, errorDescription, null, null);
    }

    private static AuthorizationException authEx(int code, @Nullable String error) {
        return new AuthorizationException(
                TYPE_OAUTH_AUTHORIZATION_ERROR, code, error, null, null, null);
    }

    private static AuthorizationException tokenEx(int code, @Nullable String error) {
        return new AuthorizationException(
                TYPE_OAUTH_TOKEN_ERROR, code, error, null, null, null);
    }

    private static AuthorizationException registrationEx(int code, @Nullable String error) {
        return new AuthorizationException(
                TYPE_OAUTH_REGISTRATION_ERROR, code, error, null, null, null);
    }

    /**
     * Creates an exception based on one of the existing values defined in
     * {@link GeneralErrors}, {@link AuthorizationRequestErrors} or {@link TokenRequestErrors},
     * providing a root cause.
     */
    public static AuthorizationException fromTemplate(
            @NonNull AuthorizationException ex,
            @Nullable Throwable rootCause) {
        return new AuthorizationException(
                ex.type,
                ex.code,
                ex.error,
                ex.errorDescription,
                ex.errorUri,
                rootCause);
    }

    /**
     * Creates an exception based on HTTP error codes returned from HttpUrlConnection
     * Exception includes actual error code from HttpURLConnection.getResponseStream()
     * Errors are considered anything >= 400
     */
    public static AuthorizationException fromHttpError(
            int responseCode,
            @NonNull String responseMessage,
            @NonNull String errorString) {
        return new AuthorizationException(
            TYPE_HTTP_ERROR,
            responseCode,
            responseMessage,
            errorString,
            null,
            null);
    }

    /**
     * Creates an exception based on one of the existing values defined in
     * {@link AuthorizationRequestErrors} or {@link TokenRequestErrors}, adding information
     * retrieved from OAuth error response.
     */
    public static AuthorizationException fromOAuthTemplate(
            @NonNull AuthorizationException ex,
            @Nullable String errorOverride,
            @Nullable String errorDescriptionOverride,
            @Nullable Uri errorUriOverride) {
        return new AuthorizationException(
                ex.type,
                ex.code,
                (errorOverride != null) ? errorOverride : ex.error,
                (errorDescriptionOverride != null) ? errorDescriptionOverride : ex.errorDescription,
                (errorUriOverride != null) ? errorUriOverride : ex.errorUri,
                null);
    }

    /**
     * Creates an exception from an OAuth redirect URI that describes an authorization failure.
     */
    public static AuthorizationException fromOAuthRedirect(
            @NonNull Uri redirectUri) {
        String error = redirectUri.getQueryParameter(PARAM_ERROR);
        String errorDescription = redirectUri.getQueryParameter(PARAM_ERROR_DESCRIPTION);
        String errorUri = redirectUri.getQueryParameter(PARAM_ERROR_URI);
        AuthorizationException base = AuthorizationRequestErrors.byString(error);
        return new AuthorizationException(
                base.type,
                base.code,
                error,
                errorDescription != null ? errorDescription : base.errorDescription,
                errorUri != null ? Uri.parse(errorUri) : base.errorUri,
                null);
    }

    /**
     * Reconstructs an {@link AuthorizationException} from the JSON produced by
     * {@link #toJsonString()}.
     * @throws JSONException if the JSON is malformed or missing required properties
     */
    public static AuthorizationException fromJson(@NonNull String jsonStr) throws JSONException {
        checkNotEmpty(jsonStr, "jsonStr cannot be null or empty");
        return fromJson(new JSONObject(jsonStr));
    }

    /**
     * Reconstructs an {@link AuthorizationException} from the JSON produced by
     * {@link #toJson()}.
     * @throws JSONException if the JSON is malformed or missing required properties
     */
    public static AuthorizationException fromJson(@NonNull JSONObject json) throws JSONException {
        checkNotNull(json, "json cannot be null");
        return new AuthorizationException(
                json.getInt(KEY_TYPE),
                json.getInt(KEY_CODE),
                JsonUtil.getStringIfDefined(json, KEY_ERROR),
                JsonUtil.getStringIfDefined(json, KEY_ERROR_DESCRIPTION),
                JsonUtil.getUriIfDefined(json, KEY_ERROR_URI),
                null);
    }

    /**
     * Extracts an {@link AuthorizationException} from an intent produced by {@link #toIntent()}.
     * This is used to retrieve an error response in the handler registered for a call to
     * {@link AuthorizationService#performAuthorizationRequest}.
     */
    @Nullable
    public static AuthorizationException fromIntent(Intent data) {
        checkNotNull(data);

        if (!data.hasExtra(EXTRA_EXCEPTION)) {
            return null;
        }

        try {
            return fromJson(data.getStringExtra(EXTRA_EXCEPTION));
        } catch (JSONException ex) {
            throw new IllegalArgumentException("Intent contains malformed exception data", ex);
        }
    }

    private static Map<String, AuthorizationException> exceptionMapByString(
            AuthorizationException... exceptions) {
        ArrayMap<String, AuthorizationException> map =
                new ArrayMap<>(exceptions != null ? exceptions.length : 0);

        if (exceptions != null) {
            for (AuthorizationException ex : exceptions) {
                if (ex.error != null) {
                    map.put(ex.error, ex);
                }
            }
        }

        return Collections.unmodifiableMap(map);
    }

    /**
     * The type of the error.
     * @see #TYPE_GENERAL_ERROR
     * @see #TYPE_OAUTH_AUTHORIZATION_ERROR
     * @see #TYPE_OAUTH_TOKEN_ERROR
     * @see #TYPE_RESOURCE_SERVER_AUTHORIZATION_ERROR
     */
    public final int type;

    /**
     * The error code describing the class of problem encountered from the set defined in this
     * class.
     */
    public final int code;

    /**
     * The error string as it is found in the OAuth2 protocol.
     */
    @Nullable
    public final String error;

    /**
     * The human readable error message associated with this exception, if available.
     */
    @Nullable
    public final String errorDescription;

    /**
     * A URI identifying a human-readable web page with information about this error.
     */
    @Nullable
    public final Uri errorUri;

    /**
     * Instantiates an authorization request with optional root cause information.
     */
    public AuthorizationException(
            int type,
            int code,
            @Nullable String error,
            @Nullable String errorDescription,
            @Nullable Uri errorUri,
            @Nullable Throwable rootCause) {
        super(errorDescription, rootCause);
        this.type = type;
        this.code = code;
        this.error = error;
        this.errorDescription = errorDescription;
        this.errorUri = errorUri;
    }

    /**
     * Produces a JSON representation of the authorization exception, for transmission or storage.
     * This does not include any provided root cause.
     */
    @NonNull
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, KEY_TYPE, type);
        JsonUtil.put(json, KEY_CODE, code);
        JsonUtil.putIfNotNull(json, KEY_ERROR, error);
        JsonUtil.putIfNotNull(json, KEY_ERROR_DESCRIPTION, errorDescription);
        JsonUtil.putIfNotNull(json, KEY_ERROR_URI, errorUri);
        return json;
    }

    /**
     * Provides a JSON string representation of an authorization exception, for transmission or
     * storage. This does not include any provided root cause.
     */
    @NonNull
    public String toJsonString() {
        return toJson().toString();
    }

    /**
     * Creates an intent from this exception. Used to carry error responses to the handling activity
     * specified in calls to {@link AuthorizationService#performAuthorizationRequest}.
     */
    @NonNull
    public Intent toIntent() {
        Intent data = new Intent();
        data.putExtra(EXTRA_EXCEPTION, toJsonString());
        return data;
    }

    /**
     * Exceptions are considered to be equal if their {@link #type type} and {@link #code code}
     * are the same; all other properties are irrelevant for comparison.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || !(obj instanceof AuthorizationException)) {
            return false;
        }

        AuthorizationException other = (AuthorizationException) obj;
        return this.type == other.type && this.code == other.code;
    }

    @Override
    public int hashCode() {
        // equivalent to Arrays.hashCode(new int[] { type, code });
        return (HASH_MULTIPLIER * (HASH_MULTIPLIER + type)) + code;
    }

    @Override
    public String toString() {
        return "AuthorizationException: " + toJsonString();
    }
}
