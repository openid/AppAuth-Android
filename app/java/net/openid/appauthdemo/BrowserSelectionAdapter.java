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

package net.openid.appauthdemo;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;

import net.openid.appauth.browser.BrowserDescriptor;
import net.openid.appauth.browser.BrowserSelector;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads the list of browsers on the device for selection in a list or spinner.
 */
public final class BrowserSelectionAdapter extends BaseAdapter {

    private static final int LOADER_ID = 101;

    private Context mContext;
    private ArrayList<BrowserInfo> mBrowsers;

    /**
     * Creates the adapter, using the loader manager from the specified activity.
     */
    public BrowserSelectionAdapter(@NonNull final Activity activity) {
        mContext = activity;
        initializeItemList();
        activity.getLoaderManager().initLoader(
                LOADER_ID,
                null,
                new BrowserLoaderCallbacks());
    }

    static final class BrowserInfo {
        final BrowserDescriptor mDescriptor;
        final CharSequence mLabel;
        final Drawable mIcon;

        BrowserInfo(BrowserDescriptor descriptor, CharSequence label, Drawable icon) {
            mDescriptor = descriptor;
            mLabel = label;
            mIcon = icon;
        }
    }

    @Override
    public int getCount() {
        return mBrowsers.size();
    }

    @Override
    public BrowserInfo getItem(int position) {
        return mBrowsers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.browser_selector_layout, parent, false);
        }

        BrowserInfo info = mBrowsers.get(position);

        TextView labelView = (TextView) convertView.findViewById(R.id.browser_label);
        ImageView iconView = (ImageView) convertView.findViewById(R.id.browser_icon);
        if (info == null) {
            labelView.setText(R.string.browser_appauth_default_label);
            iconView.setImageResource(R.drawable.appauth_96dp);
        } else {
            CharSequence label = info.mLabel;
            if (info.mDescriptor.useCustomTab) {
                label = String.format(mContext.getString(R.string.custom_tab_label), label);
            }
            labelView.setText(label);
            iconView.setImageDrawable(info.mIcon);
        }

        return convertView;
    }

    private void initializeItemList() {
        mBrowsers = new ArrayList<>();
        mBrowsers.add(null);
    }

    private final class BrowserLoaderCallbacks implements LoaderCallbacks<List<BrowserInfo>> {

        @Override
        public Loader<List<BrowserInfo>> onCreateLoader(int id, Bundle args) {
            return new BrowserLoader(mContext);
        }

        @Override
        public void onLoadFinished(Loader<List<BrowserInfo>> loader, List<BrowserInfo> data) {
            initializeItemList();
            mBrowsers.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<List<BrowserInfo>> loader) {
            initializeItemList();
            notifyDataSetChanged();
        }
    }

    private static class BrowserLoader extends AsyncTaskLoader<List<BrowserInfo>> {

        private List<BrowserInfo> mResult;

        BrowserLoader(Context context) {
            super(context);
        }

        @Override
        public List<BrowserInfo> loadInBackground() {
            List<BrowserDescriptor> descriptors = BrowserSelector.getAllBrowsers(getContext());
            ArrayList<BrowserInfo> infos = new ArrayList<>(descriptors.size());

            PackageManager pm = getContext().getPackageManager();
            for (BrowserDescriptor descriptor : descriptors) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(descriptor.packageName, 0);
                    CharSequence label = pm.getApplicationLabel(info);
                    Drawable icon = pm.getApplicationIcon(descriptor.packageName);
                    infos.add(new BrowserInfo(descriptor, label, icon));
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                    infos.add(new BrowserInfo(descriptor, descriptor.packageName, null));
                }
            }

            return infos;
        }

        @Override
        public void deliverResult(List<BrowserInfo> data) {
            if (isReset()) {
                mResult = null;
                return;
            }

            mResult = data;
            super.deliverResult(mResult);
        }

        @Override
        protected void onStartLoading() {
            if (mResult != null) {
                deliverResult(mResult);
            }
            forceLoad();
        }

        @Override
        protected void onReset() {
            mResult = null;
        }

        @Override
        public void onCanceled(List<BrowserInfo> data) {
            mResult = null;
            super.onCanceled(data);
        }
    }
}
