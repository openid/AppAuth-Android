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

package net.openid.appauth.app2app;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;

import org.json.JSONArray;

import java.util.Set;

/** Class to hold all important information to perform a secure redirection. */
class RedirectSession {

    private Context mContext;
    private Uri mUri;
    private String mBasePackageName = "";
    private Set<String> mBaseCertFingerprints;
    private JSONArray mAssetLinksFile = null;

    protected RedirectSession(@NonNull Context context, @NonNull Uri uri) {
        this.mContext = context;
        this.mUri = uri;
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

    protected void setBaseCertFingerprints(Set<String> baseCertFingerprints) {
        this.mBaseCertFingerprints = baseCertFingerprints;
    }

    public JSONArray getAssetLinksFile() {
        return mAssetLinksFile;
    }

    public void setAssetLinksFile(JSONArray assetLinksFile) {
        this.mAssetLinksFile = assetLinksFile;
    }
}
