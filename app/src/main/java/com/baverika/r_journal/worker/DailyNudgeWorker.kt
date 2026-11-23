package com.baverika.r_journal.worker

import android.content.Context
import androidx.work.*
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.data.local.entity.EventType
import com.baverika.r_journal.notifications.AppNotificationManager
import com.baverika.r_journal.repository.SettingsRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Background worker that runs periodic health and proactive reminder checks:
 * - Backup overdue alerts
 * - Evening habit review & streak protection
 * - Morning task summary
 * - Evening journal reflection
 * - Birthday & event celebrations
 */
class DailyNudgeWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "daily_nudge_worker"

        fun schedule(context: Context) {
            val nudgeWork = PeriodicWorkRequestBuilder<DailyNudgeWorker>(
                4, TimeUnit.HOURS
            )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                nudgeWork
            )
        }
    }

    override suspend fun doWork(): Result {
        try {
            AppNotificationManager.createChannels(applicationContext)
            val settingsRepo = SettingsRepository(applicationContext)
            val db = JournalDatabase.getDatabase(applicationContext)

            val now = LocalDateTime.now()
            val today = now.toLocalDate()
            val todayDateStr = today.toString()
            val hour = now.hour

            // 1. BACKUP OVERDUE CHECK
            if (settingsRepo.isBackupOverdue()) {
                val hoursSinceNotified = (System.currentTimeMillis() - settingsRepo.lastBackupNotifiedTimestamp) / (1000 * 60 * 60)
                if (hoursSinceNotified >= 48) {
                    val daysSince = ((System.currentTimeMillis() - settingsRepo.lastBackupTimestamp) / (1000 * 60 * 60 * 24))
                        .coerceAtLeast(settingsRepo.backupReminderDays.toLong())
                    AppNotificationManager.showBackupReminder(applicationContext, daysSince.toInt())
                    settingsRepo.lastBackupNotifiedTimestamp = System.currentTimeMillis()
                }
            }

            // 2. MORNING TASK SUMMARY
            if (settingsRepo.notifTaskSummaryEnabled && hour in 7..11 && settingsRepo.lastTaskSummaryNotifiedDate != todayDateStr) {
                val todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val todayEnd = todayStart + 24 * 60 * 60 * 1000L
                val upcomingTasks = db.taskDao().getUpcomingTasksSync(100)
                val todayTasks = upcomingTasks.filter { task ->
                    task.dueDate != null && task.dueDate in todayStart until todayEnd && !task.isCompleted
                }
                if (todayTasks.isNotEmpty()) {
                    AppNotificationManager.showDailyTaskSummary(
                        applicationContext,
                        todayTasks.size,
                        todayTasks.map { it.title }
                    )
                    settingsRepo.lastTaskSummaryNotifiedDate = todayDateStr
                }
            }

            // 3. EVENING HABIT REVIEW & STREAK PROTECTION (7:00 PM onwards)
            if (settingsRepo.notifHabitsEnabled && hour >= 19 && settingsRepo.lastHabitNotifiedDate != todayDateStr) {
                val activeHabits = db.habitDao().getAllActiveHabitsSync()
                val dayOfWeek = today.dayOfWeek.value // 1=Mon, 7=Sun
                val scheduledToday = activeHabits.filter { it.frequency.contains(dayOfWeek) }
                val todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val todayLogs = db.habitDao().getHabitLogsForDateSync(todayStart)
                val completedHabitIds = todayLogs.map { it.habitId }.toSet()
                val pendingHabits = scheduledToday.filter { it.id !in completedHabitIds }

                if (pendingHabits.isNotEmpty()) {
                    AppNotificationManager.showHabitReview(
                        applicationContext,
                        pendingCount = pendingHabits.size,
                        pendingHabitNames = pendingHabits.take(3).joinToString(", ") { it.title }
                    )
                    settingsRepo.lastHabitNotifiedDate = todayDateStr
                }
            }

            // 4. EVENING JOURNAL REFLECTION (8:00 PM onwards)
            if (settingsRepo.notifJournalEnabled && hour >= 20 && settingsRepo.lastJournalNotifiedDate != todayDateStr) {
                val todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val entry = db.journalDao().getEntryByDate(todayStart)
                if (entry == null || entry.id == "mine") {
                    AppNotificationManager.showJournalPrompt(applicationContext)
                    settingsRepo.lastJournalNotifiedDate = todayDateStr
                }
            }

            // 5. BIRTHDAY & SPECIAL EVENT ALERTS (7:00 AM - 12:00 PM)
            if (settingsRepo.notifEventsEnabled && hour in 7..12 && settingsRepo.lastEventNotifiedDate != todayDateStr) {
                val events = db.eventDao().getAllEventsList()
                val todayEvents = events.filter { it.day == today.dayOfMonth && it.month == today.monthValue }
                if (todayEvents.isNotEmpty()) {
                    for (ev in todayEvents) {
                        val description = when (ev.type) {
                            EventType.BIRTHDAY -> "🎂 Today is ${ev.title}! Don't forget to send warm wishes!"
                            EventType.ANNIVERSARY -> "💍 Today is ${ev.title}! Happy Anniversary!"
                            else -> "📅 Today: ${ev.title}"
                        }
                        AppNotificationManager.showEventAlert(
                            applicationContext,
                            eventTitle = ev.title,
                            description = description,
                            eventId = ev.id
                        )
                    }
                    settingsRepo.lastEventNotifiedDate = todayDateStr
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
