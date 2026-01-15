package com.baverika.r_journal.repository

import android.content.Context
import android.content.SharedPreferences
import com.baverika.r_journal.ui.theme.AppTheme

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("r_journal_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_BIRTH_DAY = "birth_day"
        private const val KEY_BIRTH_MONTH = "birth_month"
        private const val KEY_BIRTH_YEAR = "birth_year"
        private const val KEY_LAST_BIRTHDAY_SHOWN_YEAR = "last_birthday_shown_year"
        private const val KEY_SPECIAL_MOMENTS_ENABLED = "special_moments_enabled"
    }

    var birthDay: Int
        get() = prefs.getInt(KEY_BIRTH_DAY, 14) // Default: User's birthday
        set(value) = prefs.edit().putInt(KEY_BIRTH_DAY, value).apply()

    var birthMonth: Int
        get() = prefs.getInt(KEY_BIRTH_MONTH, 4) // Default: April
        set(value) = prefs.edit().putInt(KEY_BIRTH_MONTH, value).apply()

    var birthYear: Int
        get() = prefs.getInt(KEY_BIRTH_YEAR, 1999) // Default: 1999
        set(value) = prefs.edit().putInt(KEY_BIRTH_YEAR, value).apply()

    var lastBirthdayShownYear: Int
        get() = prefs.getInt(KEY_LAST_BIRTHDAY_SHOWN_YEAR, -1)
        set(value) = prefs.edit().putInt(KEY_LAST_BIRTHDAY_SHOWN_YEAR, value).apply()

    var specialMomentsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SPECIAL_MOMENTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SPECIAL_MOMENTS_ENABLED, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()
        }

    var appTheme: AppTheme
        get() {
            val themeName = prefs.getString(KEY_APP_THEME, AppTheme.MIDNIGHT.name) ?: AppTheme.MIDNIGHT.name
            return try {
                AppTheme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                AppTheme.MIDNIGHT
            }
        }
        set(value) {
            prefs.edit().putString(KEY_APP_THEME, value.name).apply()
        }
}
