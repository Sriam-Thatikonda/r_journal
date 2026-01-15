package com.baverika.r_journal.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.baverika.r_journal.MainActivity
import com.baverika.r_journal.R
import com.baverika.r_journal.data.local.database.JournalDatabase
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
            if (delay <= 0) return // Don't schedule past reminders
            
            val inputData = workDataOf(
                KEY_TASK_ID to taskId,
                KEY_TASK_TITLE to taskTitle,
                KEY_NOTIFICATION_TYPE to TYPE_REMINDER
            )
            
            val reminderWork = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
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
        createNotificationChannel()
        
        val notificationType = inputData.getString(KEY_NOTIFICATION_TYPE) ?: TYPE_REMINDER
        
        return when (notificationType) {
            TYPE_REMINDER -> handleTaskReminder()
            TYPE_OVERDUE -> handleOverdueCheck()
            TYPE_DAILY_SUMMARY -> handleDailySummary()
            else -> Result.failure()
        }
    }
    
    private fun handleTaskReminder(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: "Task Reminder"
        
        // Verify task still exists and isn't completed
        val db = JournalDatabase.getDatabase(applicationContext)
        val task = db.taskDao().getUpcomingTasksSync(100).find { it.id == taskId }
        
        if (task != null && !task.isCompleted) {
            showNotification(
                id = taskId.hashCode(),
                title = "Task Reminder",
                message = taskTitle,
                taskId = taskId
            )
        }
        
        return Result.success()
    }
    
    private fun handleOverdueCheck(): Result {
        val db = JournalDatabase.getDatabase(applicationContext)
        val overdueTasks = db.taskDao().getUpcomingTasksSync(100)
            .filter { 
                it.dueDate != null && 
                it.dueDate < System.currentTimeMillis() && 
                !it.isCompleted 
            }
        
        if (overdueTasks.isNotEmpty()) {
            val count = overdueTasks.size
            showNotification(
                id = "overdue_tasks".hashCode(),
                title = "Overdue Tasks",
                message = "You have $count overdue task${if (count > 1) "s" else ""}",
                taskId = null
            )
        }
        
        return Result.success()
    }
    
    private fun handleDailySummary(): Result {
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
            val count = todayTasks.size
            showNotification(
                id = "daily_summary".hashCode(),
                title = "Today's Tasks",
                message = "You have $count task${if (count > 1) "s" else ""} due today",
                taskId = null
            )
        }
        
        return Result.success()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Task reminder notifications"
            }
            
            val notificationManager = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showNotification(
        id: Int,
        title: String,
        message: String,
        taskId: String?
    ) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", if (taskId != null) "edit_task/$taskId" else "tasks")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_task_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, notification)
    }
}
