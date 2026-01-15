package com.baverika.r_journal.tasks.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.baverika.r_journal.MainActivity
import com.baverika.r_journal.R
import com.baverika.r_journal.data.local.database.JournalDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * App Widget Provider for displaying upcoming tasks on the home screen.
 */
class TaskWidgetProvider : AppWidgetProvider() {
    
    companion object {
        const val ACTION_TOGGLE_TASK = "com.baverika.r_journal.ACTION_TOGGLE_TASK"
        const val ACTION_QUICK_ADD_TASK = "com.baverika.r_journal.ACTION_QUICK_ADD_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_COMPLETED = "extra_task_completed"
        
        fun requestUpdate(context: Context) {
            val intent = Intent(context, TaskWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TaskWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.sendBroadcast(intent)
        }
    }
    
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
            ACTION_TOGGLE_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                val isCompleted = intent.getBooleanExtra(EXTRA_TASK_COMPLETED, false) // This is the OLD state
                
                if (taskId != null) {
                    val pendingResult = goAsync()
                    val db = JournalDatabase.getDatabase(context)
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Toggle state: !isCompleted
                            db.taskDao().updateTaskCompletionSync(taskId, !isCompleted)
                            
                            withContext(Dispatchers.Main) {
                                requestUpdate(context)
                                pendingResult.finish()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_QUICK_ADD_TASK -> {
                val addIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "add_task")
                }
                context.startActivity(addIntent)
            }
        }
    }
    
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_tasks)
        
        // Set up list adapter
        val serviceIntent = Intent(context, TaskWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.task_widget_list, serviceIntent)
        views.setEmptyView(R.id.task_widget_list, R.id.task_widget_empty)
        
        // Click template for list items
        val toggleIntent = Intent(context, TaskWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_TASK
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.task_widget_list, togglePendingIntent)
        
        // Open App Click
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "tasks")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.task_widget_title, openAppPendingIntent)
        
        // Quick Add Click
        val quickAddIntent = Intent(context, TaskWidgetProvider::class.java).apply {
            action = ACTION_QUICK_ADD_TASK
        }
        val quickAddPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            quickAddIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.task_widget_add_button, quickAddPendingIntent)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.task_widget_list)
    }
}
