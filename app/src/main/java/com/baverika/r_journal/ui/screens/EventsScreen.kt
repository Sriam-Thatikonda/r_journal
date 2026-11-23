package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.baverika.r_journal.data.local.entity.Event
import com.baverika.r_journal.data.local.entity.EventType
import com.baverika.r_journal.ui.viewmodel.EventUiModel
import com.baverika.r_journal.ui.viewmodel.EventViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    viewModel: EventViewModel,
    navController: NavController
) {
    val events by viewModel.allEvents.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            // Liquid Glass FAB for Events
            val glassBorder = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.65f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    Color.White.copy(alpha = 0.2f)
                )
            )
            Surface(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.5.dp, glassBorder, RoundedCornerShape(26.dp))
                    .clickable { showAddDialog = true },
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                shadowElevation = 6.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.30f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Event",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "🎂", fontSize = 48.sp)
                    Text(
                        text = "No upcoming special dates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Add birthdays, anniversaries, or special milestones\nto never miss a celebration.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Special Date")
                    }
                }
            }
        } else {
            // Group events into This Week, This Month, and Later
            val thisWeek = remember(events) { events.filter { it.daysRemaining <= 7L } }
            val thisMonth = remember(events) { events.filter { it.daysRemaining in 8L..31L } }
            val later = remember(events) { events.filter { it.daysRemaining > 31L } }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (thisWeek.isNotEmpty()) {
                    item {
                        EventSectionHeader(title = "⚡ Upcoming This Week", count = thisWeek.size)
                    }
                    items(thisWeek, key = { it.event.id }) { item ->
                        EventItem(
                            item = item,
                            isUrgent = true,
                            onDelete = { viewModel.deleteEvent(item.event) }
                        )
                    }
                }

                if (thisMonth.isNotEmpty()) {
                    item {
                        EventSectionHeader(title = "📅 Coming This Month", count = thisMonth.size)
                    }
                    items(thisMonth, key = { it.event.id }) { item ->
                        EventItem(
                            item = item,
                            isUrgent = false,
                            onDelete = { viewModel.deleteEvent(item.event) }
                        )
                    }
                }

                if (later.isNotEmpty()) {
                    item {
                        EventSectionHeader(title = "🎉 Later This Year", count = later.size)
                    }
                    items(later, key = { it.event.id }) { item ->
                        EventItem(
                            item = item,
                            isUrgent = false,
                            onDelete = { viewModel.deleteEvent(item.event) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddEventDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, day, month, year, type ->
                    viewModel.addEvent(
                        Event(
                            title = title,
                            day = day,
                            month = month,
                            year = year,
                            type = type
                        )
                    )
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun EventSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun EventItem(
    item: EventUiModel,
    isUrgent: Boolean = false,
    onDelete: () -> Unit
) {
    val event = item.event
    val daysRemaining = item.daysRemaining
    val today = LocalDate.now()
    val eventThisYear = LocalDate.of(today.year, event.month, event.day)
    val nextOccurrenceYear = if (eventThisYear.isBefore(today)) today.year + 1 else today.year

    val milestoneText = remember(event, nextOccurrenceYear) {
        if (event.year != null && event.year > 0) {
            val count = nextOccurrenceYear - event.year
            if (count > 0) {
                when (event.type) {
                    EventType.BIRTHDAY -> "Turning $count"
                    EventType.ANNIVERSARY -> {
                        val ordinal = when {
                            count % 100 in 11..13 -> "${count}th"
                            count % 10 == 1 -> "${count}st"
                            count % 10 == 2 -> "${count}nd"
                            count % 10 == 3 -> "${count}rd"
                            else -> "${count}th"
                        }
                        "$ordinal Anniversary"
                    }
                    else -> "$count years"
                }
            } else null
        } else null
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUrgent) 3.dp else 1.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isUrgent) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val eventEmoji = when (event.type) {
                EventType.BIRTHDAY -> "🎂"
                EventType.ANNIVERSARY -> "💍"
                EventType.MEETING -> "📅"
                else -> "🎉"
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = eventEmoji, fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (milestoneText != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = milestoneText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = LocalDate.of(nextOccurrenceYear, event.month, event.day).format(
                        DateTimeFormatter.ofPattern(if (nextOccurrenceYear == today.year) "EEEE, MMMM d" else "EEEE, MMMM d, yyyy")
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (daysRemaining == 0L) {
                        MaterialTheme.colorScheme.primary
                    } else if (daysRemaining <= 7L) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (daysRemaining == 0L) "Today! 🎉" else if (daysRemaining == 1L) "Tomorrow" else "$daysRemaining days",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (daysRemaining == 0L) {
                            MaterialTheme.colorScheme.onPrimary
                        } else if (daysRemaining <= 7L) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Int, Int, Int?, EventType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedType by remember { mutableStateOf(EventType.BIRTHDAY) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val initialEpochMillis = remember(selectedDate) {
            selectedDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEpochMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Special Date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 50) title = it },
                    label = { Text("Title (e.g. Mom's Birthday)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            text = "${title.length}/50",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Date: ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EventTypeChip(
                        type = EventType.BIRTHDAY,
                        selected = selectedType == EventType.BIRTHDAY,
                        onClick = { selectedType = EventType.BIRTHDAY }
                    )
                    EventTypeChip(
                        type = EventType.ANNIVERSARY,
                        selected = selectedType == EventType.ANNIVERSARY,
                        onClick = { selectedType = EventType.ANNIVERSARY }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EventTypeChip(
                        type = EventType.MEETING,
                        selected = selectedType == EventType.MEETING,
                        onClick = { selectedType = EventType.MEETING }
                    )
                    EventTypeChip(
                        type = EventType.CUSTOM,
                        selected = selectedType == EventType.CUSTOM,
                        onClick = { selectedType = EventType.CUSTOM }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(title, selectedDate.dayOfMonth, selectedDate.monthValue, selectedDate.year, selectedType)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EventTypeChip(
    type: EventType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val emoji = when (type) {
        EventType.BIRTHDAY -> "🎂"
        EventType.ANNIVERSARY -> "💍"
        EventType.MEETING -> "📅"
        EventType.CUSTOM -> "🎉"
    }
    val label = when (type) {
        EventType.BIRTHDAY -> "Birthday"
        EventType.ANNIVERSARY -> "Anniversary"
        EventType.MEETING -> "Meeting"
        EventType.CUSTOM -> "Custom"
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("$emoji $label") }
    )
}
