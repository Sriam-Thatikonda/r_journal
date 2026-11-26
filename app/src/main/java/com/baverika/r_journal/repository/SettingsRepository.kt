package com.baverika.r_journal.repository

import android.content.Context
import android.content.SharedPreferences
import com.baverika.r_journal.ui.theme.AppFontStyle
import com.baverika.r_journal.ui.theme.AppTheme

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("r_journal_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_APP_FONT_STYLE = "app_font_style"
        private const val KEY_BIRTH_DAY = "birth_day"
        private const val KEY_BIRTH_MONTH = "birth_month"
        private const val KEY_BIRTH_YEAR = "birth_year"
        private const val KEY_LAST_BIRTHDAY_SHOWN_YEAR = "last_birthday_shown_year"
        private const val KEY_SPECIAL_MOMENTS_ENABLED = "special_moments_enabled"
        private const val KEY_WIDGET_OPACITY = "widget_opacity"
        private const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"
        private const val KEY_BACKUP_REMINDER_DAYS = "backup_reminder_days"
        private const val KEY_NOTIF_TASKS_ENABLED = "notif_tasks_enabled"
        private const val KEY_NOTIF_TASK_SUMMARY_ENABLED = "notif_task_summary_enabled"
        private const val KEY_NOTIF_HABITS_ENABLED = "notif_habits_enabled"
        private const val KEY_NOTIF_JOURNAL_ENABLED = "notif_journal_enabled"
        private const val KEY_NOTIF_EVENTS_ENABLED = "notif_events_enabled"
        private const val KEY_LAST_BACKUP_NOTIFIED_TIMESTAMP = "last_backup_notified_timestamp"
        private const val KEY_LAST_HABIT_NOTIFIED_DATE = "last_habit_notified_date"
        private const val KEY_LAST_TASK_SUMMARY_NOTIFIED_DATE = "last_task_summary_notified_date"
        private const val KEY_LAST_JOURNAL_NOTIFIED_DATE = "last_journal_notified_date"
        private const val KEY_LAST_EVENT_NOTIFIED_DATE = "last_event_notified_date"
    }

    var notifTasksEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_TASKS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_TASKS_ENABLED, value).apply()

    var notifTaskSummaryEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_TASK_SUMMARY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_TASK_SUMMARY_ENABLED, value).apply()

    var notifHabitsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_HABITS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_HABITS_ENABLED, value).apply()

    var notifJournalEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_JOURNAL_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_JOURNAL_ENABLED, value).apply()

    var notifEventsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_EVENTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_EVENTS_ENABLED, value).apply()

    var lastBackupNotifiedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_NOTIFIED_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_NOTIFIED_TIMESTAMP, value).apply()

    var lastHabitNotifiedDate: String
        get() = prefs.getString(KEY_LAST_HABIT_NOTIFIED_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_HABIT_NOTIFIED_DATE, value).apply()

    var lastTaskSummaryNotifiedDate: String
        get() = prefs.getString(KEY_LAST_TASK_SUMMARY_NOTIFIED_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_TASK_SUMMARY_NOTIFIED_DATE, value).apply()

    var lastJournalNotifiedDate: String
        get() = prefs.getString(KEY_LAST_JOURNAL_NOTIFIED_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_JOURNAL_NOTIFIED_DATE, value).apply()

    var lastEventNotifiedDate: String
        get() = prefs.getString(KEY_LAST_EVENT_NOTIFIED_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_EVENT_NOTIFIED_DATE, value).apply()

    var lastBackupTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_TIMESTAMP, value).apply()

    var backupReminderDays: Int
        get() = prefs.getInt(KEY_BACKUP_REMINDER_DAYS, 7)
        set(value) = prefs.edit().putInt(KEY_BACKUP_REMINDER_DAYS, value).apply()

    fun isBackupOverdue(): Boolean {
        if (backupReminderDays <= 0) return false
        val now = System.currentTimeMillis()
        val intervalMs = backupReminderDays * 24 * 60 * 60 * 1000L
        return (now - lastBackupTimestamp) > intervalMs
    }

    var widgetOpacity: Int
        get() = prefs.getInt(KEY_WIDGET_OPACITY, 80)
        set(value) = prefs.edit().putInt(KEY_WIDGET_OPACITY, value).apply()

    var birthDay: Int
        get() = prefs.getInt(KEY_BIRTH_DAY, 1)
        set(value) = prefs.edit().putInt(KEY_BIRTH_DAY, value).apply()

    var birthMonth: Int
        get() = prefs.getInt(KEY_BIRTH_MONTH, 1)
        set(value) = prefs.edit().putInt(KEY_BIRTH_MONTH, value).apply()

    var birthYear: Int
        get() = prefs.getInt(KEY_BIRTH_YEAR, 2000)
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

    fun hasExplicitBiometricSetting(): Boolean = prefs.contains(KEY_BIOMETRIC_ENABLED)

    var appTheme: AppTheme
        get() {
            val themeName = prefs.getString(KEY_APP_THEME, AppTheme.OBSIDIAN.name)
            return AppTheme.fromPersistedName(themeName)
        }
        set(value) {
            prefs.edit().putString(KEY_APP_THEME, value.name).apply()
        }

    var appFontStyle: AppFontStyle
        get() {
            val styleName = prefs.getString(KEY_APP_FONT_STYLE, AppFontStyle.MODERN.name)
            return AppFontStyle.fromPersistedName(styleName)
        }
        set(value) {
            prefs.edit().putString(KEY_APP_FONT_STYLE, value.name).apply()
        }
}
