package com.baverika.r_journal.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.notifications.AppNotificationManager
import com.baverika.r_journal.repository.SettingsRepository
import java.util.concurrent.TimeUnit

/**
 * Worker for scheduling and sending task reminder notifications.
 * 
 * This worker is scheduled using WorkManager and handles:
 * - Task due date reminders
 * - Overdue task notifications
 * - Periodic task summary notifications
 */
class TaskReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val CHANNEL_ID = "task_reminders"
        const val CHANNEL_NAME = "Task Reminders"
        
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_NOTIFICATION_TYPE = "notification_type"
        
        const val TYPE_REMINDER = "reminder"
        const val TYPE_OVERDUE = "overdue"
        const val TYPE_DAILY_SUMMARY = "daily_summary"
        
        /**
         * Schedule a reminder for a specific task.
         */
        fun scheduleReminder(
            context: Context,
            taskId: String,
            taskTitle: String,
            reminderTimeMillis: Long
        ) {
            val delay = reminderTimeMillis - System.currentTimeMillis()
            val initialDelay = if (delay <= 0) 0L else delay
            
            val inputData = workDataOf(
                KEY_TASK_ID to taskId,
                KEY_TASK_TITLE to taskTitle,
                KEY_NOTIFICATION_TYPE to TYPE_REMINDER
            )
            
            val reminderWork = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("task_reminder_$taskId")
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "task_reminder_$taskId",
                    ExistingWorkPolicy.REPLACE,
                    reminderWork
                )
        }
        
        /**
         * Cancel a scheduled reminder.
         */
        fun cancelReminder(context: Context, taskId: String) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("task_reminder_$taskId")
        }
        
        /**
         * Schedule daily task summary notification.
         */
        fun scheduleDailySummary(context: Context) {
            val dailySummaryWork = PeriodicWorkRequestBuilder<TaskReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInputData(workDataOf(KEY_NOTIFICATION_TYPE to TYPE_DAILY_SUMMARY))
                .addTag("daily_task_summary")
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "daily_task_summary",
                    ExistingPeriodicWorkPolicy.KEEP,
                    dailySummaryWork
                )
        }
        
        /**
         * Schedule overdue task check (runs every 6 hours).
         */
        fun scheduleOverdueCheck(context: Context) {
            val overdueWork = PeriodicWorkRequestBuilder<TaskReminderWorker>(
                6, TimeUnit.HOURS
            )
                .setInputData(workDataOf(KEY_NOTIFICATION_TYPE to TYPE_OVERDUE))
                .addTag("overdue_task_check")
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "overdue_task_check",
                    ExistingPeriodicWorkPolicy.KEEP,
                    overdueWork
                )
        }
    }
    
    override suspend fun doWork(): Result {
        AppNotificationManager.createChannels(applicationContext)
        
        // Sync upcoming special date tasks in background
        try {
            com.baverika.r_journal.utils.EventTaskGenerator.ensureEventTasksCreated(applicationContext)
        } catch (e: Exception) {
            // Non-fatal, continue with worker task
        }

        val notificationType = inputData.getString(KEY_NOTIFICATION_TYPE) ?: TYPE_REMINDER
        
        return when (notificationType) {
            TYPE_REMINDER -> handleTaskReminder()
            TYPE_OVERDUE -> handleOverdueCheck()
            TYPE_DAILY_SUMMARY -> handleDailySummary()
            else -> Result.failure()
        }
    }
    
    private suspend fun handleTaskReminder(): Result {
        val settingsRepo = SettingsRepository(applicationContext)
        if (!settingsRepo.notifTasksEnabled) return Result.success()

        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: "Task Reminder"
        
        // Verify task still exists and isn't completed
        val db = JournalDatabase.getDatabase(applicationContext)
        val task = db.taskDao().getTaskById(taskId)
        
        if (task != null && !task.isCompleted) {
            AppNotificationManager.showTaskReminder(
                context = applicationContext,
                taskId = taskId,
                taskTitle = taskTitle
            )
        }
        
        return Result.success()
    }
    
    private fun handleOverdueCheck(): Result {
        val settingsRepo = SettingsRepository(applicationContext)
        if (!settingsRepo.notifTasksEnabled) return Result.success()

        val db = JournalDatabase.getDatabase(applicationContext)
        val overdueTasks = db.taskDao().getUpcomingTasksSync(100)
            .filter { 
                it.dueDate != null && 
                it.dueDate < System.currentTimeMillis() && 
                !it.isCompleted 
            }
        
        if (overdueTasks.isNotEmpty()) {
            AppNotificationManager.showOverdueTasks(
                context = applicationContext,
                count = overdueTasks.size
            )
        }
        
        return Result.success()
    }
    
    private fun handleDailySummary(): Result {
        val settingsRepo = SettingsRepository(applicationContext)
        if (!settingsRepo.notifTaskSummaryEnabled) return Result.success()

        val db = JournalDatabase.getDatabase(applicationContext)
        val allTasks = db.taskDao().getUpcomingTasksSync(100)
        
        val todayStart = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val todayEnd = todayStart + 24 * 60 * 60 * 1000
        
        val todayTasks = allTasks.filter { task ->
            task.dueDate != null && 
            task.dueDate >= todayStart && 
            task.dueDate < todayEnd &&
            !task.isCompleted
        }
        
        if (todayTasks.isNotEmpty()) {
            AppNotificationManager.showDailyTaskSummary(
                context = applicationContext,
                count = todayTasks.size,
                sampleTitles = todayTasks.map { it.title }
            )
        }
        
        return Result.success()
    }
}
