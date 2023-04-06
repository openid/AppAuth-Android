/*
 * Copyright 2018 The AppAuth for Android Authors. All Rights Reserved.
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

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import net.openid.appauth.AuthorizationException.GeneralErrors;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An OpenID Connect ID Token. Contains claims about the authentication of an End-User by an
 * Authorization Server. Supports parsing ID Tokens from JWT Compact Serializations and validation
 * according to the OpenID Connect specification.
 *
 * @see "OpenID Connect Core ID Token, Section 2
 * <http://openid.net/specs/openid-connect-core-1_0.html#IDToken>"
 * @see "OpenID Connect Core ID Token Validation, Section 3.1.3.7
 * <http://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation>"
 */
public class IdToken {

    private static final String KEY_ISSUER = "iss";
    private static final String KEY_SUBJECT = "sub";
    private static final String KEY_AUDIENCE = "aud";
    private static final String KEY_EXPIRATION = "exp";
    private static final String KEY_ISSUED_AT = "iat";
    private static final String KEY_NONCE = "nonce";
    private static final String KEY_AUTHORIZED_PARTY = "azp";
    private static final Long MILLIS_PER_SECOND = 1000L;
    private static final Long TEN_MINUTES_IN_SECONDS = 600L;

    private static final Set<String> BUILT_IN_CLAIMS = builtInParams(
            KEY_ISSUER,
            KEY_SUBJECT,
            KEY_AUDIENCE,
            KEY_EXPIRATION,
            KEY_ISSUED_AT,
            KEY_NONCE,
            KEY_AUTHORIZED_PARTY);

    /**
     * Issuer Identifier for the Issuer of the response.
     */
    @NonNull
    public final String issuer;

    /**
     * Subject Identifier. A locally unique and never reassigned identifier within the Issuer
     * for the End-User.
     */
    @NonNull
    public final String subject;

    /**
     * Audience(s) that this ID Token is intended for.
     */
    @NonNull
    public final List<String> audience;

    /**
     * Expiration time on or after which the ID Token MUST NOT be accepted for processing.
     */
    @NonNull
    public final Long expiration;

    /**
     * Time at which the JWT was issued.
     */
    @NonNull
    public final Long issuedAt;

    /**
     * String value used to associate a Client session with an ID Token,
     * and to mitigate replay attacks.
     */
    @Nullable
    public final String nonce;

    /**
     * Authorized party - the party to which the ID Token was issued.
     * If present, it MUST contain the OAuth 2.0 Client ID of this party.
     */
    @Nullable
    public final String authorizedParty;

    /**
     * Additional claims present in this ID Token.
     */
    @NonNull
    public final Map<String, Object> additionalClaims;

    @VisibleForTesting
    IdToken(@NonNull String issuer,
            @NonNull String subject,
            @NonNull List<String> audience,
            @NonNull Long expiration,
            @NonNull Long issuedAt) {
        this(issuer, subject, audience, expiration, issuedAt, null, null, Collections.emptyMap());
    }

    @VisibleForTesting
    IdToken(@NonNull String issuer,
            @NonNull String subject,
            @NonNull List<String> audience,
            @NonNull Long expiration,
            @NonNull Long issuedAt,
            @Nullable String nonce,
            @Nullable String authorizedParty) {
        this(issuer, subject, audience, expiration, issuedAt,
                nonce, authorizedParty, Collections.emptyMap());
    }

    IdToken(@NonNull String issuer,
            @NonNull String subject,
            @NonNull List<String> audience,
            @NonNull Long expiration,
            @NonNull Long issuedAt,
            @Nullable String nonce,
            @Nullable String authorizedParty,
            @NonNull Map<String, Object> additionalClaims) {
        this.issuer = issuer;
        this.subject = subject;
        this.audience = audience;
        this.expiration = expiration;
        this.issuedAt = issuedAt;
        this.nonce = nonce;
        this.authorizedParty = authorizedParty;
        this.additionalClaims = additionalClaims;
    }

    private static JSONObject parseJwtSection(String section) throws JSONException {
        byte[] decodedSection = Base64.decode(section, Base64.URL_SAFE);
        String jsonString = new String(decodedSection);
        return new JSONObject(jsonString);
    }

    static IdToken from(String token) throws JSONException, IdTokenException {
        String[] sections = token.split("\\.");

        if (sections.length <= 1) {
            throw new IdTokenException("ID token must have both header and claims section");
        }

        // We ignore header contents, but parse it to check that it is structurally valid JSON
        parseJwtSection(sections[0]);
        JSONObject claims = parseJwtSection(sections[1]);

        final String issuer = JsonUtil.getString(claims, KEY_ISSUER);
        final String subject = JsonUtil.getString(claims, KEY_SUBJECT);
        List<String> audience;
        try {
            audience = JsonUtil.getStringList(claims, KEY_AUDIENCE);
        } catch (JSONException jsonEx) {
            audience = new ArrayList<>();
            audience.add(JsonUtil.getString(claims, KEY_AUDIENCE));
        }
        final Long expiration = claims.getLong(KEY_EXPIRATION);
        final Long issuedAt = claims.getLong(KEY_ISSUED_AT);
        final String nonce = JsonUtil.getStringIfDefined(claims, KEY_NONCE);
        final String authorizedParty = JsonUtil.getStringIfDefined(claims, KEY_AUTHORIZED_PARTY);

        for (String key: BUILT_IN_CLAIMS) {
            claims.remove(key);
        }
        Map<String, Object> additionalClaims = JsonUtil.toMap(claims);

        return new IdToken(
            issuer,
            subject,
            audience,
            expiration,
            issuedAt,
            nonce,
            authorizedParty,
            additionalClaims
        );
    }

    @VisibleForTesting
    void validate(@NonNull TokenRequest tokenRequest, Clock clock) throws AuthorizationException {
        validate(tokenRequest, clock, false);
    }

    void validate(@NonNull TokenRequest tokenRequest,
                  Clock clock,
                  boolean skipIssuerHttpsCheck) throws AuthorizationException {
        // OpenID Connect Core Section 3.1.3.7. rule #1
        // Not enforced: AppAuth does not support JWT encryption.

        // OpenID Connect Core Section 3.1.3.7. rule #2
        // Validates that the issuer in the ID Token matches that of the discovery document.
        AuthorizationServiceDiscovery discoveryDoc = tokenRequest.configuration.discoveryDoc;
        if (discoveryDoc != null) {
            String expectedIssuer = discoveryDoc.getIssuer();
            if (!this.issuer.equals(expectedIssuer)) {
                throw AuthorizationException.fromTemplate(GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                    new IdTokenException("Issuer mismatch"));
            }

            // OpenID Connect Core Section 2.
            // The iss value is a case sensitive URL using the https scheme that contains scheme,
            // host, and optionally, port number and path components and no query or fragment
            // components.
            Uri issuerUri = Uri.parse(this.issuer);

            if (!skipIssuerHttpsCheck && !issuerUri.getScheme().equals("https")) {
                throw AuthorizationException.fromTemplate(GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                    new IdTokenException("Issuer must be an https URL"));
            }

            if (TextUtils.isEmpty(issuerUri.getHost())) {
                throw AuthorizationException.fromTemplate(GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                    new IdTokenException("Issuer host can not be empty"));
            }

            if (issuerUri.getFragment() != null || issuerUri.getQueryParameterNames().size() > 0) {
                throw AuthorizationException.fromTemplate(GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                    new IdTokenException(
                        "Issuer URL should not containt query parameters or fragment components"));
            }
        }


        // OpenID Connect Core Section 3.1.3.7. rule #3 & Section 2 azp Claim
        // Validates that the aud (audience) Claim contains the client ID, or that the azp
        // (authorized party) Claim matches the client ID.
        String clientId = tokenRequest.clientId;
        if (!this.audience.contains(clientId) && !clientId.equals(this.authorizedParty)) {
            throw AuthorizationException.fromTemplate(GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                new IdTokenException("Audience mismatch"));
        }

        // OpenID Connect Core Section 3.1.3.7. rules #4 & #5
        // Not enforced.

        // OpenID Connect Core Section 3.1.3.7. rule #6
        // As noted above, AppAuth only supports the code flow which results in direct
        // communication of the ID Token from the Token Endpoint to the Client, and we are
        // exercising the option to use TLS server validation instead of checking the token
        // signature. Users may additionally check the token signature should they wish.

        // OpenID Connect Core Section 3.1.3.7. rules #7 & #8
        // Not enforced. See rule #6.

        // OpenID Connect Core Section 3.1.3.7. rule #9
        // Validates that the current time is before the expiry time.
        Long nowInSeconds = clock.getCurrentTimeMillis() / MILLIS_PER_SECOND;
        if (nowInSeconds > this.expiration) {
            throw AuthorizationException.fromTemplate(GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                new IdTokenException("ID Token expired"));
        }

        // OpenID Connect Core Section 3.1.3.7. rule #10
        // Validates that the issued at time is not more than +/- 10 minutes on the current
        // time.
        if (Math.abs(nowInSeconds - this.issuedAt) > TEN_MINUTES_IN_SECONDS) {
            throw AuthorizationException.fromTemplate(GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                new IdTokenException("Issued at time is more than 10 minutes "
                    + "before or after the current time"));
        }

        // Only relevant for the authorization_code response type
        if (GrantTypeValues.AUTHORIZATION_CODE.equals(tokenRequest.grantType)) {
            // OpenID Connect Core Section 3.1.3.7. rule #11
            // Validates the nonce.
            String expectedNonce = tokenRequest.nonce;
            if (!TextUtils.equals(this.nonce, expectedNonce)) {
                throw AuthorizationException.fromTemplate(GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                    new IdTokenException("Nonce mismatch"));
            }
        }
        // OpenID Connect Core Section 3.1.3.7. rules #12
        // ACR is not directly supported by AppAuth.

        // OpenID Connect Core Section 3.1.3.7. rules #13
        // max_age is not directly supported by AppAuth.
    }

    static class IdTokenException extends Exception {
        IdTokenException(String message) {
            super(message);
        }
    }
}
