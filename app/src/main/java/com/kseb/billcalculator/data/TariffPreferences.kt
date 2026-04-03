package com.kseb.billcalculator.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kseb.billcalculator.model.TariffConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "tariff_preferences")

class TariffPreferences(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        val TARIFF_CONFIG_KEY = stringPreferencesKey("tariff_config")
    }

    val tariffConfig: Flow<TariffConfig> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[TARIFF_CONFIG_KEY]
        if (jsonString != null) {
            try {
                json.decodeFromString<TariffConfig>(jsonString)
            } catch (_: Exception) {
                TariffConfig.DEFAULT
            }
        } else {
            TariffConfig.DEFAULT
        }
    }

    suspend fun saveTariffConfig(config: TariffConfig) {
        context.dataStore.edit { preferences ->
            preferences[TARIFF_CONFIG_KEY] = json.encodeToString(config)
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.remove(TARIFF_CONFIG_KEY)
        }
    }
}
