package com.baverika.r_journal.quotes.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.baverika.r_journal.quotes.settings.WidgetRefreshInterval
import com.baverika.r_journal.quotes.settings.WidgetSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that listens for device unlock events (ACTION_USER_PRESENT).
 * When the user unlocks their phone and the refresh setting is ON_SCREEN_UNLOCK,
 * the quote widget is automatically refreshed with a new quote.
 */
class ScreenUnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d(TAG, "Screen unlocked - checking if widget should refresh")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settingsDataStore = WidgetSettingsDataStore.getInstance(context)
                    val interval = settingsDataStore.getRefreshInterval()
                    
                    Log.d(TAG, "Current refresh interval: $interval")
                    
                    if (interval == WidgetRefreshInterval.ON_SCREEN_UNLOCK) {
                        Log.d(TAG, "Refreshing widget on screen unlock")
                        QuotesWidgetUpdater.updateWidget(context)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing widget on screen unlock", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ScreenUnlockReceiver"
    }
}
