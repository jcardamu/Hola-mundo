package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("jellyfin_prefs")

class PreferencesManager(private val context: Context) {
    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val USER_ID = stringPreferencesKey("user_id")
        val HAS_LAUNCHED = androidx.datastore.preferences.core.booleanPreferencesKey("has_launched")
    }

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[SERVER_URL] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val hasLaunched: Flow<Boolean?> = context.dataStore.data.map { it[HAS_LAUNCHED] }

    suspend fun setHasLaunched() {
        context.dataStore.edit { prefs ->
            prefs[HAS_LAUNCHED] = true
        }
    }

    suspend fun saveAuthData(url: String, token: String, id: String) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_URL] = url
            prefs[ACCESS_TOKEN] = token
            prefs[USER_ID] = id
        }
    }

    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs.remove(SERVER_URL)
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(USER_ID)
        }
    }
}
