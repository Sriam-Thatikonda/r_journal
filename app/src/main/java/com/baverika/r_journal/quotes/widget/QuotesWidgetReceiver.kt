package com.baverika.r_journal.quotes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.baverika.r_journal.MainActivity
import com.baverika.r_journal.R
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.quotes.data.QuoteRepository
import com.baverika.r_journal.quotes.settings.WidgetSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AppWidget provider for the Quotes Widget.
 * Handles widget updates and user interactions.
 */
class QuotesWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        // Update each widget instance
        for (appWidgetId in appWidgetIds) {
            updateQuoteWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive: ${intent.action}")

        when (intent.action) {
            ACTION_REFRESH_WIDGET -> {
                Log.d(TAG, "Refresh widget action received")
                // Manual refresh triggered - just refresh, don't open app
                refreshAllWidgets(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "First widget enabled, scheduling refresh")
        // Schedule the refresh worker when first widget is added
        CoroutineScope(Dispatchers.IO).launch {
            val settings = WidgetSettingsDataStore.getInstance(context)
            val interval = settings.getRefreshInterval()
            QuoteWidgetRefreshScheduler.scheduleRefresh(context, interval)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Last widget disabled, canceling refresh")
        // Cancel the worker when last widget is removed
        QuoteWidgetRefreshScheduler.cancelRefresh(context)
    }

    companion object {
        private const val TAG = "QuotesWidgetReceiver"
        const val ACTION_REFRESH_WIDGET = "com.baverika.r_journal.ACTION_REFRESH_QUOTE_WIDGET"

        /**
         * Refresh all quote widgets
         */
        fun refreshAllWidgets(context: Context) {
            Log.d(TAG, "Refreshing all widgets")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, QuotesWidgetReceiver::class.java)
            )
            Log.d(TAG, "Found ${appWidgetIds.size} widgets to refresh")

            for (appWidgetId in appWidgetIds) {
                updateQuoteWidget(context, appWidgetManager, appWidgetId)
            }
        }

        /**
         * Update a single widget with a new quote
         */
        fun updateQuoteWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            Log.d(TAG, "Updating widget $appWidgetId")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = JournalDatabase.getDatabase(context)
                    val repository = QuoteRepository(db.quoteDao())
                    val settingsDataStore = WidgetSettingsDataStore.getInstance(context)

                    // Get the last shown quote ID to avoid repetition
                    val lastShownId = settingsDataStore.getLastShownQuoteId()
                    Log.d(TAG, "Last shown quote ID: $lastShownId")

                    // Get a random quote (different from last shown)
                    val quote = repository.getRandomQuote(lastShownId)
                    Log.d(TAG, "Got quote: ${quote?.text?.take(30) ?: "null"}")

                    // Store the new quote ID
                    quote?.let {
                        settingsDataStore.setLastShownQuoteId(it.id)
                        Log.d(TAG, "Stored new quote ID: ${it.id}")
                    }

                    withContext(Dispatchers.Main) {
                        val views = RemoteViews(context.packageName, R.layout.widget_quotes)

                        if (quote != null) {
                            // Show quote
                            views.setViewVisibility(R.id.quote_content_container, View.VISIBLE)
                            views.setViewVisibility(R.id.empty_state_container, View.GONE)

                            views.setTextViewText(R.id.widget_quote_text, "\"${quote.text}\"")
                            
                            if (quote.author != null) {
                                views.setTextViewText(R.id.widget_quote_author, "— ${quote.author}")
                                views.setViewVisibility(R.id.widget_quote_author, View.VISIBLE)
                            } else {
                                views.setViewVisibility(R.id.widget_quote_author, View.GONE)
                            }
                            Log.d(TAG, "Set quote text and author")
                        } else {
                            // Show empty state
                            views.setViewVisibility(R.id.quote_content_container, View.GONE)
                            views.setViewVisibility(R.id.empty_state_container, View.VISIBLE)
                            Log.d(TAG, "Showing empty state")
                        }


                        // Set refresh button click - use different unique request code
                        val refreshIntent = Intent(context, QuotesWidgetReceiver::class.java).apply {
                            action = ACTION_REFRESH_WIDGET
                        }
                        val refreshPendingIntent = PendingIntent.getBroadcast(
                            context,
                            appWidgetId + 10000,
                            refreshIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

                        // Update the widget
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        Log.d(TAG, "Widget $appWidgetId updated successfully")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating widget $appWidgetId", e)
                }
            }
        }
    }
}
