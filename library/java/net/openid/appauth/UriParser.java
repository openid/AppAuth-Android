package net.openid.appauth;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by thoeflicker on 5/5/16.
 */
public class UriParser {
    /**
     * The URI response parameter mode
     */
    private enum UriResponseMode {
        QUERY,
        FRAGMENT
    }

    private final Uri uri;
    private final UriResponseMode mode;
    private final String TAG = getClass().getName();
    private Map<String, String> fragmentParamMap;

    public UriParser(@NonNull Uri uri) {
        this.uri = uri;
        mode = uri.getFragment() != null ? UriResponseMode.FRAGMENT : UriResponseMode.QUERY;
        if (mode == UriResponseMode.FRAGMENT) {
            parseFragmentUri();
        }
    }

    /**
     * Returns a set of the unique names of all query parameters. Iterating
     * over the set will return the names in order of their first occurrence.
     *
     * @throws UnsupportedOperationException if this isn't a hierarchical URI
     *
     * @return a set of decoded names
     */
    public Set<String> getQueryParameterNames() {
        if (mode == UriResponseMode.QUERY) {
            return uri.getQueryParameterNames();
        } else {
            return fragmentParamMap.keySet();
        }
    }


    /**
     * Searches the query string for parameter values with the given key.
     *
     * @param key which will be encoded
     *
     * @throws UnsupportedOperationException if this isn't a hierarchical URI
     * @throws NullPointerException if key is null
     * @return a list of decoded values
     */
    public String getQueryParameter(String key) {
        if (mode == UriResponseMode.QUERY) {
            return uri.getQueryParameter(key);
        } else {
            key = Uri.encode(key);
            return fragmentParamMap.get(key);
        }
    }

    @VisibleForTesting
    void parseFragmentUri() {
        fragmentParamMap = new HashMap<>();
        String fragment = uri.getEncodedFragment();
        if (fragment == null) {
            return;
        }
        String[] keyValuePairs = fragment.split("&");
        for (String keyValue: keyValuePairs) {
            String[] raw = keyValue.split("=");
            if (raw.length != 2) {
                Log.d(TAG, "parseFragmentUri: Unqualified URI response argument encountered: ".concat(keyValue));
                continue;
            }
            fragmentParamMap.put(raw[0], raw[1]);
        }
    }
}
