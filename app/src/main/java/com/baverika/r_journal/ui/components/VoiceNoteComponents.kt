package com.baverika.r_journal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.utils.VoicePlayerHelper
import com.baverika.r_journal.utils.formatDuration
import kotlinx.coroutines.delay
import java.io.File

/**
 * Voice note player widget for chat bubbles
 */
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
    
    // Progress update loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val current = player.currentPosition
            val total = player.duration
            if (total > 0) {
                progress = current.toFloat() / total.toFloat()
                currentPositionMs = current.toLong()
            }
            delay(100)
        }
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(100),
        label = "progress"
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
            .fillMaxWidth(0.7f)
            .clip(RoundedCornerShape(16.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause button
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
                    .background(iconTint.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = iconTint
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Waveform progress visualizer
            Column(modifier = Modifier.weight(1f)) {
                AudioWaveformVisualizer(
                    progress = animatedProgress,
                    isPlaying = isPlaying,
                    tint = iconTint,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(if (isPlaying) currentPositionMs else 0L),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Mic icon indicator
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice note",
                modifier = Modifier.size(16.dp),
                tint = iconTint.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Recording indicator bar shown while recording
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
    
    // Update duration every 100ms when not paused
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
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Recording indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPaused) Color.Gray else Color.Red
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPaused) "Paused" else "Recording",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatDuration(displayDuration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            // Controls
            Row {
                // Pause/Resume
                IconButton(
                    onClick = { if (isPaused) onResume() else onPause() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                // Stop and send
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop and Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * Animated voice waveform visualizer for voice notes
 */
@Composable
fun AudioWaveformVisualizer(
    progress: Float,
    isPlaying: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 20
) {
    val heights = remember {
        listOf(
            0.3f, 0.6f, 0.4f, 0.8f, 0.95f, 0.5f, 0.75f, 0.3f, 0.85f, 0.6f,
            0.9f, 0.4f, 0.7f, 0.95f, 0.5f, 0.65f, 0.8f, 0.35f, 0.7f, 0.4f
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.take(barCount).forEachIndexed { index, baseHeight ->
            val barProgressThreshold = index.toFloat() / barCount.toFloat()
            val isPlayed = progress >= barProgressThreshold

            val activeHeightScale by animateFloatAsState(
                targetValue = if (isPlaying && isPlayed) (baseHeight * 1.1f).coerceAtMost(1f) else baseHeight,
                animationSpec = tween(150),
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
