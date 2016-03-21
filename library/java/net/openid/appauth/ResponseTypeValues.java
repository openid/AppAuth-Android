package net.openid.appauth;

public class ResponseTypeValues {
    /**
     * For requesting an authorization code.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1.1"> "The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 3.1.1</a>
     */
    public static final String CODE = "code";

    /**
     * For requesting an access token via an implicit grant.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1.1"> "The OAuth 2.0
     * Authorization Framework" (RFC 6749), Section 3.1.1</a>
     */
    public static final String TOKEN = "token";

    /**
     * For requesting an OpenID Conenct ID Token.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#IDToken">
     *     "OpenID Connect Core 1.0", Section 2</a>
     */
    public static final String ID_TOKEN = "id_token";

}
