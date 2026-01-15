package com.baverika.r_journal.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for recording voice notes.
 * Handles recording, pausing, resuming, and saving.
 */
class VoiceRecorderHelper(private val context: Context) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var isRecording = false
    private var isPaused = false
    private var recordingStartTime = 0L
    private var totalDuration = 0L
    private var pauseStartTime = 0L
    
    // Callbacks
    var onRecordingComplete: ((String, Long) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    val isCurrentlyRecording: Boolean
        get() = isRecording
    
    val isCurrentlyPaused: Boolean
        get() = isPaused
    
    val currentDuration: Long
        get() {
            return if (isRecording && !isPaused) {
                totalDuration + (System.currentTimeMillis() - recordingStartTime)
            } else {
                totalDuration
            }
        }
    
    /**
     * Start a new recording
     */
    fun startRecording(): Boolean {
        return try {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.filesDir
            
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val fileName = "VOICE_${timeStamp}.m4a"
            val file = File(storageDir, fileName)
            currentFilePath = file.absolutePath
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(currentFilePath)
                prepare()
                start()
            }
            
            isRecording = true
            isPaused = false
            recordingStartTime = System.currentTimeMillis()
            totalDuration = 0L
            true
        } catch (e: Exception) {
            e.printStackTrace()
            onError?.invoke("Failed to start recording: ${e.message}")
            cleanup()
            false
        }
    }
    
    /**
     * Pause the current recording (API 24+)
     */
    fun pauseRecording(): Boolean {
        if (!isRecording || isPaused) return false
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                isPaused = true
                totalDuration += (System.currentTimeMillis() - recordingStartTime)
                pauseStartTime = System.currentTimeMillis()
                true
            } else {
                // For older APIs, stop and save recording
                stopAndSave()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onError?.invoke("Failed to pause: ${e.message}")
            false
        }
    }
    
    /**
     * Resume a paused recording (API 24+)
     */
    fun resumeRecording(): Boolean {
        if (!isRecording || !isPaused) return false
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                isPaused = false
                recordingStartTime = System.currentTimeMillis()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onError?.invoke("Failed to resume: ${e.message}")
            false
        }
    }
    
    /**
     * Stop recording and save the file
     */
    fun stopAndSave(): Boolean {
        if (!isRecording) return false
        
        return try {
            if (!isPaused) {
                totalDuration += (System.currentTimeMillis() - recordingStartTime)
            }
            
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            isPaused = false
            
            currentFilePath?.let { path ->
                val file = File(path)
                if (file.exists() && file.length() > 0) {
                    onRecordingComplete?.invoke(path, totalDuration)
                    return true
                }
            }
            
            onError?.invoke("Recording file not found or empty")
            false
        } catch (e: Exception) {
            e.printStackTrace()
            onError?.invoke("Failed to save recording: ${e.message}")
            cleanup()
            false
        }
    }
    
    /**
     * Cancel and delete the current recording
     */
    fun cancelRecording() {
        cleanup()
        currentFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        currentFilePath = null
    }
    
    /**
     * Force stop and save if recording is in progress
     * Called when app goes to background or on back press
     */
    fun forceStopAndSave() {
        if (isRecording) {
            stopAndSave()
        }
    }
    
    private fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        isRecording = false
        isPaused = false
        totalDuration = 0L
    }
    
    fun release() {
        cleanup()
    }
}

/**
 * Helper class for playing voice notes
 */
class VoicePlayerHelper {
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    
    var onCompletion: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onProgressUpdate: ((Int, Int) -> Unit)? = null // current position, total duration
    
    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true
    
    val currentPosition: Int
        get() = if (isPrepared) mediaPlayer?.currentPosition ?: 0 else 0
    
    val duration: Int
        get() = if (isPrepared) mediaPlayer?.duration ?: 0 else 0
    
    fun play(filePath: String): Boolean {
        return try {
            release()
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener {
                    onCompletion?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    onError?.invoke("Playback error")
                    false
                }
                setOnPreparedListener {
                    isPrepared = true
                    start()
                }
                prepareAsync()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            onError?.invoke("Failed to play: ${e.message}")
            false
        }
    }
    
    fun pause() {
        if (isPlaying) {
            mediaPlayer?.pause()
        }
    }
    
    fun resume() {
        if (isPrepared && !isPlaying) {
            mediaPlayer?.start()
        }
    }
    
    fun seekTo(position: Int) {
        if (isPrepared) {
            mediaPlayer?.seekTo(position)
        }
    }
    
    fun stop() {
        mediaPlayer?.stop()
        isPrepared = false
    }
    
    fun release() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        isPrepared = false
    }
}

/**
 * Format duration in milliseconds to mm:ss
 */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
