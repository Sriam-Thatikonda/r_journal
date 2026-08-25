package com.baverika.r_journal.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.baverika.r_journal.R
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.repository.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackerSingleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            TrackerWidgetConfigureActivity.deleteTrackerPref(context, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_INCREMENT_SINGLE_TRACKER) {
            val trackerId = intent.getStringExtra(EXTRA_TRACKER_ID)
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

            if (trackerId != null) {
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

                            // Refresh all single tracker widgets
                            val singleWidgetIds = appWidgetManager.getAppWidgetIds(
                                ComponentName(context, TrackerSingleWidgetProvider::class.java)
                            )
                            for (id in singleWidgetIds) {
                                updateAppWidget(context, appWidgetManager, id)
                            }

                            // Refresh master list tracker widget as well
                            val masterWidgetIds = appWidgetManager.getAppWidgetIds(
                                ComponentName(context, TrackerWidgetProvider::class.java)
                            )
                            for (id in masterWidgetIds) {
                                TrackerWidgetProvider.updateAppWidget(context, appWidgetManager, id)
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_INCREMENT_SINGLE_TRACKER = "com.baverika.r_journal.ACTION_INCREMENT_SINGLE_TRACKER"
        const val EXTRA_TRACKER_ID = "single_tracker_id"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val trackerId = TrackerWidgetConfigureActivity.loadTrackerPref(context, appWidgetId)

                val db = JournalDatabase.getDatabase(context)
                val repository = TrackerRepository(db.trackerDao())

                val tracker = trackerId?.let { repository.getTrackerById(it) }

                withContext(Dispatchers.Main) {
                    val views = RemoteViews(context.packageName, R.layout.widget_single_tracker)
                    WidgetUpdateUtils.applyWidgetBackground(context, views, R.id.widget_root)

                    if (tracker == null) {
                        views.setTextViewText(R.id.tracker_title, "Select Tracker")
                        views.setTextViewText(R.id.tracker_count_goal, "Tap to setup")
                    } else {
                        views.setTextViewText(R.id.tracker_title, "${tracker.emoji} ${tracker.title}")
                        views.setTextViewText(R.id.tracker_count_goal, "${tracker.currentCount} / ${tracker.goal}")

                        // Unique request code per appWidgetId so PendingIntents don't collide
                        val requestCode = appWidgetId + 20000
                        val incrementIntent = Intent(context, TrackerSingleWidgetProvider::class.java).apply {
                            action = ACTION_INCREMENT_SINGLE_TRACKER
                            putExtra(EXTRA_TRACKER_ID, tracker.id)
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            incrementIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        // Attach pending intent to ENTIRE widget root so tapping anywhere increments
                        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
