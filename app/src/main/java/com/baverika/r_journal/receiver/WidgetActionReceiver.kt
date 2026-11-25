package com.baverika.r_journal.receiver

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.baverika.r_journal.MainActivity
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.quotes.widget.QuotesWidgetReceiver
import com.baverika.r_journal.repository.JournalRepository
import com.baverika.r_journal.repository.TrackerRepository
import com.baverika.r_journal.tasks.widget.TaskWidgetProvider
import com.baverika.r_journal.widget.HabitWidgetProvider
import com.baverika.r_journal.widget.TrackerSingleWidgetProvider
import com.baverika.r_journal.widget.TrackerWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * Non-exported receiver for internal AppWidget click PendingIntents so third-party apps
 * cannot trigger widget mutations or launches via exported broadcast actions.
 */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            HabitWidgetProvider.ACTION_TOGGLE_HABIT -> {
                val habitId = intent.getStringExtra(HabitWidgetProvider.EXTRA_HABIT_ID) ?: return
                val isCompleted = intent.getBooleanExtra(HabitWidgetProvider.EXTRA_IS_COMPLETED, false)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = JournalDatabase.getDatabase(context)
                        val repository = JournalRepository(db.journalDao())
                        val dateMillis = LocalDate.now()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toEpochSecond() * 1000
                        repository.toggleHabitCompletion(habitId, dateMillis, isCompleted)

                        withContext(Dispatchers.Main) {
                            val appWidgetManager = AppWidgetManager.getInstance(context)
                            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                                ComponentName(context, HabitWidgetProvider::class.java)
                            )
                            for (id in appWidgetIds) {
                                HabitWidgetProvider.updateAppWidget(context, appWidgetManager, id)
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            TaskWidgetProvider.ACTION_TOGGLE_TASK -> {
                val taskId = intent.getStringExtra(TaskWidgetProvider.EXTRA_TASK_ID) ?: return
                val isCompleted = intent.getBooleanExtra(TaskWidgetProvider.EXTRA_TASK_COMPLETED, false)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = JournalDatabase.getDatabase(context)
                        db.taskDao().updateTaskCompletionSync(taskId, !isCompleted)
                        withContext(Dispatchers.Main) {
                            TaskWidgetProvider.requestUpdate(context)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            TaskWidgetProvider.ACTION_QUICK_ADD_TASK -> {
                val addIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "add_task")
                }
                context.startActivity(addIntent)
            }

            TrackerWidgetProvider.ACTION_INCREMENT_TRACKER,
            TrackerSingleWidgetProvider.ACTION_INCREMENT_SINGLE_TRACKER -> {
                val trackerId = intent.getStringExtra(TrackerWidgetProvider.EXTRA_TRACKER_ID)
                    ?: intent.getStringExtra(TrackerSingleWidgetProvider.EXTRA_TRACKER_ID)
                    ?: return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = JournalDatabase.getDatabase(context)
                        val repository = TrackerRepository(db.trackerDao())
                        val tracker = repository.getTrackerById(trackerId)
                        if (tracker != null) {
                            repository.incrementTracker(trackerId, tracker.incrementStep)
                        }

                        withContext(Dispatchers.Main) {
                            val appWidgetManager = AppWidgetManager.getInstance(context)
                            val masterIds = appWidgetManager.getAppWidgetIds(
                                ComponentName(context, TrackerWidgetProvider::class.java)
                            )
                            for (id in masterIds) {
                                TrackerWidgetProvider.updateAppWidget(context, appWidgetManager, id)
                            }
                            val singleIds = appWidgetManager.getAppWidgetIds(
                                ComponentName(context, TrackerSingleWidgetProvider::class.java)
                            )
                            for (id in singleIds) {
                                TrackerSingleWidgetProvider.updateAppWidget(context, appWidgetManager, id)
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            QuotesWidgetReceiver.ACTION_REFRESH_WIDGET -> {
                QuotesWidgetReceiver.refreshAllWidgets(context)
            }
        }
    }
}
