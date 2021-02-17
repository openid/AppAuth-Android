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

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.material.snackbar.Snackbar
import net.openid.appauth.*
import net.openid.appauth.browser.AnyBrowserMatcher
import net.openid.appauth.browser.BrowserMatcher
import net.openid.appauthdemokotlin.databinding.ActivityLoginBinding
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Demonstrates the usage of the AppAuth to authorize a user with an OAuth2 / OpenID Connect
 * provider. Based on the configuration provided in `res/raw/auth_config.json`, the code
 * contained here will:
 *
 * - Retrieve an OpenID Connect discovery document for the provider, or use a local static
 * configuration.
 * - Utilize dynamic client registration, if no static client id is specified.
 * - Initiate the authorization request using the built-in heuristics or a user-selected browser.
 *
 * _NOTE_: From a clean checkout of this project, the authorization service is not configured.
 * Edit `res/values/auth_config.xml` to provide the required configuration properties. See the
 * README.md in the app/ directory for configuration instructions, and the adjacent IDP-specific
 * instructions.
 */
class LoginActivity : AppCompatActivity() {
    private var authService: AuthorizationService? = null
    private lateinit var authStateManager: AuthStateManager
    private lateinit var configuration: Configuration
    private val clientId = AtomicReference<String?>()
    private val authRequest = AtomicReference<AuthorizationRequest?>()
    private val authIntent = AtomicReference<CustomTabsIntent?>()
    private var authIntentLatch = CountDownLatch(1)
    private lateinit var executor: ExecutorService
    private var usePendingIntents = false
    private var browserMatcher: BrowserMatcher = AnyBrowserMatcher.INSTANCE
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        executor = Executors.newSingleThreadExecutor()
        authStateManager = AuthStateManager.getInstance(this)
        configuration = Configuration.getInstance(this)
        if (authStateManager.current.isAuthorized
                && !configuration.hasConfigurationChanged()
        ) {
            Log.i(TAG, "User is already authenticated, proceeding to token activity")
            startActivity(Intent(this, TokenActivity::class.java))
            finish()
            return
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setContentView(R.layout.activity_login)
        binding.retry.setOnClickListener { executor.submit { initializeAppAuth() } }
        binding.startAuth.setOnClickListener { startAuth() }
        binding.loginHintValue.addTextChangedListener(
                LoginHintChangeHandler()
        )
        if (!configuration.isValid) {
            displayError(configuration.configurationError, false)
            return
        }
        if (configuration.hasConfigurationChanged()) {
            // discard any existing authorization state due to the change of configuration
            Log.i(TAG, "Configuration change detected, discarding old state")
            authStateManager.replace(AuthState())
            configuration.acceptConfiguration()
        }
        if (intent.getBooleanExtra(EXTRA_FAILED, false)) {
            Snackbar.make(binding.coordinator, "Authorization canceled", Snackbar.LENGTH_SHORT)
                    .show()
        }
        displayLoading("Initializing")
        executor.submit { initializeAppAuth() }
    }

    override fun onStart() {
        super.onStart()
        if (executor.isShutdown) {
            executor = Executors.newSingleThreadExecutor()
        }
    }

    override fun onStop() {
        var n = 10
        var i = false
        super.onStop()
        executor.shutdownNow()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (authService != null) {
            authService!!.dispose()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        displayAuthOptions()
        if (resultCode == RESULT_CANCELED) {
            Snackbar.make(binding.coordinator, "Authorization canceled", Snackbar.LENGTH_SHORT)
                    .show()
        } else {
            val intent = Intent(this, TokenActivity::class.java)
            intent.putExtras(data!!.extras!!)
            startActivity(intent)
        }
    }

    @MainThread
    fun startAuth() {
        displayLoading("Making authorization request")
        usePendingIntents = binding.pendingIntentsCheckbox.isChecked
        executor.submit { doAuth() }
    }

    /**
     * Initializes the authorization service configuration if necessary, either from the local
     * static values or by retrieving an OpenID discovery document.
     */
    @WorkerThread
    private fun initializeAppAuth() {
        Log.i(TAG, "Initializing AppAuth")
        recreateAuthorizationService()
        if (authStateManager.current.authorizationServiceConfiguration != null) {
            // configuration is already created, skip to client initialization
            Log.i(TAG, "auth config already established")
            initializeClient()
            return
        }

        // if we are not using discovery, build the authorization service configuration directly
        // from the static configuration values.
        if (configuration.discoveryUri == null) {
            Log.i(TAG, "Creating auth config from res/raw/auth_config.json")
            val config = AuthorizationServiceConfiguration(
                    configuration.authEndpointUri!!,
                    configuration.tokenEndpointUri!!,
                    configuration.endSessionEndpoint,
                    configuration.registrationEndpointUri
            )
            authStateManager.replace(AuthState(config))
            initializeClient()
            return
        }

        // WrongThread inference is incorrect for lambdas
        // noinspection WrongThread
        runOnUiThread { displayLoading("Retrieving discovery document") }
        Log.i(TAG, "Retrieving OpenID discovery doc")
        AuthorizationServiceConfiguration.fetchFromUrl(
                configuration.discoveryUri!!,
                { config: AuthorizationServiceConfiguration?, ex: AuthorizationException? ->
                    handleConfigurationRetrievalResult(
                            config,
                            ex
                    )
                },
                configuration.connectionBuilder
        )
    }

    @MainThread
    private fun handleConfigurationRetrievalResult(
            config: AuthorizationServiceConfiguration?,
            ex: AuthorizationException?
    ) {
        if (config == null) {
            Log.i(TAG, "Failed to retrieve discovery document", ex)
            displayError("Failed to retrieve discovery document: " + ex!!.message, true)
            return
        }
        Log.i(TAG, "Discovery document retrieved")
        authStateManager.replace(AuthState(config))
        executor.submit { initializeClient() }
    }

    /**
     * Initiates a dynamic registration request if a client ID is not provided by the static
     * configuration.
     */
    @WorkerThread
    private fun initializeClient() {
        if (configuration.clientId != null) {
            Log.i(TAG, "Using static client ID: " + configuration.clientId)
            // use a statically configured client ID
            clientId.set(configuration.clientId)
            runOnUiThread { initializeAuthRequest() }
            return
        }
        val lastResponse = authStateManager.current.lastRegistrationResponse
        if (lastResponse != null) {
            Log.i(TAG, "Using dynamic client ID: " + lastResponse.clientId)
            clientId.set(lastResponse.clientId)
            runOnUiThread { initializeAuthRequest() }
            return
        }

        runOnUiThread { displayLoading("Dynamically registering client") }
        Log.i(TAG, "Dynamically registering client")
        val registrationRequest = RegistrationRequest.Builder(
                authStateManager.current.authorizationServiceConfiguration!!,
                listOf(configuration.redirectUri)
        )
                .setTokenEndpointAuthenticationMethod(ClientSecretBasic.NAME)
                .build()
        authService!!.performRegistrationRequest(
                registrationRequest
        ) { response: RegistrationResponse?, ex: AuthorizationException? ->
            handleRegistrationResponse(
                    response,
                    ex
            )
        }
    }

    @MainThread
    private fun handleRegistrationResponse(
            response: RegistrationResponse?,
            ex: AuthorizationException?
    ) {
        authStateManager.updateAfterRegistration(response, ex)
        if (response == null) {
            Log.i(TAG, "Failed to dynamically register client", ex)
            runOnUiThread { displayError("Failed to register client: " + ex!!.message, true) }
            return
        }
        Log.i(TAG, "Dynamically registered client: " + response.clientId)
        clientId.set(response.clientId)
        initializeAuthRequest()
    }


    /**
     * Performs the authorization request, using the browser selected in the spinner,
     * and a user-provided `login_hint` if available.
     */
    @WorkerThread
    private fun doAuth() {
        try {
            authIntentLatch.await()
        } catch (ex: InterruptedException) {
            Log.w(TAG, "Interrupted while waiting for auth intent")
        }
        if (usePendingIntents) {
            val completionIntent = Intent(this, TokenActivity::class.java)
            val cancelIntent = Intent(this, LoginActivity::class.java)
            cancelIntent.putExtra(EXTRA_FAILED, true)
            cancelIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            authService!!.performAuthorizationRequest(
                    authRequest.get()!!,
                    PendingIntent.getActivity(this, 0, completionIntent, 0),
                    PendingIntent.getActivity(this, 0, cancelIntent, 0),
                    authIntent.get()!!
            )
        } else {
            val intent = authService!!.getAuthorizationRequestIntent(
                    authRequest.get()!!,
                    authIntent.get()!!
            )
            startActivityForResult(intent, RC_AUTH)
        }
    }

    private fun recreateAuthorizationService() {
        if (authService != null) {
            Log.i(TAG, "Discarding existing AuthService instance")
            authService!!.dispose()
        }
        authService = createAuthorizationService()
        authRequest.set(null)
        authIntent.set(null)
    }

    private fun createAuthorizationService(): AuthorizationService {
        Log.i(TAG, "Creating authorization service")
        val builder = AppAuthConfiguration.Builder()
        builder.setBrowserMatcher(browserMatcher)
        builder.setConnectionBuilder(configuration.connectionBuilder)
        return AuthorizationService(this, builder.build())
    }

    @MainThread
    private fun displayLoading(loadingMessage: String) {
        binding.loadingContainer.visibility = View.VISIBLE
        binding.authContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
        binding.loadingDescription.text = loadingMessage
    }

    @MainThread
    private fun displayError(error: String?, recoverable: Boolean) {
        binding.errorContainer.visibility = View.VISIBLE
        binding.loadingContainer.visibility = View.GONE
        binding.authContainer.visibility = View.GONE
        binding.errorDescription.text = error
        binding.retry.visibility = if (recoverable) View.VISIBLE else View.GONE
    }

    @MainThread
    private fun initializeAuthRequest() {
        createAuthRequest(loginHint)
        warmUpBrowser()
        displayAuthOptions()
    }

    @MainThread
    private fun displayAuthOptions() {
        binding.authContainer.visibility = View.VISIBLE
        binding.loadingContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
        val state = authStateManager.current
        val config = state.authorizationServiceConfiguration
        var authEndpointStr: String = if (config!!.discoveryDoc != null) {
            "Discovered auth endpoint: \n"
        } else {
            "Static auth endpoint: \n"
        }
        authEndpointStr += config.authorizationEndpoint
        binding.authEndpoint.text = authEndpointStr
        var clientIdStr: String = if (state.lastRegistrationResponse != null) {
            "Dynamic client ID: \n"
        } else {
            "Static client ID: \n"
        }
        clientIdStr += clientId
        binding.clientId.text = clientIdStr
    }

    private fun warmUpBrowser() {
        authIntentLatch = CountDownLatch(1)
        executor.execute {
            Log.i(TAG, "Warming up browser instance for auth request")
            val intentBuilder =
                    authService!!.createCustomTabsIntentBuilder(authRequest.get()!!.toUri())
            authIntent.set(intentBuilder.build())
            authIntentLatch.countDown()
        }
    }

    private fun createAuthRequest(loginHint: String?) {
        Log.i(TAG, "Creating auth request for login hint: $loginHint")
        val authRequestBuilder = AuthorizationRequest.Builder(
                authStateManager.current.authorizationServiceConfiguration!!,
                clientId.get()!!,
                ResponseTypeValues.CODE,
                configuration.redirectUri!!
        )
                .setScope(configuration.scope)
        if (!TextUtils.isEmpty(loginHint)) {
            authRequestBuilder.setLoginHint(loginHint)
        }
        authRequest.set(authRequestBuilder.build())
    }

    private val loginHint: String
        get() = binding.loginHintValue
                .text
                .toString()
                .trim { it <= ' ' }

    /**
     * Responds to changes in the login hint. After a "debounce" delay, warms up the browser
     * for a request with the new login hint; this avoids constantly re-initializing the
     * browser while the user is typing.
     */
    inner class LoginHintChangeHandler internal constructor() : TextWatcher {
        private val debounceDelayMs = 500
        private val mHandler: Handler = Handler(Looper.getMainLooper())
        private var task: RecreateAuthRequestTask
        override fun beforeTextChanged(cs: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(cs: CharSequence, start: Int, before: Int, count: Int) {
            task.cancel()
            task = RecreateAuthRequestTask()
            mHandler.postDelayed(task, debounceDelayMs.toLong())
        }

        override fun afterTextChanged(ed: Editable) {}

        init {
            task = RecreateAuthRequestTask()
        }
    }

    inner class RecreateAuthRequestTask : Runnable {
        private val mCanceled = AtomicBoolean()
        override fun run() {
            if (mCanceled.get()) {
                return
            }
            createAuthRequest(loginHint)
            warmUpBrowser()
        }

        fun cancel() {
            mCanceled.set(true)
        }
    }

    companion object {
        private const val TAG = "LoginActivity"
        private const val EXTRA_FAILED = "failed"
        private const val RC_AUTH = 100
    }
}