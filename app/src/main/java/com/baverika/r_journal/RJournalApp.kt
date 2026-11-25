package com.baverika.r_journal

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.baverika.r_journal.quotes.widget.ScreenUnlockReceiver

class RJournalApp : Application() {
    companion object {
        lateinit var instance: RJournalApp
            private set
    }

    private val screenUnlockReceiver = ScreenUnlockReceiver()

    override fun onCreate() {
        super.onCreate()
        instance = this
        ContextCompat.registerReceiver(
            this,
            screenUnlockReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}
