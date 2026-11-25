package com.baverika.r_journal.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Clipboard helper that marks sensitive secrets (Android 13+) and safely clears them
 * in an app-scoped coroutine without cancelling on UI navigation or wiping unrelated copies.
 */
object ClipboardUtils {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var clearJob: Job? = null

    fun copySensitiveText(
        context: Context,
        label: String,
        secret: String,
        autoClearDelayMs: Long = 30_000L
    ) {
        val appContext = context.applicationContext
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, secret).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                description.extras = PersistableBundle().apply {
                    putBoolean("android.content.extra.IS_SENSITIVE", true)
                }
            }
        }
        clipboard.setPrimaryClip(clip)

        clearJob?.cancel()
        if (autoClearDelayMs > 0L) {
            clearJob = appScope.launch {
                delay(autoClearDelayMs)
                clearIfUnchanged(clipboard, secret)
            }
        }
    }

    private fun clearIfUnchanged(clipboard: ClipboardManager, expectedSecret: String) {
        try {
            val currentText = clipboard.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.text
                ?.toString()
            if (currentText == expectedSecret) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clipboard.clearPrimaryClip()
                } else {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }
        } catch (_: Exception) {
            // Clipboard access may be restricted in background on newer Android; ignore safely
        }
    }
}
