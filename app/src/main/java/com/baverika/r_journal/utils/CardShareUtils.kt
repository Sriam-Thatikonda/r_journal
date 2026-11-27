package com.baverika.r_journal.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object CardShareUtils {
    const val SHARED_CARDS_DIR = "shared_cards"

    /**
     * Ensures the shared cards cache subdirectory exists and removes old exports
     * so cached share images do not accumulate.
     */
    fun prepareSharedDir(cacheDir: File): File {
        val dir = File(cacheDir, SHARED_CARDS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        } else {
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
        }
        return dir
    }

    fun createTargetPngFile(cacheDir: File, fileNamePrefix: String): File {
        val dir = prepareSharedDir(cacheDir)
        val safePrefix = fileNamePrefix.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(dir, "${safePrefix}_${System.currentTimeMillis()}.png")
    }

    suspend fun shareBitmapAsPng(
        context: Context,
        bitmap: Bitmap,
        fileNamePrefix: String,
        shareTitle: String,
        shareText: String = ""
    ) {
        val imageFile = withContext(Dispatchers.IO) {
            val outFile = createTargetPngFile(context.cacheDir, fileNamePrefix)
            FileOutputStream(outFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            outFile
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (shareText.isNotBlank()) {
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            clipData = ClipData.newRawUri(shareTitle, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, shareTitle)
        if (context !is android.app.Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            ExternalLauncherGuard.markLaunched()
            context.startActivity(chooser)
        } catch (e: Exception) {
            ExternalLauncherGuard.clear()
            Toast.makeText(context, "Unable to open share sheet", Toast.LENGTH_SHORT).show()
        }
    }
}
