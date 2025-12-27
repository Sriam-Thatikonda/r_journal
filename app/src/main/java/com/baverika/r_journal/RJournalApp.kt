// app/src/main/java/com/baverika/r_journal/RJournalApp.kt
package com.baverika.r_journal

import android.app.Application
import com.baverika.r_journal.data.remote.RetrofitClient
import com.baverika.r_journal.data.remote.ServerPrefs

class RJournalApp : Application() {
    companion object {
        lateinit var instance: RJournalApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val hostPort = ServerPrefs.getHostPort(this)
        RetrofitClient.setHostPort(hostPort)
    }
}
