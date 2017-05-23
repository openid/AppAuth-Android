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

import android.util.Base64;

import net.openid.appauth.internal.Logger;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.regex.Pattern;


/**
 * Generates code verifiers and challenges for PKCE exchange.
 *
 * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636)
 * <https://tools.ietf.org/html/rfc7636>"
 */
public final class CodeVerifierUtil {

    /**
     * The minimum permitted length for a code verifier.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4.1
     * <https://tools.ietf.org/html/rfc7636#section-4.1>"
     */
    public static final int MIN_CODE_VERIFIER_LENGTH = 43;

    /**
     * The maximum permitted length for a code verifier.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4.1
     * <https://tools.ietf.org/html/rfc7636#section-4.1>"
     */
    public static final int MAX_CODE_VERIFIER_LENGTH = 128;

    /**
     * The default entropy (in bytes) used for the code verifier.
     */
    public static final int DEFAULT_CODE_VERIFIER_ENTROPY = 64;

    /**
     * The minimum permitted entropy (in bytes) for use with
     * {@link #generateRandomCodeVerifier(SecureRandom,int)}.
     */
    public static final int MIN_CODE_VERIFIER_ENTROPY = 32;

    /**
     * The maximum permitted entropy (in bytes) for use with
     * {@link #generateRandomCodeVerifier(SecureRandom,int)}.
     */
    public static final int MAX_CODE_VERIFIER_ENTROPY = 96;

    /**
     * Base64 encoding settings used for generated code verifiers.
     */
    private static final int PKCE_BASE64_ENCODE_SETTINGS =
            Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE;

    /**
     * Regex for legal code verifier strings, as defined in the spec.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4.1
     * <https://tools.ietf.org/html/rfc7636#section-4.1>"
     */
    private static final Pattern REGEX_CODE_VERIFIER =
            Pattern.compile("^[0-9a-zA-Z\\-\\.\\_\\~]{43,128}$");


    private CodeVerifierUtil() {
        throw new IllegalStateException("This type is not intended to be instantiated");
    }

    /**
     * Throws an IllegalArgumentException if the provided code verifier is invalid.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients (RFC 7636), Section 4.1
     * <https://tools.ietf.org/html/rfc7636#section-4.1>"
     */
    public static void checkCodeVerifier(String codeVerifier) {
        checkArgument(MIN_CODE_VERIFIER_LENGTH <= codeVerifier.length(),
                "codeVerifier length is shorter than allowed by the PKCE specification");
        checkArgument(codeVerifier.length() <= MAX_CODE_VERIFIER_LENGTH,
                "codeVerifier length is longer than allowed by the PKCE specification");
        checkArgument(REGEX_CODE_VERIFIER.matcher(codeVerifier).matches(),
                "codeVerifier string contains illegal characters");
    }

    /**
     * Generates a random code verifier string using {@link SecureRandom} as the source of
     * entropy, with the default entropy quantity as defined by
     * {@link #DEFAULT_CODE_VERIFIER_ENTROPY}.
     */
    public static String generateRandomCodeVerifier() {
        return generateRandomCodeVerifier(new SecureRandom(), DEFAULT_CODE_VERIFIER_ENTROPY);
    }

    /**
     * Generates a random code verifier string using the provided entropy source and the specified
     * number of bytes of entropy.
     */
    public static String generateRandomCodeVerifier(SecureRandom entropySource, int entropyBytes) {
        checkNotNull(entropySource, "entropySource cannot be null");
        checkArgument(MIN_CODE_VERIFIER_ENTROPY <= entropyBytes,
                "entropyBytes is less than the minimum permitted");
        checkArgument(entropyBytes <= MAX_CODE_VERIFIER_ENTROPY,
                "entropyBytes is greater than the maximum permitted");
        byte[] randomBytes = new byte[entropyBytes];
        entropySource.nextBytes(randomBytes);
        return Base64.encodeToString(randomBytes, PKCE_BASE64_ENCODE_SETTINGS);
    }

    /**
     * Produces a challenge from a code verifier, using SHA-256 as the challenge method if the
     * system supports it (all Android devices _should_ support SHA-256), and falls back
     * to the {@link AuthorizationRequest#CODE_CHALLENGE_METHOD_PLAIN "plain" challenge type} if
     * unavailable.
     */
    public static String deriveCodeVerifierChallenge(String codeVerifier) {
        try {
            MessageDigest sha256Digester = MessageDigest.getInstance("SHA-256");
            sha256Digester.update(codeVerifier.getBytes("ISO_8859_1"));
            byte[] digestBytes = sha256Digester.digest();
            return Base64.encodeToString(digestBytes, PKCE_BASE64_ENCODE_SETTINGS);
        } catch (NoSuchAlgorithmException e) {
            Logger.warn("SHA-256 is not supported on this device! Using plain challenge", e);
            return codeVerifier;
        } catch (UnsupportedEncodingException e) {
            Logger.error("ISO-8859-1 encoding not supported on this device!", e);
            throw new IllegalStateException("ISO-8859-1 encoding not supported", e);
        }
    }

    /**
     * Returns the challenge method utilized on this system: typically
     * {@link AuthorizationRequest#CODE_CHALLENGE_METHOD_S256 SHA-256} if supported by
     * the system, {@link AuthorizationRequest#CODE_CHALLENGE_METHOD_PLAIN plain} otherwise.
     */
    public static String getCodeVerifierChallengeMethod() {
        try {
            MessageDigest.getInstance("SHA-256");
            // no exception, so SHA-256 is supported
            return AuthorizationRequest.CODE_CHALLENGE_METHOD_S256;
        } catch (NoSuchAlgorithmException e) {
            return AuthorizationRequest.CODE_CHALLENGE_METHOD_PLAIN;
        }
    }
}
