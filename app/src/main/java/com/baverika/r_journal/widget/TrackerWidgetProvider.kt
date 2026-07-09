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
import com.baverika.r_journal.repository.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackerWidgetProvider : AppWidgetProvider() {

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
        
        if (intent.action == ACTION_INCREMENT_TRACKER) {
            val trackerId = intent.getStringExtra(EXTRA_TRACKER_ID)
            if (trackerId != null) {
                incrementTracker(context, trackerId)
            }
        }
    }

    private fun incrementTracker(context: Context, trackerId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = JournalDatabase.getDatabase(context)
            val repository = TrackerRepository(db.trackerDao())
            
            val tracker = repository.getTrackerById(trackerId)
            if (tracker != null) {
                repository.incrementTracker(trackerId, tracker.incrementStep)
            }
            
            // Trigger refresh
            withContext(Dispatchers.Main) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, TrackerWidgetProvider::class.java)
                )
                for (id in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, id)
                }
            }
        }
    }

    companion object {
        const val ACTION_INCREMENT_TRACKER = "com.baverika.r_journal.ACTION_INCREMENT_TRACKER"
        const val EXTRA_TRACKER_ID = "tracker_id"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = JournalDatabase.getDatabase(context)
                val repository = TrackerRepository(db.trackerDao())
                
                // Get all trackers (non-archived)
                val trackers = repository.getAllTrackersSync()
                
                withContext(Dispatchers.Main) {
                    val views = RemoteViews(context.packageName, R.layout.widget_tracker)
                    WidgetUpdateUtils.applyWidgetBackground(context, views, R.id.widget_root)
                    
                    // Hide all tracker containers first
                    for (i in 1..4) {
                        val containerId = context.resources.getIdentifier("tracker_${i}_container", "id", context.packageName)
                        views.setViewVisibility(containerId, View.GONE)
                    }
                    
                    if (trackers.isEmpty()) {
                        views.setViewVisibility(R.id.empty_state, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.empty_state, View.GONE)
                        
                        // Show up to 4 trackers
                        trackers.take(4).forEachIndexed { index, tracker ->
                            val i = index + 1
                            val containerId = context.resources.getIdentifier("tracker_${i}_container", "id", context.packageName)
                            val addBtnId = context.resources.getIdentifier("tracker_${i}_add", "id", context.packageName)
                            val titleId = context.resources.getIdentifier("tracker_${i}_title", "id", context.packageName)
                            val progressId = context.resources.getIdentifier("tracker_${i}_progress", "id", context.packageName)
                            
                            views.setViewVisibility(containerId, View.VISIBLE)
                            views.setTextViewText(titleId, "${tracker.emoji} ${tracker.title}")
                            views.setTextViewText(progressId, "${tracker.currentCount} / ${tracker.goal}")
                            
                            // Pending Intent for Increment Button
                            val incrementIntent = Intent(context, TrackerWidgetProvider::class.java).apply {
                                action = ACTION_INCREMENT_TRACKER
                                putExtra(EXTRA_TRACKER_ID, tracker.id)
                            }
                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                tracker.id.hashCode(),
                                incrementIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(addBtnId, pendingIntent)
                        }
                    }
                    
                    // Root click opens trackers list in main app
                    val rootIntent = Intent(context, MainActivity::class.java).apply {
                        putExtra("navigate_to", "trackers")
                    }
                    val rootPendingIntent = PendingIntent.getActivity(
                        context,
                        99,
                        rootIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_title, rootPendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_root, rootPendingIntent)
                    
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
