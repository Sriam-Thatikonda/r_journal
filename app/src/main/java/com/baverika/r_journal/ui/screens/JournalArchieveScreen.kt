package com.baverika.r_journal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baverika.r_journal.R
import com.baverika.r_journal.data.local.entity.Event
import com.baverika.r_journal.data.local.entity.EventType
import com.baverika.r_journal.data.local.entity.JournalEntrySummary
import com.baverika.r_journal.repository.EventRepository
import com.baverika.r_journal.repository.JournalRepository
import com.baverika.r_journal.ui.components.EmptyState
import com.baverika.r_journal.ui.components.HighlightedText
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.LocalAppTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ArchiveViewMode {
    GRID,
    LIST,
    TIMELINE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalArchiveScreen(
    journalRepo: JournalRepository,
    eventRepo: EventRepository,
    onEntryClick: (JournalEntrySummary) -> Unit,
    searchQuery: String = ""
) {
    val allEntries by journalRepo.allEntrySummaries.collectAsState(initial = emptyList())
    val allEvents by eventRepo.allEvents.collectAsState(initial = emptyList())
    val haptic = LocalHapticFeedback.current

    val isBlueSky = LocalAppTheme.current == AppTheme.BLUE_SKY
    val isSpace = LocalAppTheme.current == AppTheme.ROSEWOOD

    var viewMode by rememberSaveable { mutableStateOf(ArchiveViewMode.GRID) }

    val filteredEntries = remember(allEntries, searchQuery) {
        if (searchQuery.isBlank()) allEntries
        else {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            val terms = q.split("\\s+".toRegex()).filter { it.isNotBlank() }
            allEntries.filter { entry ->
                val fullText = entry.searchableText.lowercase(Locale.getDefault())
                val dateStr = entry.dateFormatted.lowercase(Locale.getDefault())
                val dayStr = entry.dayOfWeek.lowercase(Locale.getDefault())
                val previewStr = entry.previewText?.lowercase(Locale.getDefault()) ?: ""
                val moodStr = entry.mood?.lowercase(Locale.getDefault()) ?: ""
                val tagsStr = entry.tags.map { it.lowercase(Locale.getDefault()) }

                // 1. Whole query match against any message, tag, mood, or date
                fullText.contains(q) ||
                dateStr.contains(q) ||
                dayStr.contains(q) ||
                previewStr.contains(q) ||
                moodStr.contains(q) ||
                tagsStr.any { it.contains(q) } ||
                entry.moodEmojis.any { it.contains(q) } ||
                // 2. Multi-term match (every word appears somewhere in the entry)
                (terms.isNotEmpty() && terms.all { term ->
                    fullText.contains(term) ||
                    dateStr.contains(term) ||
                    dayStr.contains(term) ||
                    previewStr.contains(term) ||
                    moodStr.contains(term) ||
                    tagsStr.any { it.contains(term) } ||
                    entry.moodEmojis.any { it.contains(term) }
                })
            }
        }
    }

    // Group entries chronologically by Month & Year (preserving descending order)
    val groupedEntries = remember(filteredEntries) {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        val map = linkedMapOf<String, MutableList<JournalEntrySummary>>()
        for (entry in filteredEntries) {
            val key = entry.localDate.format(formatter)
            map.getOrPut(key) { mutableListOf() }.add(entry)
        }
        map
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isBlueSky) {
            Image(
                painter = painterResource(id = R.drawable.bg_journal_archive),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(6.dp),
                contentScale = ContentScale.Crop
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (allEntries.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Your Journal Awaits",
                    message = "Start writing your first entry by tapping the + button"
                )
            } else if (filteredEntries.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No Entries Found",
                    message = "No journal entries matched \"$searchQuery\""
                )
            } else {
                // Header Controls Bar: Result count & View Mode Switcher
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "Found ${filteredEntries.size} of ${allEntries.size} entries"
                            } else {
                                "${filteredEntries.size} entries recorded"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isBlueSky) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // View mode switcher segmented pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSpace) Color(0x33FFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ViewModeIconButton(
                                    icon = Icons.Default.GridView,
                                    description = "Grid view",
                                    selected = viewMode == ArchiveViewMode.GRID,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewMode = ArchiveViewMode.GRID
                                    }
                                )
                                ViewModeIconButton(
                                    icon = Icons.AutoMirrored.Filled.ViewList,
                                    description = "List view",
                                    selected = viewMode == ArchiveViewMode.LIST,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewMode = ArchiveViewMode.LIST
                                    }
                                )
                                ViewModeIconButton(
                                    icon = Icons.Default.Timeline,
                                    description = "Timeline view",
                                    selected = viewMode == ArchiveViewMode.TIMELINE,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewMode = ArchiveViewMode.TIMELINE
                                    }
                                )
                            }
                        }
                    }
                }

                // Crossfade between view modes
                Crossfade(
                    targetState = viewMode,
                    label = "ArchiveViewModeTransition"
                ) { mode ->
                    when (mode) {
                        ArchiveViewMode.GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                groupedEntries.forEach { (monthYear, entries) ->
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        ArchiveSectionHeader(
                                            title = monthYear,
                                            count = entries.size,
                                            isBlueSky = isBlueSky
                                        )
                                    }
                                    items(entries, key = { it.id }) { entry ->
                                        val entryDate = entry.localDate
                                        val dayEvents = allEvents.filter { event ->
                                            event.day == entryDate.dayOfMonth && event.month == entryDate.monthValue
                                        }

                                        EnhancedJournalCard(
                                            entry = entry,
                                            events = dayEvents,
                                            searchQuery = searchQuery,
                                            onClick = { onEntryClick(entry) }
                                        )
                                    }
                                }
                            }
                        }

                        ArchiveViewMode.LIST -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                groupedEntries.forEach { (monthYear, entries) ->
                                    item(key = "hdr_$monthYear") {
                                        ArchiveSectionHeader(
                                            title = monthYear,
                                            count = entries.size,
                                            isBlueSky = isBlueSky
                                        )
                                    }
                                    items(entries, key = { it.id }) { entry ->
                                        val entryDate = entry.localDate
                                        val dayEvents = allEvents.filter { event ->
                                            event.day == entryDate.dayOfMonth && event.month == entryDate.monthValue
                                        }

                                        JournalListItemCard(
                                            entry = entry,
                                            events = dayEvents,
                                            searchQuery = searchQuery,
                                            onClick = { onEntryClick(entry) }
                                        )
                                    }
                                }
                            }
                        }

                        ArchiveViewMode.TIMELINE -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                groupedEntries.forEach { (monthYear, entries) ->
                                    item(key = "hdr_tl_$monthYear") {
                                        ArchiveSectionHeader(
                                            title = monthYear,
                                            count = entries.size,
                                            isBlueSky = isBlueSky
                                        )
                                    }
                                    itemsIndexed(entries, key = { _, it -> it.id }) { idx, entry ->
                                        val entryDate = entry.localDate
                                        val dayEvents = allEvents.filter { event ->
                                            event.day == entryDate.dayOfMonth && event.month == entryDate.monthValue
                                        }
                                        val isLast = idx == entries.lastIndex

                                        JournalTimelineItemCard(
                                            entry = entry,
                                            events = dayEvents,
                                            isLastInGroup = isLast,
                                            searchQuery = searchQuery,
                                            onClick = { onEntryClick(entry) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewModeIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun ArchiveSectionHeader(
    title: String,
    count: Int,
    isBlueSky: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isBlueSky) Color.White else MaterialTheme.colorScheme.onSurface
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Text(
                text = "$count ${if (count == 1) "entry" else "entries"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

private fun getMatchingSnippet(entry: JournalEntrySummary, query: String): String? {
    if (query.isBlank()) return entry.previewText
    val q = query.trim()
    val terms = q.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (terms.isEmpty()) return entry.previewText

    val fullText = entry.searchableText
    var idx = fullText.indexOf(q, ignoreCase = true)
    var matchLen = q.length

    if (idx == -1) {
        for (term in terms) {
            val termIdx = fullText.indexOf(term, ignoreCase = true)
            if (termIdx != -1) {
                idx = termIdx
                matchLen = term.length
                break
            }
        }
    }

    if (idx != -1) {
        val start = (idx - 30).coerceAtLeast(0)
        val end = (idx + matchLen + 70).coerceAtMost(fullText.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < fullText.length) "…" else ""
        return prefix + fullText.substring(start, end).trim() + suffix
    }

    // If query matched in tags, show matching tag(s)
    val matchingTags = entry.tags.filter { tag ->
        terms.any { term -> tag.contains(term, ignoreCase = true) }
    }
    if (matchingTags.isNotEmpty()) {
        val tagsStr = matchingTags.joinToString(", ") { "#$it" }
        return if (!entry.previewText.isNullOrBlank()) "$tagsStr • ${entry.previewText}" else tagsStr
    }

    return entry.previewText
}

// ─────────────────────────────────────────────────────────────
// Grid Card (2-column layout)
// ─────────────────────────────────────────────────────────────
@Composable
fun EnhancedJournalCard(
    entry: JournalEntrySummary,
    events: List<Event> = emptyList(),
    searchQuery: String = "",
    onClick: () -> Unit
) {
    val hasImages = entry.hasImages
    val imageCount = entry.imageCount
    val moodEmojis = entry.moodEmojis
    val haptic = LocalHapticFeedback.current

    val isBlueSky = LocalAppTheme.current == AppTheme.BLUE_SKY
    val isSpace = LocalAppTheme.current == AppTheme.ROSEWOOD

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(if (isSpace) 20.dp else 14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSpace) 1.dp else 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSpace -> Color(0xB3101216)
                isBlueSky -> Color.Black.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.surface
            },
            contentColor = if (isSpace || isBlueSky) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.onSurface
        ),
        border = when {
            isSpace -> BorderStroke(1.dp, Color(0x2EFFFFFF))
            isBlueSky -> BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Day of week and date + events
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = entry.dayOfWeek.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    HighlightedText(
                        text = entry.dateFormatted,
                        query = searchQuery,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        highlightColor = MaterialTheme.colorScheme.primary,
                        highlightBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (events.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        events.take(3).forEach { event ->
                            val icon = when (event.type) {
                                EventType.BIRTHDAY -> "🎂"
                                EventType.ANNIVERSARY -> "💍"
                                else -> "🎉"
                            }
                            Text(text = icon, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mood emojis
            if (moodEmojis.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    moodEmojis.take(4).forEach { emoji ->
                        Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Preview snippet
            val previewText = remember(entry, searchQuery) {
                getMatchingSnippet(entry, searchQuery)
            }
            HighlightedText(
                text = previewText ?: "No preview",
                query = searchQuery,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = if (previewText != null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                highlightColor = MaterialTheme.colorScheme.primary,
                highlightBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Footer counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
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

// ─────────────────────────────────────────────────────────────
// List View Card (Full-width detailed row)
// ─────────────────────────────────────────────────────────────
@Composable
private fun JournalListItemCard(
    entry: JournalEntrySummary,
    events: List<Event> = emptyList(),
    searchQuery: String = "",
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isBlueSky = LocalAppTheme.current == AppTheme.BLUE_SKY
    val isSpace = LocalAppTheme.current == AppTheme.ROSEWOOD

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSpace -> Color(0xB3101216)
                isBlueSky -> Color.Black.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = when {
            isSpace -> BorderStroke(1.dp, Color(0x2EFFFFFF))
            isBlueSky -> BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Date Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(width = 52.dp, height = 56.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${entry.localDate.dayOfMonth}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = entry.localDate.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.moodEmojis.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            entry.moodEmojis.take(4).forEach { emoji ->
                                Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        HighlightedText(
                            text = entry.dateFormatted,
                            query = searchQuery,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            highlightColor = MaterialTheme.colorScheme.primary,
                            highlightBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        )
                    }

                    if (events.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            events.take(3).forEach { event ->
                                val icon = when (event.type) {
                                    EventType.BIRTHDAY -> "🎂"
                                    EventType.ANNIVERSARY -> "💍"
                                    else -> "🎉"
                                }
                                Text(text = icon, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val preview = remember(entry, searchQuery) {
                    getMatchingSnippet(entry, searchQuery)
                }
                HighlightedText(
                    text = preview ?: "No preview available",
                    query = searchQuery,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (preview != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    highlightColor = MaterialTheme.colorScheme.primary,
                    highlightBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${entry.messageCount} msg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (entry.hasImages) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${entry.imageCount} img",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open entry",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Timeline View Item (Vertical connected stream)
// ─────────────────────────────────────────────────────────────
@Composable
private fun JournalTimelineItemCard(
    entry: JournalEntrySummary,
    events: List<Event> = emptyList(),
    isLastInGroup: Boolean,
    searchQuery: String = "",
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isBlueSky = LocalAppTheme.current == AppTheme.BLUE_SKY
    val isSpace = LocalAppTheme.current == AppTheme.ROSEWOOD

    IntrinsicHeightRow(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 2.dp)
    ) {
        // Timeline vertical rail + circle indicator
        Column(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            if (!isLastInGroup) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Content Card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isSpace -> Color(0xB3101216)
                    isBlueSky -> Color.Black.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            border = when {
                isSpace -> BorderStroke(1.dp, Color(0x2EFFFFFF))
                isBlueSky -> BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HighlightedText(
                        text = entry.dateFormatted,
                        query = searchQuery,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        highlightColor = MaterialTheme.colorScheme.primary,
                        highlightBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )

                    if (events.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            events.take(2).forEach { event ->
                                val icon = when (event.type) {
                                    EventType.BIRTHDAY -> "🎂"
                                    EventType.ANNIVERSARY -> "💍"
                                    else -> "🎉"
                                }
                                Text(text = icon, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (entry.moodEmojis.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        entry.moodEmojis.take(5).forEach { emoji ->
                            Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                val preview = remember(entry, searchQuery) {
                    getMatchingSnippet(entry, searchQuery)
                }
                if (!preview.isNullOrBlank()) {
                    HighlightedText(
                        text = preview,
                        query = searchQuery,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        highlightColor = MaterialTheme.colorScheme.primary,
                        highlightBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${entry.messageCount} messages",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    if (entry.hasImages) {
                        Text(
                            text = "\u2022 ${entry.imageCount} photos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntrinsicHeightRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        content = content
    )
}
