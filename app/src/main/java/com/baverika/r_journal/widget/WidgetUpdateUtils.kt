package com.baverika.r_journal.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.RemoteViews
import com.baverika.r_journal.R
import com.baverika.r_journal.repository.SettingsRepository
import com.baverika.r_journal.quotes.widget.QuotesWidgetReceiver
import com.baverika.r_journal.tasks.widget.TaskWidgetProvider
import com.baverika.r_journal.ui.challenge.widget.ChallengeWidgetProvider

object WidgetUpdateUtils {
    fun applyWidgetBackground(context: Context, views: RemoteViews, viewId: Int) {
        val settingsRepo = SettingsRepository(context)
        val opacity = settingsRepo.widgetOpacity
        val alpha = (opacity * 255) / 100
        val color = Color.argb(alpha, 0, 0, 0)
        
        // Ensure the background uses the rounded pure black drawable
        views.setInt(viewId, "setBackgroundResource", R.drawable.widget_background_pure_black)
        // Set background tint list to dynamically control transparency
        views.setColorStateList(viewId, "setBackgroundTintList", ColorStateList.valueOf(color))
    }

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // 1. Habit Widget
        val habitIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, HabitWidgetProvider::class.java)
        )
        for (id in habitIds) {
            HabitWidgetProvider.updateAppWidget(context, appWidgetManager, id)
        }

        // 2. Challenge Widget
        val challengeIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, ChallengeWidgetProvider::class.java)
        )
        for (id in challengeIds) {
            ChallengeWidgetProvider.updateAppWidget(context, appWidgetManager, id)
        }

        // 3. Task Widget
        TaskWidgetProvider.requestUpdate(context)

        // 4. Quotes Widget
        QuotesWidgetReceiver.refreshAllWidgets(context)

        // 5. Trackers Widget
        val trackerIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, TrackerWidgetProvider::class.java)
        )
        for (id in trackerIds) {
            TrackerWidgetProvider.updateAppWidget(context, appWidgetManager, id)
        }
    }
}
