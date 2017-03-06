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

/**
 * The grant type values defined by the [OAuth2 spec](https://tools.ietf.org/html/rfc6749), and
 * used in {@link AuthorizationRequest authorization} and
 * {@link RegistrationRequest dynamic client registration} requests.
 */
public final class GrantTypeValues {
    /**
     * The grant type used for exchanging an authorization code for one or more tokens.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.3
     * <https://tools.ietf.org/html/rfc6749#section-4.1.3>"
     */
    public static final String AUTHORIZATION_CODE = "authorization_code";

    /**
     * The grant type used when obtaining an access token.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.2
     * <https://tools.ietf.org/html/rfc6749#section-4.2>"
     */
    public static final String IMPLICIT = "implicit";

    /**
     * The grant type used when exchanging a refresh token for a new token.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 6
     * <https://tools.ietf.org/html/rfc6749#section-6>"
     */
    public static final String REFRESH_TOKEN = "refresh_token";

    private GrantTypeValues() {
        throw new IllegalStateException("This type is not intended to be instantiated");
    }
}
