package com.authmanager.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "auth_manager_cache")

/**
 * Simple last-known-good cache for the three repo files. Used when a live GitHub
 * fetch fails (no internet, API down) so the app can still show something instead
 * of a blank screen — and is overwritten every time a live fetch succeeds.
 */
class LocalCache(private val context: Context) {

    private val keysKey = stringPreferencesKey("cache_keys")
    private val devicesKey = stringPreferencesKey("cache_devices")
    private val blockedKey = stringPreferencesKey("cache_blocked")
    private val timestampKey = stringPreferencesKey("cache_timestamp")

    suspend fun save(keysRaw: String, devicesRaw: String, blockedRaw: String) {
        context.dataStore.edit { prefs ->
            prefs[keysKey] = keysRaw
            prefs[devicesKey] = devicesRaw
            prefs[blockedKey] = blockedRaw
            prefs[timestampKey] = System.currentTimeMillis().toString()
        }
    }

    /** Returns Triple(keysRaw, devicesRaw, blockedRaw) or null if nothing cached yet. */
    suspend fun load(): CachedData? {
        val prefs = context.dataStore.data.first()
        val keysRaw = prefs[keysKey] ?: return null
        val devicesRaw = prefs[devicesKey] ?: return null
        val blockedRaw = prefs[blockedKey] ?: return null
        val ts = prefs[timestampKey]?.toLongOrNull() ?: 0L
        return CachedData(keysRaw, devicesRaw, blockedRaw, ts)
    }
}

data class CachedData(
    val keysRaw: String,
    val devicesRaw: String,
    val blockedRaw: String,
    val cachedAt: Long,
)
