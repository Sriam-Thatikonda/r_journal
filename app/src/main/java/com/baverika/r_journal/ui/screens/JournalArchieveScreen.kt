package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baverika.r_journal.data.local.entity.JournalEntrySummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalArchiveScreen(
    journalRepo: com.baverika.r_journal.repository.JournalRepository,
    eventRepo: com.baverika.r_journal.repository.EventRepository,
    onEntryClick: (JournalEntrySummary) -> Unit
) {
    // ✅ Use lightweight summaries
    val allEntries by journalRepo.allEntrySummaries.collectAsState(initial = emptyList())
    val allEvents by eventRepo.allEvents.collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        if (allEntries.isEmpty()) {
            // Empty state
            com.baverika.r_journal.ui.components.EmptyState(
                icon = Icons.Default.MenuBook,
                title = "Your Journal Awaits",
                message = "Start writing your first entry by tapping the + button"
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allEntries, key = { it.id }) { entry ->
                    // Find events for this entry's date
                    val entryDate = entry.localDate
                    val dayEvents = allEvents.filter { event ->
                        event.day == entryDate.dayOfMonth && event.month == entryDate.monthValue
                    }

                    EnhancedJournalCard(
                        entry = entry,
                        events = dayEvents,
                        onClick = { onEntryClick(entry) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedJournalCard(
    entry: JournalEntrySummary,
    events: List<com.baverika.r_journal.data.local.entity.Event> = emptyList(),
    onClick: () -> Unit
) {
    val hasImages = entry.hasImages
    val imageCount = entry.imageCount
    val moodEmojis = entry.moodEmojis
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Row 1: Day and Date (Vertical stack for grid compactness)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = entry.dayOfWeek.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            text = entry.dateFormatted, // Now includes Year
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Event Indicators
                    if (events.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            events.take(3).forEach { event ->
                                val icon = when (event.type) {
                                    com.baverika.r_journal.data.local.entity.EventType.BIRTHDAY -> "🎂"
                                    com.baverika.r_journal.data.local.entity.EventType.ANNIVERSARY -> "❤️"
                                    else -> "🎉"
                                }
                                Text(text = icon, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Mood
            if (moodEmojis.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    moodEmojis.take(4).forEach { emoji -> // Limit emojis to prevent overflow
                        Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Row 3: Preview
            val previewText = entry.previewText
            Text(
                text = previewText ?: "No preview",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = if (previewText != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: Counts (SpaceBetween: Msg Left, Img Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Message Count (Left)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${entry.messageCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Image Count (Right)
                if (hasImages) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$imageCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}