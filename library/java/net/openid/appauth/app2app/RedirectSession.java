package net.openid.appauth.app2app;

import android.content.Context;
import android.net.Uri;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Class to hold all important information to perform a secure redirection.
 */
class RedirectSession {

    private Context mContext;
    private Uri mUri;
    private String mBasePackageName = "";
    private Set<String> mBaseCertFingerprints;

    protected RedirectSession(@NotNull Context mContext, @NotNull Uri mUri) {
        this.mContext = mContext;
        this.mUri = mUri;
    }

    @NotNull
    protected Context getContext() {
        return mContext;
    }

    protected void setContext(@NotNull Context context) {
        this.mContext = context;
    }

    @NotNull
    protected Uri getUri() {
        return mUri;
    }

    protected void setUri(@NotNull Uri uri) {
        this.mUri = uri;
    }

    @NotNull
    protected String getBasePackageName() {
        return mBasePackageName;
    }

    protected void setBasePackageName(@NotNull String basePackageName) {
        this.mBasePackageName = basePackageName;
    }

    protected Set<String> getBaseCertFingerprints() {
        return mBaseCertFingerprints;
    }

    protected void setBaseCertFingerprints(Set<String> mBaseCertFingerprints) {
        this.mBaseCertFingerprints = mBaseCertFingerprints;
    }
}
