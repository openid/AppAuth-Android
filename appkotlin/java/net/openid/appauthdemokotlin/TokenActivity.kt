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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import net.openid.appauth.*
import net.openid.appauth.AuthorizationService.TokenResponseCallback
import net.openid.appauth.ClientAuthentication.UnsupportedAuthenticationMethod
import net.openid.appauthdemokotlin.databinding.ActivityTokenBinding
import okio.Okio
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * Displays the authorized state of the user. This activity is provided with the outcome of the
 * authorization flow, which it uses to negotiate the final authorized state,
 * by performing an authorization code exchange if necessary. After this, the activity provides
 * additional post-authorization operations if available, such as fetching user info and refreshing
 * access tokens.
 */
class TokenActivity : AppCompatActivity() {
    private lateinit var authService: AuthorizationService
    private lateinit var stateManager: AuthStateManager
    private val userInfoJson = AtomicReference<JSONObject?>()
    private lateinit var executor: ExecutorService
    private lateinit var configuration: Configuration
    private lateinit var binding: ActivityTokenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateManager = AuthStateManager.getInstance(this)
        executor = Executors.newSingleThreadExecutor()
        configuration = Configuration.getInstance(this)
        val config: Configuration = Configuration.getInstance(this)
        if (config.hasConfigurationChanged()) {
            Toast.makeText(
                    this,
                    "Configuration change detected",
                    Toast.LENGTH_SHORT
            )
                    .show()
            signOut()
            return
        }
        authService = AuthorizationService(
                this,
                AppAuthConfiguration.Builder()
                        .setConnectionBuilder(config.connectionBuilder)
                        .build()
        )
        binding = ActivityTokenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        displayLoading("Restoring state...")
        if (savedInstanceState != null) {
            try {
                userInfoJson.set(JSONObject(savedInstanceState.getString(KEY_USER_INFO)!!))
            } catch (ex: JSONException) {
                Log.e(TAG, "Failed to parse saved user info JSON, discarding", ex)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (executor.isShutdown) {
            executor = Executors.newSingleThreadExecutor()
        }
        if (stateManager.current.isAuthorized) {
            displayAuthorized()
            return
        }

        // the stored AuthState is incomplete, so check if we are currently receiving the result of
        // the authorization flow from the browser.
        val response = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        if (response != null || ex != null) {
            stateManager.updateAfterAuthorization(response, ex)
        }
        when {
            response?.authorizationCode != null -> {
                // authorization code exchange is required
                stateManager.updateAfterAuthorization(response, ex)
                exchangeAuthorizationCode(response)
            }
            ex != null -> {
                displayNotAuthorized("Authorization flow failed: " + ex.message)
            }
            else -> {
                displayNotAuthorized("No authorization state retained - reauthorization required")
            }
        }
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        // user info is retained to survive activity restarts, such as when rotating the
        // device or switching apps. This isn't essential, but it helps provide a less
        // jarring UX when these events occur - data does not just disappear from the view.
        if (userInfoJson.get() != null) {
            state.putString(KEY_USER_INFO, userInfoJson.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authService.dispose()
        executor.shutdownNow()
    }

    @MainThread
    private fun displayNotAuthorized(explanation: String) {
        binding.notAuthorized.visibility = View.VISIBLE
        binding.authorized.visibility = View.GONE
        binding.loadingContainer.visibility = View.GONE
        binding.explanation.text = explanation
        binding.reauth.setOnClickListener { signOut() }
    }

    @MainThread
    private fun displayLoading(message: String) {
        binding.loadingContainer.visibility = View.VISIBLE
        binding.authorized.visibility = View.GONE
        binding.notAuthorized.visibility = View.GONE
        binding.loadingDescription.text = message
    }

    @MainThread
    private fun displayAuthorized() {
        binding.authorized.visibility = View.VISIBLE
        binding.notAuthorized.visibility = View.GONE
        binding.loadingContainer.visibility = View.GONE
        val state = stateManager.current
        val refreshTokenInfoView = binding.refreshTokenInfo
        refreshTokenInfoView.setText(if (state.refreshToken == null) R.string.no_refresh_token_returned else R.string.refresh_token_returned)
        val idTokenInfoView = binding.idTokenInfo
        idTokenInfoView.setText(if (state.idToken == null) R.string.no_id_token_returned else R.string.id_token_returned)
        val accessTokenInfoView = binding.accessTokenInfo
        if (state.accessToken == null) {
            accessTokenInfoView.setText(R.string.no_access_token_returned)
        } else {
            val expiresAt = state.accessTokenExpirationTime
            when {
                expiresAt == null -> {
                    accessTokenInfoView.setText(R.string.no_access_token_expiry)
                }
                expiresAt < System.currentTimeMillis() -> {
                    accessTokenInfoView.setText(R.string.access_token_expired)
                }
                else -> {
                    val template = resources.getString(R.string.access_token_expires_at)
                    accessTokenInfoView.text = String.format(
                            template,
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(expiresAt)
                    )
                }
            }
        }
        val refreshTokenButton = binding.refreshToken
        refreshTokenButton.visibility = if (state.refreshToken != null) View.VISIBLE else View.GONE
        refreshTokenButton.setOnClickListener { refreshAccessToken() }
        val viewProfileButton = binding.viewProfile
        val discoveryDoc = state.authorizationServiceConfiguration!!.discoveryDoc
        if ((discoveryDoc == null || discoveryDoc.userinfoEndpoint == null)
                && configuration.userInfoEndpointUri == null
        ) {
            viewProfileButton.visibility = View.GONE
        } else {
            viewProfileButton.visibility = View.VISIBLE
            viewProfileButton.setOnClickListener { fetchUserInfo() }
        }
        binding.signOut.setOnClickListener { endSession() }
        val userInfoCard = binding.userinfoCard
        val userInfo = userInfoJson.get()
        if (userInfo == null) {
            userInfoCard.visibility = View.INVISIBLE
        } else {
            try {
                var name: String? = "???"
                if (userInfo.has("name")) {
                    name = userInfo.getString("name")
                }
                binding.userinfoName.text = name
                if (userInfo.has("picture")) {
                    Glide.with(this@TokenActivity)
                            .load(Uri.parse(userInfo.getString("picture")))
                            .fitCenter()
                            .into(binding.userinfoProfile)
                }
                binding.userinfoJson.text = userInfoJson.toString()
                userInfoCard.visibility = View.VISIBLE
            } catch (ex: JSONException) {
                Log.e(TAG, "Failed to read userinfo JSON", ex)
            }
        }
    }

    @MainThread
    private fun refreshAccessToken() {
        displayLoading("Refreshing access token")
        performTokenRequest(stateManager.current.createTokenRefreshRequest()) { tokenResponse: TokenResponse?,
                                                                                authException: AuthorizationException? ->
            handleAccessTokenResponse(tokenResponse, authException)
        }
    }

    @MainThread
    private fun exchangeAuthorizationCode(authorizationResponse: AuthorizationResponse) {
        displayLoading("Exchanging authorization code")
        performTokenRequest(
                authorizationResponse.createTokenExchangeRequest()
        ) { tokenResponse: TokenResponse?,
            authException: AuthorizationException? ->
            handleCodeExchangeResponse(tokenResponse, authException)
        }
    }

    @MainThread
    private fun performTokenRequest(
            request: TokenRequest,
            callback: TokenResponseCallback
    ) {
        val clientAuthentication: ClientAuthentication = try {
            stateManager.current.clientAuthentication
        } catch (ex: UnsupportedAuthenticationMethod) {
            Log.d(
                    TAG, "Token request cannot be made, client authentication for the token "
                    + "endpoint could not be constructed (%s)", ex
            )
            displayNotAuthorized("Client authentication method is unsupported")
            return
        }
        authService.performTokenRequest(
                request,
                clientAuthentication,
                callback
        )
    }

    @WorkerThread
    private fun handleAccessTokenResponse(
            tokenResponse: TokenResponse?,
            authException: AuthorizationException?
    ) {
        stateManager.updateAfterTokenResponse(tokenResponse, authException)
        runOnUiThread { displayAuthorized() }
    }

    @WorkerThread
    private fun handleCodeExchangeResponse(
            tokenResponse: TokenResponse?,
            authException: AuthorizationException?
    ) {
        stateManager.updateAfterTokenResponse(tokenResponse, authException)
        if (!stateManager.current.isAuthorized) {
            val message = ("Authorization Code exchange failed"
                    + if (authException != null) authException.error else "")
            runOnUiThread { displayNotAuthorized(message) }
        } else {
            runOnUiThread { displayAuthorized() }
        }
    }

    /**
     * Demonstrates the use of [AuthState.performActionWithFreshTokens] to retrieve
     * user info from the IDP's user info endpoint. This callback will negotiate a new access
     * token / id token for use in a follow-up action, or provide an error if this fails.
     */
    @MainThread
    private fun fetchUserInfo() {
        displayLoading("Fetching user info")
        stateManager.current.performActionWithFreshTokens(authService) { accessToken: String?, idToken: String?,
                                                                         ex: AuthorizationException? ->
            this.fetchUserInfo(accessToken, idToken, ex)
        }
    }

    @MainThread
    private fun fetchUserInfo(accessToken: String?, idToken: String?, ex: AuthorizationException?) {
        if (ex != null) {
            Log.e(TAG, "Token refresh failed when fetching user info")
            userInfoJson.set(null)
            runOnUiThread { displayAuthorized() }
            return
        }
        val discovery = stateManager.current.authorizationServiceConfiguration!!.discoveryDoc
        val userInfoEndpoint: URL = try {
            if (configuration.userInfoEndpointUri != null) {
                URL(configuration.userInfoEndpointUri.toString())
            } else {
                URL(discovery!!.userinfoEndpoint.toString())
            }
        } catch (urlEx: MalformedURLException) {
            Log.e(TAG, "Failed to construct user info endpoint URL", urlEx)
            userInfoJson.set(null)
            runOnUiThread { displayAuthorized() }
            return
        }
        executor.submit {
            try {
                val conn = userInfoEndpoint.openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $accessToken")
                conn.instanceFollowRedirects = false
                val response = Okio.buffer(Okio.source(conn.inputStream))
                        .readString(Charset.forName("UTF-8"))
                userInfoJson.set(JSONObject(response))
            } catch (ioEx: IOException) {
                Log.e(TAG, "Network error when querying userinfo endpoint", ioEx)
                Snackbar.make(binding.coordinator, "Fetching user info failed", Snackbar.LENGTH_SHORT).show()
            } catch (jsonEx: JSONException) {
                Log.e(TAG, "Failed to parse userinfo response")
                Snackbar.make(binding.coordinator, "Failed to parse userinfo response", Snackbar.LENGTH_SHORT).show()
            }
            runOnUiThread { displayAuthorized() }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == END_SESSION_REQUEST_CODE && resultCode == RESULT_OK) {
            signOut()
            finish()
        } else {
            Snackbar.make(binding.coordinator, "Sign out canceled", Snackbar.LENGTH_SHORT)
                    .show()
        }
    }

    @MainThread
    private fun endSession() {
        val endSessionEnter = authService.getEndSessionRequestIntent(
                EndSessionRequest.Builder(
                        stateManager.current.authorizationServiceConfiguration!!,
                        stateManager.current.idToken!!,
                        configuration.endSessionUri!!
                ).build()
        )
        startActivityForResult(endSessionEnter, END_SESSION_REQUEST_CODE)
    }

    @MainThread
    private fun signOut() {
        // discard the authorization and token state, but retain the configuration and
        // dynamic client registration (if applicable), to save from retrieving them again.
        val currentState = stateManager.current
        val clearedState = AuthState(currentState.authorizationServiceConfiguration!!)
        if (currentState.lastRegistrationResponse != null) {
            clearedState.update(currentState.lastRegistrationResponse)
        }
        stateManager.replace(clearedState)
        val mainIntent = Intent(this, LoginActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(mainIntent)
        finish()
    }

    companion object {
        private const val TAG = "TokenActivity"
        private const val KEY_USER_INFO = "userInfo"
        private const val END_SESSION_REQUEST_CODE = 911
    }
}