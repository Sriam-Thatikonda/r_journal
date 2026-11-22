package com.baverika.r_journal.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

enum class BreathingPhase(val label: String, val instruction: String, val durationSec: Int) {
    INHALE("Inhale", "Breathe in deeply through your nose", 4),
    HOLD("Hold", "Gently hold your breath and pause", 4),
    EXHALE("Exhale", "Release slowly and completely through your mouth", 6),
    REST("Rest", "Notice the sensation of calm", 2)
}

@Composable
fun UrgeSurfingDialog(
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var totalSecondsRemaining by remember { mutableIntStateOf(60) }
    var currentPhase by remember { mutableStateOf(BreathingPhase.INHALE) }
    var phaseSecondsRemaining by remember { mutableIntStateOf(BreathingPhase.INHALE.durationSec) }
    var isCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (totalSecondsRemaining > 0) {
            delay(1000L)
            totalSecondsRemaining--
            phaseSecondsRemaining--

            if (phaseSecondsRemaining <= 0) {
                currentPhase = when (currentPhase) {
                    BreathingPhase.INHALE -> BreathingPhase.HOLD
                    BreathingPhase.HOLD -> BreathingPhase.EXHALE
                    BreathingPhase.EXHALE -> BreathingPhase.REST
                    BreathingPhase.REST -> BreathingPhase.INHALE
                }
                phaseSecondsRemaining = currentPhase.durationSec
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } catch (_: Exception) {}
            }
        }
        isCompleted = true
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
    }

    // Animation for breathing circle scale
    val targetScale = when (currentPhase) {
        BreathingPhase.INHALE -> 1.25f
        BreathingPhase.HOLD -> 1.25f
        BreathingPhase.EXHALE -> 0.75f
        BreathingPhase.REST -> 0.75f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = currentPhase.durationSec * 1000,
            easing = FastOutSlowInEasing
        ),
        label = "breathingScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Urge Surfing",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ride the craving wave (60s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                if (!isCompleted) {
                    // Progress bar
                    LinearProgressIndicator(
                        progress = { (60 - totalSecondsRemaining).toFloat() / 60f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // Animated Breathing Visual
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer pulsating aura ring
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .scale(animatedScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Core Circle
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .scale(animatedScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentPhase.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Phase instruction
                    Text(
                        text = currentPhase.instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Text(
                        text = "${totalSecondsRemaining}s remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    // Completion celebration state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 20.dp)
                    ) {
                        Text(text = "🌊", fontSize = 48.sp)
                        Text(
                            text = "You Surfed the Urge!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Craving impulses typically fade after 60 seconds of focused deep breathing. You are in control.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (isCompleted) "Finish & Return" else "I'm Centered Now")
                }
            }
        }
    }
}
