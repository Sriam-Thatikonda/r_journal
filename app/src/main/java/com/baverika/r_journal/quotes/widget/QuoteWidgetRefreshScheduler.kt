package com.baverika.r_journal.quotes.widget

import android.content.Context
import androidx.work.*
import com.baverika.r_journal.quotes.settings.WidgetRefreshInterval
import java.util.concurrent.TimeUnit

/**
 * Scheduler for managing periodic quote widget refresh using WorkManager.
 * Handles scheduling based on user preferences.
 */
object QuoteWidgetRefreshScheduler {

    private const val WORK_NAME = "QuoteWidgetRefreshWork"

    /**
     * Schedule or reschedule the widget refresh based on the selected interval.
     * For ON_UNLOCK interval, no periodic work is scheduled (handled by MainActivity).
     */
    fun scheduleRefresh(context: Context, interval: WidgetRefreshInterval) {
        val workManager = WorkManager.getInstance(context)

        when (interval) {
            WidgetRefreshInterval.EVERY_DAY -> {
                val request = PeriodicWorkRequestBuilder<QuoteWidgetRefreshWorker>(
                    24, TimeUnit.HOURS
                )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                            .build()
                    )
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            }

            WidgetRefreshInterval.EVERY_HOUR -> {
                val request = PeriodicWorkRequestBuilder<QuoteWidgetRefreshWorker>(
                    1, TimeUnit.HOURS
                )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                            .build()
                    )
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            }

            WidgetRefreshInterval.ON_SCREEN_UNLOCK -> {
                // Cancel any existing periodic work for ON_SCREEN_UNLOCK mode
                // Refresh is triggered by ScreenUnlockReceiver when phone is unlocked
                workManager.cancelUniqueWork(WORK_NAME)
            }
        }
    }

    /**
     * Cancel all scheduled refresh work
     */
    fun cancelRefresh(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Trigger an immediate one-time refresh (for manual refresh)
     */
    fun triggerImmediateRefresh(context: Context) {
        val request = OneTimeWorkRequestBuilder<QuoteWidgetRefreshWorker>()
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
