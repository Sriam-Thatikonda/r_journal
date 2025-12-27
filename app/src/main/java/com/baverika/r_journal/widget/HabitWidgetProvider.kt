package com.baverika.r_journal.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.baverika.r_journal.MainActivity
import com.baverika.r_journal.R
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.repository.JournalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class HabitWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_TOGGLE_HABIT -> {
                val habitId = intent.getStringExtra(EXTRA_HABIT_ID)
                val isCompleted = intent.getBooleanExtra(EXTRA_IS_COMPLETED, false)
                
                if (habitId != null) {
                    toggleHabit(context, habitId, isCompleted)
                }
            }
        }
    }

    private fun toggleHabit(context: Context, habitId: String, isCompleted: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = JournalDatabase.getDatabase(context)
            val repository = JournalRepository(db.journalDao())
            
            val today = LocalDate.now()
            val dateMillis = today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            
            repository.toggleHabitCompletion(habitId, dateMillis, isCompleted)
            
            // Update widget
            withContext(Dispatchers.Main) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, HabitWidgetProvider::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_HABIT = "com.baverika.r_journal.ACTION_TOGGLE_HABIT"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_IS_COMPLETED = "is_completed"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = JournalDatabase.getDatabase(context)
                val repository = JournalRepository(db.journalDao())
                
                val today = LocalDate.now()
                val dateMillis = today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
                val dayOfWeek = today.dayOfWeek.value
                
                // Get habits for today
                val allHabits = repository.getAllActiveHabits()
                val todaysHabits = allHabits.filter { it.frequency.contains(dayOfWeek) }
                
                // Get completion status
                val logs = repository.getHabitLogsForDateSync(dateMillis)
                
                val habitsWithStatus = todaysHabits.map { habit ->
                    val isCompleted = logs.any { it.habitId == habit.id && it.isCompleted }
                    habit to isCompleted
                }
                
                val completedCount = habitsWithStatus.count { it.second }
                
                withContext(Dispatchers.Main) {
                    val views = RemoteViews(context.packageName, R.layout.widget_habit_tracker)
                    
                    // Set header
                    views.setTextViewText(R.id.widget_title, "Today's Habits")
                    views.setTextViewText(
                        R.id.widget_progress,
                        "$completedCount/${todaysHabits.size}"
                    )
                    
                    // Hide all habit containers first
                    for (i in 1..5) {
                        val containerId = context.resources.getIdentifier("habit_${i}_container", "id", context.packageName)
                        views.setViewVisibility(containerId, View.GONE)
                    }
                    
                    // Show empty state or habits
                    if (habitsWithStatus.isEmpty()) {
                        views.setViewVisibility(R.id.empty_state, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.empty_state, View.GONE)
                        
                        // Populate up to 5 habits
                        habitsWithStatus.take(5).forEachIndexed { index, (habit, isCompleted) ->
                            val i = index + 1
                            val containerId = context.resources.getIdentifier("habit_${i}_container", "id", context.packageName)
                            val checkboxId = context.resources.getIdentifier("habit_${i}_checkbox", "id", context.packageName)
                            val titleId = context.resources.getIdentifier("habit_${i}_title", "id", context.packageName)
                            
                            views.setViewVisibility(containerId, View.VISIBLE)
                            views.setTextViewText(titleId, habit.title)
                            views.setImageViewResource(
                                checkboxId,
                                if (isCompleted) R.drawable.ic_check_box else R.drawable.ic_check_box_outline_blank
                            )
                            
                            // Set click intent
                            val toggleIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                                action = ACTION_TOGGLE_HABIT
                                putExtra(EXTRA_HABIT_ID, habit.id)
                                putExtra(EXTRA_IS_COMPLETED, !isCompleted)
                            }
                            val togglePendingIntent = PendingIntent.getBroadcast(
                                context,
                                habit.id.hashCode(),
                                toggleIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(checkboxId, togglePendingIntent)
                        }
                    }
                    
                    // Click on title opens app
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
                    
                    // Update widget
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
