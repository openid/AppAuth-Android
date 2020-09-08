package net.openid.appauth.app2app;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Set;
import org.json.JSONArray;

/**
 * Class to hold all important information to perform a secure redirection.
 */
class RedirectSession {

    private Context mContext;
    private Uri mUri;
    private String mBasePackageName = "";
    private Set<String> mBaseCertFingerprints;
    private JSONArray mAssetLinksFile = null;

    protected RedirectSession(@NonNull Context mContext, @NonNull Uri mUri) {
        this.mContext = mContext;
        this.mUri = mUri;
    }

    @NonNull
    protected Context getContext() {
        return mContext;
    }

    protected void setContext(@NonNull Context context) {
        this.mContext = context;
    }

    @NonNull
    protected Uri getUri() {
        return mUri;
    }

    protected void setUri(@NonNull Uri uri) {
        this.mUri = uri;
    }

    @NonNull
    protected String getBasePackageName() {
        return mBasePackageName;
    }

    protected void setBasePackageName(@NonNull String basePackageName) {
        this.mBasePackageName = basePackageName;
    }

    protected Set<String> getBaseCertFingerprints() {
        return mBaseCertFingerprints;
    }

    protected void setBaseCertFingerprints(Set<String> mBaseCertFingerprints) {
        this.mBaseCertFingerprints = mBaseCertFingerprints;
    }

    public JSONArray getAssetLinksFile() {
        return mAssetLinksFile;
    }

    public void setAssetLinksFile(JSONArray mAssetLinksFile) {
        this.mAssetLinksFile = mAssetLinksFile;
    }
}
