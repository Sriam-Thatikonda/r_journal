package com.baverika.r_journal.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.tasks.widget.TaskWidgetProvider
import com.baverika.r_journal.worker.TaskReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles interactive action clicks from notifications (e.g. "Mark Done", "Snooze 1h").
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_MARK_DONE = "com.baverika.r_journal.ACTION_TASK_MARK_DONE"
        const val ACTION_TASK_SNOOZE = "com.baverika.r_journal.ACTION_TASK_SNOOZE"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, taskId.hashCode())
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            ACTION_TASK_MARK_DONE -> {
                notificationManager.cancel(notifId)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = JournalDatabase.getDatabase(context)
                        db.taskDao().updateTaskCompletionSync(taskId, true)
                        TaskWidgetProvider.requestUpdate(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_TASK_SNOOZE -> {
                notificationManager.cancel(notifId)
                val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
                val snoozeTime = System.currentTimeMillis() + 60 * 60 * 1000L // 1 hour later
                TaskReminderWorker.scheduleReminder(
                    context = context,
                    taskId = taskId,
                    taskTitle = taskTitle,
                    reminderTimeMillis = snoozeTime
                )
            }
        }
    }
}
