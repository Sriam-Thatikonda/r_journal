package com.baverika.r_journal.quotes.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * WorkManager worker for refreshing the quote widget periodically.
 * This worker is scheduled based on user preferences (every day, every hour).
 */
class QuoteWidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Refreshing quote widget...")
            
            // Update all quote widgets
            QuotesWidgetUpdater.updateWidget(applicationContext)
            
            Log.d(TAG, "Quote widget refresh completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Quote widget refresh failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "QuoteWidgetRefreshWorker"
    }
}
