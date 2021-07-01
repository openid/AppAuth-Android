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
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.AnyThread
import org.json.JSONException
import net.openid.appauth.*
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

class AuthStateManager private constructor(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)
    private val preferencesLock: ReentrantLock = ReentrantLock()
    private val currentAuthState: AtomicReference<AuthState> = AtomicReference()

    @get:AnyThread
    val current: AuthState
        get() {
            if (currentAuthState.get() != null) {
                return currentAuthState.get()
            }
            val state = readState()
            return if (currentAuthState.compareAndSet(null, state)) {
                state
            } else {
                currentAuthState.get()
            }
        }

    @AnyThread
    fun replace(state: AuthState): AuthState {
        writeState(state)
        currentAuthState.set(state)
        return state
    }

    @AnyThread
    fun updateAfterAuthorization(
            response: AuthorizationResponse?,
            ex: AuthorizationException?): AuthState {
        val current = current
        current.update(response, ex)
        return replace(current)
    }

    @AnyThread
    fun updateAfterTokenResponse(
            response: TokenResponse?,
            ex: AuthorizationException?): AuthState {
        val current = current
        current.update(response, ex)
        return replace(current)
    }

    @AnyThread
    fun updateAfterRegistration(
            response: RegistrationResponse?,
            ex: AuthorizationException?): AuthState {
        val current = current
        if (ex != null) {
            return current
        }
        current.update(response)
        return replace(current)
    }

    @AnyThread
    private fun readState(): AuthState {
        preferencesLock.lock()
        return try {
            val currentState = preferences.getString(KEY_STATE, null)
                    ?: return AuthState()
            try {
                AuthState.jsonDeserialize(currentState)
            } catch (ex: JSONException) {
                Log.w(TAG, "Failed to deserialize stored auth state - discarding")
                AuthState()
            }
        } finally {
            preferencesLock.unlock()
        }
    }

    @AnyThread
    private fun writeState(state: AuthState?) {
        preferencesLock.lock()
        try {
            val editor = preferences.edit()
            if (state == null) {
                editor.remove(KEY_STATE)
            } else {
                editor.putString(KEY_STATE, state.jsonSerializeString())
            }
            check(editor.commit()) { "Failed to write state to shared prefs" }
        } finally {
            preferencesLock.unlock()
        }
    }

    companion object {
        private val INSTANCE_REF = AtomicReference(WeakReference<AuthStateManager?>(null))
        private const val TAG = "AuthStateManager"
        private const val STORE_NAME = "AuthState"
        private const val KEY_STATE = "state"

        @AnyThread
        fun getInstance(context: Context): AuthStateManager {
            var manager = INSTANCE_REF.get().get()
            if (manager == null) {
                manager = AuthStateManager(context.applicationContext)
                INSTANCE_REF.set(WeakReference(manager))
            }
            return manager
        }
    }

}
