package com.authmanager.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore(name = "auth_manager_session")

/**
 * Persists only a "remember this login" flag — never the password itself.
 * When set, the app skips the login screen on next launch. The username and
 * password stay hardcoded in GitHubConfig (already on-device either way);
 * this store just remembers that the admin previously chose to stay
 * logged in, so nothing more sensitive than a boolean touches disk here.
 */
class SessionStore(private val context: Context) {

    private val rememberMeKey = booleanPreferencesKey("remember_me")

    suspend fun setRememberMe(value: Boolean) {
        context.sessionDataStore.edit { prefs ->
            prefs[rememberMeKey] = value
        }
    }

    suspend fun isRemembered(): Boolean {
        return context.sessionDataStore.data.first()[rememberMeKey] ?: false
    }
}
