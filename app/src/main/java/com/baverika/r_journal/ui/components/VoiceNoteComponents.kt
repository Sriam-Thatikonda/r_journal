package com.baverika.r_journal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.utils.VoicePlayerHelper
import com.baverika.r_journal.utils.formatDuration
import kotlinx.coroutines.delay

@Composable
fun VoiceNotePlayer(
    filePath: String,
    durationMs: Long,
    modifier: Modifier = Modifier,
    isUserMessage: Boolean = true
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    
    val player = remember { VoicePlayerHelper() }
    
    DisposableEffect(filePath) {
        player.onCompletion = {
            isPlaying = false
            progress = 0f
            currentPositionMs = 0L
        }
        
        onDispose {
            player.release()
        }
    }
    
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val current = player.currentPosition
            val total = player.duration
            if (total > 0) {
                progress = current.toFloat() / total.toFloat()
                currentPositionMs = current.toLong()
            }
            delay(50)
        }
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(50),
        label = "voiceProgress"
    )
    
    val backgroundColor = if (isUserMessage) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val iconTint = if (isUserMessage) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth(0.75f)
            .clip(RoundedCornerShape(16.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        player.pause()
                        isPlaying = false
                    } else {
                        if (progress > 0f && progress < 1f) {
                            player.resume()
                        } else {
                            player.play(filePath)
                        }
                        isPlaying = true
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(iconTint.copy(alpha = 0.18f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = iconTint
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                AudioWaveformVisualizer(
                    progress = animatedProgress,
                    isPlaying = isPlaying,
                    tint = iconTint,
                    modifier = Modifier.padding(vertical = 4.dp),
                    onSeek = { fraction ->
                        val total = player.duration
                        if (total > 0) {
                            val targetPos = (fraction * total).toInt()
                            player.seekTo(targetPos)
                            progress = fraction
                            currentPositionMs = targetPos.toLong()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(if (isPlaying || currentPositionMs > 0L) currentPositionMs else 0L),
                        style = MaterialTheme.typography.labelSmall,
                        color = iconTint.copy(alpha = 0.75f)
                    )
                    Text(
                        text = formatDuration(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = iconTint.copy(alpha = 0.75f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = iconTint.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun AudioWaveformVisualizer(
    progress: Float,
    isPlaying: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 20,
    onSeek: ((Float) -> Unit)? = null
) {
    val heights = remember {
        listOf(
            0.3f, 0.6f, 0.4f, 0.8f, 0.95f, 0.5f, 0.75f, 0.3f, 0.85f, 0.6f,
            0.9f, 0.4f, 0.7f, 0.95f, 0.5f, 0.65f, 0.8f, 0.35f, 0.7f, 0.4f
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .pointerInput(onSeek) {
                if (onSeek != null) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(fraction)
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            heights.take(barCount).forEachIndexed { index, baseHeight ->
                val barProgressThreshold = index.toFloat() / barCount.toFloat()
                val isPlayed = progress >= barProgressThreshold

                val activeHeightScale by animateFloatAsState(
                    targetValue = if (isPlaying && isPlayed) (baseHeight * 1.15f).coerceAtMost(1f) else baseHeight,
                    animationSpec = tween(120),
                    label = "barHeight"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1.dp)
                        .fillMaxHeight(activeHeightScale)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isPlayed) tint else tint.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

/**
 * Recording indicator bar shown while recording in the input bar.
 */
@Composable
fun RecordingIndicator(
    durationMs: Long,
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayDuration by remember { mutableStateOf(durationMs) }

    // Update duration when recording and not paused
    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(100)
            displayDuration += 100
        }
    }

    // Sync with actual duration
    LaunchedEffect(durationMs) {
        displayDuration = durationMs
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Cancel / Delete Button
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Cancel Recording",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Blinking red dot + Duration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dotAlpha by animateFloatAsState(
                    targetValue = if (isPaused) 0.4f else 1.0f,
                    animationSpec = tween(500),
                    label = "recordingDotAlpha"
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isPaused) Color.Gray else Color.Red.copy(alpha = dotAlpha))
                )
                Text(
                    text = if (isPaused) "Paused" else "Recording...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDuration(displayDuration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Actions: Pause/Resume + Stop/Send
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pause / Resume button
                IconButton(
                    onClick = { if (isPaused) onResume() else onPause() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Stop & Send button
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Recording",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
