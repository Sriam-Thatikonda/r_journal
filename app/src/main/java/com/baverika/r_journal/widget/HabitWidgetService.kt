package com.baverika.r_journal.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.baverika.r_journal.R
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.data.local.entity.Habit
import com.baverika.r_journal.repository.JournalRepository
import java.time.LocalDate
import java.time.ZoneId

class HabitWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return HabitWidgetViewsFactory(this.applicationContext)
    }
}

class HabitWidgetViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    
    private var habits: List<Pair<Habit, Boolean>> = emptyList()
    
    override fun onCreate() {
        // Initial load
        loadHabits()
    }

    override fun onDataSetChanged() {
        // Reload data when widget is updated
        loadHabits()
    }

    private fun loadHabits() {
        try {
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
            
            habits = todaysHabits.map { habit ->
                val isCompleted = logs.any { it.habitId == habit.id && it.isCompleted }
                habit to isCompleted
            }
        } catch (e: Exception) {
            habits = emptyList()
        }
    }

    override fun onDestroy() {
        habits = emptyList()
    }

    override fun getCount(): Int = habits.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_habit_item)
        
        if (position < habits.size) {
            val (habit, isCompleted) = habits[position]
            
            views.setTextViewText(R.id.habit_title, habit.title)
            views.setBoolean(R.id.habit_checkbox, "setChecked", isCompleted)
            
            // Set click intent to toggle habit
            val fillInIntent = Intent().apply {
                action = HabitWidgetProvider.ACTION_TOGGLE_HABIT
                putExtra(HabitWidgetProvider.EXTRA_HABIT_ID, habit.id)
                putExtra(HabitWidgetProvider.EXTRA_IS_COMPLETED, !isCompleted)
            }
            views.setOnClickFillInIntent(R.id.habit_checkbox, fillInIntent)
        }
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
