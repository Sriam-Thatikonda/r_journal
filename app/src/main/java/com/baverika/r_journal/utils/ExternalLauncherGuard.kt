package com.baverika.r_journal.utils

/**
 * Tracks when the app intentionally launches an external system picker/camera activity
 * so MainActivity.onStop() does not immediately re-lock mid-capture/import.
 */
object ExternalLauncherGuard {
    @Volatile
    private var awaitingExternalResult: Boolean = false

    fun markLaunched() {
        awaitingExternalResult = true
    }

    fun consumeIfAwaiting(): Boolean {
        val wasAwaiting = awaitingExternalResult
        awaitingExternalResult = false
        return wasAwaiting
    }

    fun clear() {
        awaitingExternalResult = false
    }
}
