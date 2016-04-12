package net.openid.appauth;

/**
 * The grant type values defined by the <a href="https://tools.ietf.org/html/rfc6749">"The OAuth 2.0
 * Authorization Framework" (RFC 6749)</a>, and used in {@link AuthorizationRequest authorization}
 * and {@link RegistrationRequest dynamic client registration} requests.
 */
public class GrantTypeValues {
    /**
     * The grant type used for exchanging an authorization code for one or more tokens.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.3"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 4.1.3</a>
     */
    public static final String AUTHORIZATION_CODE = "authorization_code";

    /**
     * The grant type used when obtaining an access token.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.2"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 4.2</a>
     */
    public static final String IMPLICIT = "implicit";

    /**
     * The grant type used when exchanging a refresh token for a new token.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-6"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 6</a>
     */
    public static final String REFRESH_TOKEN = "refresh_token";

}
