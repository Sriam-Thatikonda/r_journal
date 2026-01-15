package com.baverika.r_journal.tasks.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.baverika.r_journal.R
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.data.local.entity.Task
import com.baverika.r_journal.data.local.entity.TaskPriority
import java.text.SimpleDateFormat
import java.util.*

/**
 * Remote Views Service for the Task Widget.
 * 
 * Provides the data and views for the task list displayed in the widget.
 */
class TaskWidgetService : RemoteViewsService() {
    
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TaskWidgetRemoteViewsFactory(applicationContext)
    }
}

/**
 * Remote Views Factory that creates the individual task item views.
 */
class TaskWidgetRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
    
    private var tasks: List<Task> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    
    override fun onCreate() {
        loadTasks()
    }
    
    override fun onDataSetChanged() {
        loadTasks()
    }
    
    override fun onDestroy() {
        tasks = emptyList()
    }
    
    override fun getCount(): Int = tasks.size
    
    override fun getViewAt(position: Int): RemoteViews {
        if (position >= tasks.size) {
            return RemoteViews(context.packageName, R.layout.widget_task_item)
        }
        
        val task = tasks[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)
        
        // Set task title
        views.setTextViewText(R.id.widget_task_title, task.title)
        
        // Set strikethrough for completed tasks
        if (task.isCompleted) {
            views.setInt(R.id.widget_task_title, "setPaintFlags", 
                android.graphics.Paint.STRIKE_THRU_TEXT_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG)
            // Keep text white but maybe slightly fast/dim or just white with strike
            views.setTextColor(R.id.widget_task_title, Color.parseColor("#E0E0E0")) 
        } else {
            views.setInt(R.id.widget_task_title, "setPaintFlags", 
                android.graphics.Paint.ANTI_ALIAS_FLAG)
            views.setTextColor(R.id.widget_task_title, Color.WHITE)
        }
        
        // Set due date
        val dueDateText = formatDueDate(task.dueDate)
        views.setTextViewText(R.id.widget_task_due_date, dueDateText)
        
        // Set due date color based on overdue status
        val isOverdue = task.dueDate != null && 
                        task.dueDate < System.currentTimeMillis() && 
                        !task.isCompleted
        views.setTextColor(
            R.id.widget_task_due_date,
            if (isOverdue) Color.parseColor("#E53935") else Color.parseColor("#B3FFFFFF")
        )
        
        // Set priority indicator color using drawable resources
        val priorityDrawable = when (task.priority) {
            TaskPriority.HIGH -> R.drawable.widget_priority_high
            TaskPriority.MEDIUM -> R.drawable.widget_priority_medium
            TaskPriority.LOW -> R.drawable.widget_priority_low
        }
        views.setImageViewResource(R.id.widget_task_priority_indicator, priorityDrawable)
        
        // Set checkbox state
        views.setImageViewResource(
            R.id.widget_task_checkbox,
            if (task.isCompleted) {
                R.drawable.ic_checkbox_checked
            } else {
                R.drawable.ic_checkbox_unchecked
            }
        )
        
        // Set fill-in intent for click handling
        val fillInIntent = Intent().apply {
            putExtra(TaskWidgetProvider.EXTRA_TASK_ID, task.id)
            putExtra(TaskWidgetProvider.EXTRA_TASK_COMPLETED, task.isCompleted)
        }
        views.setOnClickFillInIntent(R.id.widget_task_item_container, fillInIntent)
        
        return views
    }
    
    override fun getLoadingView(): RemoteViews? = null
    
    override fun getViewTypeCount(): Int = 1
    
    override fun getItemId(position: Int): Long = position.toLong()
    
    override fun hasStableIds(): Boolean = true
    
    private fun loadTasks() {
        try {
            val db = JournalDatabase.getDatabase(context)
            tasks = db.taskDao().getUpcomingTasksSync(7)
        } catch (e: Exception) {
            e.printStackTrace()
            tasks = emptyList()
        }
    }
    
    private fun formatDueDate(dueDate: Long?): String {
        if (dueDate == null) return "No due date"
        
        val now = Calendar.getInstance()
        val due = Calendar.getInstance().apply { timeInMillis = dueDate }
        
        return when {
            isSameDay(now, due) -> "Today, ${timeFormat.format(Date(dueDate))}"
            isSameDay(now.apply { add(Calendar.DAY_OF_YEAR, 1) }, due) -> "Tomorrow"
            else -> dateFormat.format(Date(dueDate))
        }
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
