package com.baverika.r_journal.ui.challenge.widget

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
import com.baverika.r_journal.repository.ChallengeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.baverika.r_journal.widget.WidgetUpdateUtils

class ChallengeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = JournalDatabase.getDatabase(context)
                val repository = ChallengeRepository(db.challengeDao())
                
                val widgetDataList = repository.getWidgetData().firstOrNull() ?: emptyList()
                
                withContext(Dispatchers.Main) {
                    val views = RemoteViews(context.packageName, R.layout.widget_challenge_tracker)
                    WidgetUpdateUtils.applyWidgetBackground(context, views, R.id.widget_root)
                    
                    // Hide all challenge containers first
                    for (i in 1..3) {
                        val containerId = context.resources.getIdentifier("challenge_${i}_container", "id", context.packageName)
                        views.setViewVisibility(containerId, View.GONE)
                    }
                    
                    if (widgetDataList.isEmpty()) {
                        views.setViewVisibility(R.id.empty_state, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.empty_state, View.GONE)
                        
                        widgetDataList.take(3).forEachIndexed { index, data ->
                            val i = index + 1
                            val containerId = context.resources.getIdentifier("challenge_${i}_container", "id", context.packageName)
                            val titleId = context.resources.getIdentifier("challenge_${i}_title", "id", context.packageName)
                            val progressTextId = context.resources.getIdentifier("challenge_${i}_progress_text", "id", context.packageName)
                            val progressBarId = context.resources.getIdentifier("challenge_${i}_progress_bar", "id", context.packageName)
                            
                            views.setViewVisibility(containerId, View.VISIBLE)
                            views.setTextViewText(titleId, "${data.emoji ?: "🎯"} ${data.name}")
                            views.setTextViewText(progressTextId, "${data.completedDays} / ${data.totalDays} Days")
                            views.setProgressBar(progressBarId, 100, (data.progressPercentage * 100).toInt(), false)
                        }
                    }
                    
                    // Click on the widget opens the challenge tracker in the app
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra("navigate_to", "challenges")
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
                    
                    // Set root click listener
                    val rootIntent = Intent(context, MainActivity::class.java).apply {
                        putExtra("navigate_to", "challenges")
                    }
                    val rootPendingIntent = PendingIntent.getActivity(
                        context, 1, rootIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, rootPendingIntent)
                    
                    // Update widget
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
