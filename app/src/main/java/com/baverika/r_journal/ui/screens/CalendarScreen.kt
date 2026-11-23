package com.baverika.r_journal.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.baverika.r_journal.data.local.entity.Event
import com.baverika.r_journal.data.local.entity.EventType
import com.baverika.r_journal.data.local.entity.JournalEntrySummary
import com.baverika.r_journal.repository.EventRepository
import com.baverika.r_journal.repository.JournalRepository
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    journalRepo: JournalRepository,
    navController: NavController,
    eventRepo: EventRepository? = null
) {
    val isSpace = LocalAppTheme.current == AppTheme.ROSEWOOD
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val scope = rememberCoroutineScope()

    val entries by journalRepo.allEntrySummaries.collectAsState(initial = emptyList())
    val events by (eventRepo?.allEvents ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    // Map of Date -> JournalEntrySummary
    val entryMap = remember(entries) {
        entries.associateBy { it.localDate }
    }

    // Map of "month-day" -> List<Event> for quick matching
    val eventMap = remember(events) {
        events.groupBy { "${it.month}-${it.day}" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = { totalDrag = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    totalDrag += dragAmount
                    if (totalDrag < -100) {
                        currentMonth = currentMonth.plusMonths(1)
                        totalDrag = 0f
                    } else if (totalDrag > 100) {
                        currentMonth = currentMonth.minusMonths(1)
                        totalDrag = 0f
                    }
                }
            }
    ) {
        // Month Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (currentMonth != YearMonth.now()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            currentMonth = YearMonth.now()
                            selectedDate = LocalDate.now()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Today", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
            }
        }

        // Days of Week Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Calendar Grid Calculations
        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value // 1=Mon .. 7=Sun
        val startOffset = if (firstDayOfWeek == 7) 0 else firstDayOfWeek
        val totalSlots = daysInMonth + startOffset
        val daysList = (1..totalSlots).map {
            if (it <= startOffset) null else it - startOffset
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            userScrollEnabled = false
        ) {
            items(daysList) { day ->
                if (day != null) {
                    val date = currentMonth.atDay(day)
                    val entry = entryMap[date]
                    val dayEvents = eventMap["${date.monthValue}-${date.dayOfMonth}"] ?: emptyList()
                    val isToday = date == LocalDate.now()
                    val isSelected = date == selectedDate
                    val isFuture = date.isAfter(LocalDate.now())

                    CalendarDay(
                        day = day,
                        isToday = isToday,
                        isSelected = isSelected,
                        hasEntry = entry != null,
                        moodEmojis = entry?.moodEmojis ?: emptyList(),
                        events = dayEvents,
                        isFuture = isFuture,
                        onClick = {
                            selectedDate = date
                        }
                    )
                } else {
                    Spacer(modifier = Modifier.aspectRatio(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Embedded Day Agenda Section below Grid
        val selectedEntry = entryMap[selectedDate]
        val selectedEvents = eventMap["${selectedDate.monthValue}-${selectedDate.dayOfMonth}"] ?: emptyList()

        DayAgendaCard(
            date = selectedDate,
            entry = selectedEntry,
            events = selectedEvents,
            onOpenEntry = {
                selectedEntry?.let { entry ->
                    navController.navigate("chat_input/${entry.id}")
                }
            },
            onCreateEntry = {
                scope.launch {
                    val dateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
                    val newEntry = journalRepo.getOrCreateEntryForDate(dateMillis)
                    navController.navigate("chat_input/${newEntry.id}")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasEntry: Boolean,
    moodEmojis: List<String>,
    events: List<Event>,
    isFuture: Boolean,
    onClick: () -> Unit
) {
    val isSpace = LocalAppTheme.current == AppTheme.ROSEWOOD

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        hasEntry -> if (isSpace) Color(0x3314161C) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isToday -> MaterialTheme.colorScheme.primary
        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderStroke = when {
        isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        isToday -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        isSpace && hasEntry -> BorderStroke(0.5.dp, Color(0x33FFFFFF))
        else -> null
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(12.dp))
                else Modifier
            )
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )

            // Indicators row (Entry dot / mood + Event cake)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (events.isNotEmpty()) {
                    val eventIcon = when (events.first().type) {
                        EventType.BIRTHDAY -> "🎂"
                        EventType.ANNIVERSARY -> "💍"
                        else -> "🎉"
                    }
                    Text(text = eventIcon, fontSize = 9.sp)
                } else if (moodEmojis.isNotEmpty()) {
                    Text(text = moodEmojis.first(), fontSize = 9.sp)
                } else if (hasEntry) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    )
                }
            }
        }
    }
}

@Composable
private fun DayAgendaCard(
    date: LocalDate,
    entry: JournalEntrySummary?,
    events: List<Event>,
    onOpenEntry: () -> Unit,
    onCreateEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSpace = LocalAppTheme.current == AppTheme.ROSEWOOD
    val isToday = date == LocalDate.now()
    val isFuture = date.isAfter(LocalDate.now())
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault()) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSpace) Color(0xB3101216) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSpace) Color(0x2EFFFFFF) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSpace) 1.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Date & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = date.format(formatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isToday) "Today" else if (date == LocalDate.now().minusDays(1)) "Yesterday" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (entry != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${entry.messageCount} ${if (entry.messageCount == 1) "entry" else "entries"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Events Banner (if any)
            if (events.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        events.forEach { event ->
                            val icon = when (event.type) {
                                EventType.BIRTHDAY -> "🎂"
                                EventType.ANNIVERSARY -> "💍"
                                else -> "🎉"
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = icon, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Journal Entry Preview or Empty Prompt
            if (entry != null) {
                // Mood Emojis
                if (entry.moodEmojis.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mood:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        entry.moodEmojis.forEach { emoji ->
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }

                // Preview Snippet
                val preview = entry.previewText
                if (!preview.isNullOrBlank()) {
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))

                Button(
                    onClick = onOpenEntry,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Journal Entry")
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFuture) "Future date (no entries yet)" else "No journal entry logged for this day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

                if (!isFuture) {
                    Spacer(modifier = Modifier.weight(1f, fill = false))
                    OutlinedButton(
                        onClick = onCreateEntry,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isToday) "Write Today's Entry" else "Write Entry for this Date")
                    }
                }
            }
        }
    }
}
