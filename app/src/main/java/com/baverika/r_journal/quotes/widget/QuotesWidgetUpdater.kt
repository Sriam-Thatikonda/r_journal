package com.baverika.r_journal.quotes.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.quotes.data.QuoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper class for updating the quotes widget from anywhere in the app.
 * This provides a clean API for triggering widget updates without
 * directly accessing the widget provider.
 */
object QuotesWidgetUpdater {

    /**
     * Update all quote widgets.
     * Should be called after:
     * - Adding a new quote
     * - Editing an existing quote
     * - Deleting a quote
     * - Toggling quote active status
     * - Importing data
     * - User manually triggers refresh
     */
    fun updateWidget(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, QuotesWidgetReceiver::class.java)
            )

            // If no widgets are added, nothing to do
            if (appWidgetIds.isEmpty()) return@launch

            // Trigger update for all widget instances
            for (appWidgetId in appWidgetIds) {
                QuotesWidgetReceiver.updateQuoteWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    /**
     * Check if any quote widgets are currently added to the home screen
     */
    fun hasWidgets(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, QuotesWidgetReceiver::class.java)
        )
        return appWidgetIds.isNotEmpty()
    }

    /**
     * Get the count of active quotes (for widget setup hints)
     */
    suspend fun getActiveQuoteCount(context: Context): Int {
        val db = JournalDatabase.getDatabase(context)
        val repository = QuoteRepository(db.quoteDao())
        return repository.getActiveQuoteCount()
    }
}
