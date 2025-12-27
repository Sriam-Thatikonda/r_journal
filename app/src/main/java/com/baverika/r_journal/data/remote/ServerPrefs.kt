// app/src/main/java/com/baverika/r_journal/data/remote/ServerPrefs.kt
package com.baverika.r_journal.data.remote

import android.content.Context

object ServerPrefs {
    private const val PREFS_NAME = "server_prefs"
    private const val KEY_HOST_PORT = "host_port"

    // default: same phone
    private const val DEFAULT_HOST_PORT = "127.0.0.1:5000"

    fun getHostPort(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_HOST_PORT, DEFAULT_HOST_PORT) ?: DEFAULT_HOST_PORT
    }

    fun setHostPort(context: Context, hostPort: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HOST_PORT, hostPort.trim()).apply()
    }
}
