package com.baverika.r_journal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.baverika.r_journal.data.model.MilestoneStats
import com.baverika.r_journal.ui.theme.JetBrainsMonoFamily
import com.baverika.r_journal.ui.theme.LocalDesignTokens
import com.baverika.r_journal.ui.theme.pressScale
import com.baverika.r_journal.utils.CardShareUtils
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MilestoneCardDialog(
    stats: MilestoneStats,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val tokens = LocalDesignTokens.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var isSharing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(tokens.sheetRadius),
            color = tokens.sheetContainer,
            border = tokens.strokeDefault,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Scrollable preview wraps the recorded GraphicsLayer so the full card
                // captures unclipped even on compact screens.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawWithContent {
                                graphicsLayer.record {
                                    this@drawWithContent.drawContent()
                                }
                                drawLayer(graphicsLayer)
                            }
                    ) {
                        MilestoneShareableCardContent(stats = stats)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Row (Excluded from exported image)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        border = tokens.strokeDefault,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .pressScale()
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = {
                            if (isSharing) return@Button
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isSharing = true
                            scope.launch {
                                try {
                                    val imageBitmap = graphicsLayer.toImageBitmap()
                                    CardShareUtils.shareBitmapAsPng(
                                        context = context,
                                        bitmap = imageBitmap.asAndroidBitmap(),
                                        fileNamePrefix = "r_journal_${stats.year}_milestone",
                                        shareTitle = "Share ${stats.year} Milestone Card",
                                        shareText = "My ${stats.year} R-Journal Milestone"
                                    )
                                } finally {
                                    isSharing = false
                                }
                            }
                        },
                        enabled = !isSharing,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .weight(1.35f)
                            .pressScale()
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Preparing...",
                                style = MaterialTheme.typography.labelLarge
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Share Card",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneShareableCardContent(stats: MilestoneStats) {
    val tokens = LocalDesignTokens.current
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val formattedWords = numberFormat.format(stats.totalWordsWritten)
    val formattedImages = numberFormat.format(stats.totalImagesAttached)
    val formattedAudio = numberFormat.format(stats.totalAudioNotesRecorded)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.cardRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, tokens.borderSubtle, RoundedCornerShape(tokens.cardRadius))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(vertical = 16.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎉 ${stats.year} Milestone Card",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your reflection story (Jan 1 – Dec 31, ${stats.year})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7 Metric Items
        Column(
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MilestoneMetricRow(
                icon = Icons.Default.Create,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Total Words Written",
                value = "$formattedWords words"
            )

            MilestoneMetricRow(
                icon = Icons.Default.Image,
                iconTint = Color(0xFF4FC3F7),
                title = "Photos & Images Saved",
                value = "$formattedImages ${if (stats.totalImagesAttached == 1) "photo" else "photos"}"
            )

            MilestoneMetricRow(
                icon = Icons.Default.Mic,
                iconTint = Color(0xFFFF8A65),
                title = "Voice Notes Recorded",
                value = "$formattedAudio ${if (stats.totalAudioNotesRecorded == 1) "voice note" else "voice notes"}"
            )

            MilestoneMetricRow(
                icon = Icons.Default.DateRange,
                iconTint = Color(0xFFFFB74D),
                title = "Most Active Writing Day",
                value = stats.mostActiveDay
            )

            MilestoneMetricRow(
                icon = Icons.Default.CheckCircle,
                iconTint = Color(0xFF81C784),
                title = "Total Tasks Completed",
                value = "${stats.totalTasksCompleted} tasks"
            )

            MilestoneMetricRow(
                icon = Icons.Default.Mood,
                iconTint = Color(0xFFBA68C8),
                title = "Top Mood of the Year",
                value = stats.topMood
            )

            MilestoneMetricRow(
                icon = Icons.Default.EventAvailable,
                iconTint = MaterialTheme.colorScheme.secondary,
                title = "Total Days Journaled",
                value = "${stats.totalDaysJournaled} / ${stats.totalDaysInPeriod} days"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Subtle Branded Watermark Footer for Shared Export
        Surface(
            shape = CircleShape,
            color = tokens.highlightFill,
            border = BorderStroke(0.75.dp, tokens.borderSubtle)
        ) {
            Text(
                text = "R-Journal • ${stats.year} Year in Review",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFamily,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MilestoneMetricRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    val tokens = LocalDesignTokens.current
    Surface(
        color = tokens.cardElevated,
        border = tokens.strokeSubtle,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconTint.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
