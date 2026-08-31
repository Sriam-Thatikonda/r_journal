package com.baverika.r_journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.baverika.r_journal.data.model.MilestoneStats
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MilestoneCardDialog(
    stats: MilestoneStats,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
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
                            text = "🎉 1-Year Milestone Card",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your reflection story over the past 365 days",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val formattedWords = NumberFormat.getNumberInstance(Locale.getDefault()).format(stats.totalWordsWritten)
                val formattedImages = NumberFormat.getNumberInstance(Locale.getDefault()).format(stats.totalImagesAttached)
                val formattedAudio = NumberFormat.getNumberInstance(Locale.getDefault()).format(stats.totalAudioNotesRecorded)

                // 7 Metric Items
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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

                Spacer(modifier = Modifier.height(20.dp))

                // Done / Dismiss Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Close Card", fontWeight = FontWeight.SemiBold)
                }
            }
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
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconTint.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
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
