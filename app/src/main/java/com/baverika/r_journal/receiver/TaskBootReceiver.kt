package com.baverika.r_journal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.worker.DailyNudgeWorker
import com.baverika.r_journal.worker.TaskReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            TaskReminderWorker.scheduleDailySummary(context)
            DailyNudgeWorker.schedule(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    try {
                        com.baverika.r_journal.utils.EventTaskGenerator.ensureEventTasksCreated(context)
                    } catch (e: Exception) {
                        // Non-fatal
                    }
                    val db = JournalDatabase.getDatabase(context)
                    val activeTasks = db.taskDao().getActiveTasksWithFutureReminders(System.currentTimeMillis())
                    for (task in activeTasks) {
                        val reminderTime = task.reminderTime ?: continue
                        TaskReminderWorker.scheduleReminder(
                            context = context,
                            taskId = task.id,
                            taskTitle = task.title,
                            reminderTimeMillis = reminderTime
                        )
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
