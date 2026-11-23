package com.baverika.r_journal.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.baverika.r_journal.MainActivity
import com.baverika.r_journal.R
import com.baverika.r_journal.receiver.NotificationActionReceiver

object AppNotificationManager {

    const val CHANNEL_TASKS = "task_reminders"
    const val CHANNEL_HABITS = "habit_reminders"
    const val CHANNEL_BACKUP = "backup_reminders"
    const val CHANNEL_JOURNAL = "journal_reminders"
    const val CHANNEL_EVENTS = "event_reminders"

    const val NOTIF_ID_DAILY_TASK_SUMMARY = 1001
    const val NOTIF_ID_OVERDUE_TASKS = 1002
    const val NOTIF_ID_HABIT_REVIEW = 2001
    const val NOTIF_ID_BACKUP_REMINDER = 3001
    const val NOTIF_ID_JOURNAL_PROMPT = 4001
    const val NOTIF_ID_EVENT_PREFIX = 5000

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val tasksChannel = NotificationChannel(
                CHANNEL_TASKS,
                "Tasks & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Task due dates, alarms, and daily task briefings"
                enableVibration(true)
            }

            val habitsChannel = NotificationChannel(
                CHANNEL_HABITS,
                "Habits & Streaks",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Evening habit check-ins and streak preservation alerts"
            }

            val backupChannel = NotificationChannel(
                CHANNEL_BACKUP,
                "Backup & Data Safety",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic reminders to back up and protect your data"
            }

            val journalChannel = NotificationChannel(
                CHANNEL_JOURNAL,
                "Daily Journal & Reflection",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Evening reflection prompts and journaling reminders"
            }

            val eventsChannel = NotificationChannel(
                CHANNEL_EVENTS,
                "Events & Celebrations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Birthday, anniversary, and special event alerts"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(tasksChannel, habitsChannel, backupChannel, journalChannel, eventsChannel)
            )
        }
    }

    private fun createNavPendingIntent(context: Context, route: String, reqCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", route)
        }
        return PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // --- 1. TASKS ---

    fun showTaskReminder(
        context: Context,
        taskId: String,
        taskTitle: String
    ) {
        createChannels(context)
        val notifId = taskId.hashCode()
        val contentIntent = createNavPendingIntent(context, "edit_task/$taskId", notifId)

        // Mark Done Action
        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TASK_MARK_DONE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId * 2 + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 1h Action
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TASK_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationActionReceiver.EXTRA_TASK_TITLE, taskTitle)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId * 2 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.drawable.ic_task_notification)
            .setContentTitle("Task Reminder")
            .setContentText(taskTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(taskTitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_task_notification, "Mark Done", donePendingIntent)
            .addAction(R.drawable.ic_task_notification, "Snooze 1h", snoozePendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notifId, notification)
    }

    fun showDailyTaskSummary(context: Context, count: Int, sampleTitles: List<String>) {
        createChannels(context)
        val contentIntent = createNavPendingIntent(context, "tasks", NOTIF_ID_DAILY_TASK_SUMMARY)
        val bigText = if (sampleTitles.isNotEmpty()) {
            "You have $count task${if (count > 1) "s" else ""} due today:\n• " + sampleTitles.take(3).joinToString("\n• ")
        } else {
            "You have $count task${if (count > 1) "s" else ""} due today"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.drawable.ic_task_notification)
            .setContentTitle("Today's Tasks")
            .setContentText("You have $count task${if (count > 1) "s" else ""} scheduled for today")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID_DAILY_TASK_SUMMARY, notification)
    }

    fun showOverdueTasks(context: Context, count: Int) {
        createChannels(context)
        val contentIntent = createNavPendingIntent(context, "tasks", NOTIF_ID_OVERDUE_TASKS)

        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.drawable.ic_task_notification)
            .setContentTitle("Overdue Tasks")
            .setContentText("You have $count overdue task${if (count > 1) "s" else ""}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID_OVERDUE_TASKS, notification)
    }

    // --- 2. HABITS ---

    fun showHabitReview(context: Context, pendingCount: Int, pendingHabitNames: String) {
        createChannels(context)
        val contentIntent = createNavPendingIntent(context, "habits", NOTIF_ID_HABIT_REVIEW)

        val title = "🔥 Keep Your Habit Streak Alive!"
        val text = "You have $pendingCount habit${if (pendingCount > 1) "s" else ""} pending today: $pendingHabitNames. Check them off before midnight!"

        val notification = NotificationCompat.Builder(context, CHANNEL_HABITS)
            .setSmallIcon(R.drawable.ic_task_notification)
            .setContentTitle(title)
            .setContentText("You have $pendingCount habit${if (pendingCount > 1) "s" else ""} remaining today")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID_HABIT_REVIEW, notification)
    }

    // --- 3. BACKUP ---

    fun showBackupReminder(context: Context, daysSince: Int) {
        createChannels(context)
        val contentIntent = createNavPendingIntent(context, "export", NOTIF_ID_BACKUP_REMINDER)

        val title = "🛡️ Backup & Protect Your Journal"
        val message = "It has been $daysSince days since your last backup. Tap to export your backup ZIP and safeguard your memories."

        val exportActionIntent = createNavPendingIntent(context, "export", NOTIF_ID_BACKUP_REMINDER + 1)

        val notification = NotificationCompat.Builder(context, CHANNEL_BACKUP)
            .setSmallIcon(R.drawable.ic_task_notification)
            .setContentTitle(title)
            .setContentText("Last backup was $daysSince days ago")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_task_notification, "Back Up Now", exportActionIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID_BACKUP_REMINDER, notification)
    }

    // --- 4. DAILY JOURNAL REFLECTION ---

    fun showJournalPrompt(context: Context) {
        createChannels(context)
        val contentIntent = createNavPendingIntent(context, "chat_input", NOTIF_ID_JOURNAL_PROMPT)

        val title = "✍️ Daily Reflection"
        val message = "How was your day? Take 2 minutes to record your thoughts, feelings, or highlights."

        val notification = NotificationCompat.Builder(context, CHANNEL_JOURNAL)
            .setSmallIcon(R.drawable.ic_task_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID_JOURNAL_PROMPT, notification)
    }

    // --- 5. EVENTS & BIRTHDAYS ---

    fun showEventAlert(context: Context, eventTitle: String, description: String, eventId: String) {
        createChannels(context)
        val notifId = NOTIF_ID_EVENT_PREFIX + Math.abs(eventId.hashCode() % 1000)
        val contentIntent = createNavPendingIntent(context, "events", notifId)

        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_task_notification)
            .setContentTitle(eventTitle)
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notifId, notification)
    }
}
