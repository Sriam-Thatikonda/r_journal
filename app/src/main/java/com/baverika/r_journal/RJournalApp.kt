package com.baverika.r_journal

import android.app.Application

class RJournalApp : Application() {
    companion object {
        lateinit var instance: RJournalApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
