package net.openid.appauth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class IDToken {

    private static final String KEY_ISSUER = "iss";
    private static final String KEY_SUBJECT = "sub";
    private static final String KEY_AUDIENCE = "aud";
    private static final String KEY_EXPIRATION = "exp";
    private static final String KEY_ISSUED_AT = "iat";
    private static final String KEY_NONCE = "nonce";

    final String issuer;
    final String subject;
    final List<String> audience;
    final Long expiration;
    final Long issuedAt;
    final String nonce;

    IDToken(
        @NonNull String issuer,
        @NonNull String subject,
        @NonNull List<String> audience,
        @NonNull Long expiration,
        @NonNull Long issuedAt,
        @Nullable String nonce) {
        this.issuer = issuer;
        this.subject = subject;
        this.audience = audience;
        this.expiration = expiration;
        this.issuedAt = issuedAt;
        this.nonce = nonce;
    }

    static JSONObject parseJWTSection(String section) throws JSONException {
        byte[] decodedSection = Base64.decode(section,Base64.URL_SAFE);
        String jsonString = new String(decodedSection);
        return new JSONObject(jsonString);
    }

    static IDToken from(String token) throws JSONException, IDTokenException {
        String[] sections = token.split("\\.");

        if (sections.length <= 1) {
            throw new IDTokenException("ID token must have both header and claims section");
        }

        parseJWTSection(sections[0]);
        JSONObject claims = parseJWTSection(sections[1]);

        String issuer = JsonUtil.getString(claims, KEY_ISSUER);
        String subject = JsonUtil.getString(claims, KEY_SUBJECT);
        List<String> audience;
        try {
            audience = JsonUtil.getStringList(claims, KEY_AUDIENCE);
        } catch (JSONException jsonEx) {
            audience = new ArrayList<>();
            audience.add(JsonUtil.getString(claims, KEY_AUDIENCE));
        }
        Long expiration = claims.getLong(KEY_EXPIRATION);
        Long issuedAt = claims.getLong(KEY_ISSUED_AT);
        String nonce = JsonUtil.getStringIfDefined(claims, KEY_NONCE);

        return new IDToken(
            issuer,
            subject,
            audience,
            expiration,
            issuedAt,
            nonce
        );
    }

    static class IDTokenException extends Exception {
        IDTokenException(String message) {
            super(message);
        }
    }
}
