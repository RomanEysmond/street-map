package com.example.example.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedApiKeyStore @Inject constructor(
    @ApplicationContext context: Context
) : ApiKeyStore {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val preferences: SharedPreferences = EncryptedSharedPreferences.create(
        PREFS_FILE_NAME,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun get(): String? = preferences.getString(KEY_API_KEY, null)

    override fun save(key: String) {
        preferences.edit().putString(KEY_API_KEY, key).apply()
    }

    override fun observeHasKey(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == KEY_API_KEY) {
                trySend(!get().isNullOrBlank())
            }
        }
        trySend(!get().isNullOrBlank())
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private companion object {
        const val PREFS_FILE_NAME = "secure_prefs"
        const val KEY_API_KEY = "opentripmap_api_key"
    }
}
