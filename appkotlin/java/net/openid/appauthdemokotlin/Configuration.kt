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

package net.openid.appauthdemokotlin

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.Uri
import android.text.TextUtils
import okio.Buffer
import okio.Okio
import net.openid.appauth.connectivity.ConnectionBuilder
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.charset.Charset

/**
 * Reads and validates the demo app configuration from `res/raw/auth_config.json`. Configuration
 * changes are detected by comparing the hash of the last known configuration to the read
 * configuration. When a configuration change is detected, the app state is reset.
 */
class Configuration(private val mContext: Context) {
    private val preferences: SharedPreferences = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val resources: Resources = mContext.resources
    private var configJson: JSONObject? = null
    private var configHash: String? = null

    /**
     * Returns a description of the configuration error, if the configuration is invalid.
     */
    var configurationError: String? = null
    var clientId: String? = null
    var scope: String? = null
    var redirectUri: Uri? = null
    var endSessionUri: Uri? = null
    var discoveryUri: Uri? = null
    var authEndpointUri: Uri? = null
    var tokenEndpointUri: Uri? = null
    var endSessionEndpoint: Uri? = null
    var registrationEndpointUri: Uri? = null
    var userInfoEndpointUri: Uri? = null
    private var isHttpsRequired = false

    /**
     * Indicates whether the configuration has changed from the last known valid state.
     */
    fun hasConfigurationChanged(): Boolean {
        val lastHash = lastKnownConfigHash
        return configHash != lastHash
    }

    /**
     * Indicates whether the current configuration is valid.
     */
    val isValid: Boolean
        get() = configurationError == null

    /**
     * Indicates that the current configuration should be accepted as the "last known valid"
     * configuration.
     */
    fun acceptConfiguration() {
        preferences.edit().putString(KEY_LAST_HASH, configHash).apply()
    }

    val connectionBuilder: ConnectionBuilder
        get() = DefaultConnectionBuilder.INSTANCE

    private val lastKnownConfigHash: String?
        get() = preferences.getString(KEY_LAST_HASH, null)

    @Throws(InvalidConfigurationException::class)
    private fun readConfiguration() {
        val configSource = Okio.buffer(Okio.source(resources.openRawResource(R.raw.auth_config)))
        val configData = Buffer()
        configJson = try {
            configSource.readAll(configData)
            JSONObject(configData.readString(Charset.forName("UTF-8")))
        } catch (ex: IOException) {
            throw InvalidConfigurationException(
                    "Failed to read configuration: " + ex.message)
        } catch (ex: JSONException) {
            throw InvalidConfigurationException(
                    "Unable to parse configuration: " + ex.message)
        }
        configHash = configData.sha256().base64()
        clientId = getConfigString("client_id")
        scope = getRequiredConfigString("authorization_scope")
        redirectUri = getRequiredConfigUri("redirect_uri")
        endSessionUri = getRequiredConfigUri("end_session_uri")
        if (!isRedirectUriRegistered) {
            throw InvalidConfigurationException(
                    "redirect_uri is not handled by any activity in this app! "
                            + "Ensure that the appAuthRedirectScheme in your build.gradle file "
                            + "is correctly configured, or that an appropriate intent filter "
                            + "exists in your app manifest.")
        }
        if (getConfigString("discovery_uri") == null) {
            authEndpointUri = getRequiredConfigWebUri("authorization_endpoint_uri")
            tokenEndpointUri = getRequiredConfigWebUri("token_endpoint_uri")
            userInfoEndpointUri = getRequiredConfigWebUri("user_info_endpoint_uri")
            endSessionEndpoint = getRequiredConfigUri("end_session_endpoint")
            if (clientId == null) {
                registrationEndpointUri = getRequiredConfigWebUri("registration_endpoint_uri")
            }
        } else {
            discoveryUri = getRequiredConfigWebUri("discovery_uri")
        }
        isHttpsRequired = configJson!!.optBoolean("https_required", true)
    }

    private fun getConfigString(propName: String?): String? {
        var value = configJson!!.optString(propName) ?: return null
        value = value.trim { it <= ' ' }
        return if (TextUtils.isEmpty(value)) {
            null
        } else value
    }

    @Throws(InvalidConfigurationException::class)
    fun getRequiredConfigString(propName: String): String {
        return getConfigString(propName)
                ?: throw InvalidConfigurationException(
                        "$propName is required but not specified in the configuration")
    }

    @Throws(InvalidConfigurationException::class)
    fun getRequiredConfigUri(propName: String): Uri {
        val uriStr = getRequiredConfigString(propName)
        val uri: Uri = try {
            Uri.parse(uriStr)
        } catch (ex: Throwable) {
            throw InvalidConfigurationException("$propName could not be parsed", ex)
        }
        if (!uri.isHierarchical || !uri.isAbsolute) {
            throw InvalidConfigurationException(
                    "$propName must be hierarchical and absolute")
        }
        if (!TextUtils.isEmpty(uri.encodedUserInfo)) {
            throw InvalidConfigurationException("$propName must not have user info")
        }
        if (!TextUtils.isEmpty(uri.encodedQuery)) {
            throw InvalidConfigurationException("$propName must not have query parameters")
        }
        if (!TextUtils.isEmpty(uri.encodedFragment)) {
            throw InvalidConfigurationException("$propName must not have a fragment")
        }
        return uri
    }

    @Throws(InvalidConfigurationException::class)
    fun getRequiredConfigWebUri(propName: String): Uri {
        val uri = getRequiredConfigUri(propName)
        val scheme = uri.scheme
        if (TextUtils.isEmpty(scheme) || !("http" == scheme || "https" == scheme)) {
            throw InvalidConfigurationException(
                    "$propName must have an http or https scheme")
        }
        return uri
    }

    // ensure that the redirect URI declared in the configuration is handled by some activity
    // in the app, by querying the package manager speculatively
    private val isRedirectUriRegistered: Boolean
        get() {
            // ensure that the redirect URI declared in the configuration is handled by some activity
            // in the app, by querying the package manager speculatively
            val redirectIntent = Intent()
            redirectIntent.setPackage(mContext.packageName)
            redirectIntent.action = Intent.ACTION_VIEW
            redirectIntent.addCategory(Intent.CATEGORY_BROWSABLE)
            redirectIntent.data = redirectUri
            return mContext.packageManager.queryIntentActivities(redirectIntent, 0).isNotEmpty()
        }

    class InvalidConfigurationException : Exception {
        internal constructor(reason: String?) : super(reason)
        internal constructor(reason: String?, cause: Throwable?) : super(reason, cause)
    }

    companion object {
        //private const val TAG = "Configuration"
        private const val PREFS_NAME = "config"
        private const val KEY_LAST_HASH = "lastHash"
        private var sInstance = WeakReference<Configuration?>(null)
        fun getInstance(context: Context): Configuration {
            var config = sInstance.get()
            if (config == null) {
                config = Configuration(context)
                sInstance = WeakReference(config)
            }
            return config
        }
    }

    init {
        try {
            readConfiguration()
        } catch (ex: InvalidConfigurationException) {
            configurationError = ex.message
        }
    }
}