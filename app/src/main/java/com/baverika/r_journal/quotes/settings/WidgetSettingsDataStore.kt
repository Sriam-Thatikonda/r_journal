package com.baverika.r_journal.quotes.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Extension property for DataStore preferences
 */
private val Context.quoteWidgetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "quote_widget_settings"
)

/**
 * Enum representing widget refresh interval options
 */
enum class WidgetRefreshInterval(val displayName: String, val description: String) {
    EVERY_DAY("Every Day", "Widget updates once every 24 hours"),
    EVERY_HOUR("Every Hour", "Widget updates every hour"),
    ON_SCREEN_UNLOCK("On Screen Unlock", "Widget updates when you unlock your phone")
}

/**
 * Data class representing widget settings
 */
data class WidgetSettings(
    val refreshInterval: WidgetRefreshInterval = WidgetRefreshInterval.EVERY_DAY,
    val lastShownQuoteId: Int = -1
)

/**
 * DataStore manager for widget settings.
 * Persists user preferences for quote widget behavior.
 */
class WidgetSettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val REFRESH_INTERVAL = stringPreferencesKey("refresh_interval")
        val LAST_SHOWN_QUOTE_ID = intPreferencesKey("last_shown_quote_id")
    }

    /**
     * Flow of current widget settings
     */
    val settingsFlow: Flow<WidgetSettings> = context.quoteWidgetDataStore.data
        .catch { exception ->
            // Handle errors by emitting default values
            emit(emptyPreferences())
        }
        .map { preferences ->
            val intervalName = preferences[PreferencesKeys.REFRESH_INTERVAL]
                ?: WidgetRefreshInterval.EVERY_DAY.name
            
            // Handle migration from old ON_UNLOCK to new ON_SCREEN_UNLOCK
            val mappedIntervalName = when (intervalName) {
                "ON_UNLOCK" -> WidgetRefreshInterval.ON_SCREEN_UNLOCK.name
                else -> intervalName
            }
            
            val refreshInterval = try {
                WidgetRefreshInterval.valueOf(mappedIntervalName)
            } catch (e: IllegalArgumentException) {
                WidgetRefreshInterval.EVERY_DAY
            }

            val lastShownId = preferences[PreferencesKeys.LAST_SHOWN_QUOTE_ID] ?: -1

            WidgetSettings(
                refreshInterval = refreshInterval,
                lastShownQuoteId = lastShownId
            )
        }

    /**
     * Update the refresh interval setting
     */
    suspend fun setRefreshInterval(interval: WidgetRefreshInterval) {
        context.quoteWidgetDataStore.edit { preferences ->
            preferences[PreferencesKeys.REFRESH_INTERVAL] = interval.name
        }
    }

    /**
     * Update the last shown quote ID (to avoid repetition)
     */
    suspend fun setLastShownQuoteId(quoteId: Int) {
        context.quoteWidgetDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SHOWN_QUOTE_ID] = quoteId
        }
    }

    /**
     * Get the last shown quote ID synchronously (for one-shot reads)
     */
    suspend fun getLastShownQuoteId(): Int {
        return try {
            context.quoteWidgetDataStore.data
                .map { preferences -> preferences[PreferencesKeys.LAST_SHOWN_QUOTE_ID] ?: -1 }
                .first()
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Get current refresh interval synchronously
     */
    suspend fun getRefreshInterval(): WidgetRefreshInterval {
        return try {
            context.quoteWidgetDataStore.data
                .map { preferences ->
                    val intervalName = preferences[PreferencesKeys.REFRESH_INTERVAL]
                        ?: WidgetRefreshInterval.EVERY_DAY.name
                    
                    // Handle migration from old ON_UNLOCK to new ON_SCREEN_UNLOCK
                    val mappedIntervalName = when (intervalName) {
                        "ON_UNLOCK" -> WidgetRefreshInterval.ON_SCREEN_UNLOCK.name
                        else -> intervalName
                    }
                    
                    try {
                        WidgetRefreshInterval.valueOf(mappedIntervalName)
                    } catch (e: IllegalArgumentException) {
                        WidgetRefreshInterval.EVERY_DAY
                    }
                }
                .first()
        } catch (e: Exception) {
            WidgetRefreshInterval.EVERY_DAY
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: WidgetSettingsDataStore? = null

        fun getInstance(context: Context): WidgetSettingsDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetSettingsDataStore(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
