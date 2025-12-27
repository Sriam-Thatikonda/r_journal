package com.baverika.r_journal.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        topBar = {
            TopAppBar(
                title = { Text("Special Dates") },
                navigationIcon = {
                    IconButton(onClick = { 
                        // Open drawer (handled by parent scaffold usually, but here we might need a callback or just back)
                        // Assuming this screen is top-level in drawer, we might want a Menu icon if we can access drawer state
                        // For now, let's assume standard behavior or back if pushed
                        // If it's a top level drawer item, we usually pass a lambda to open drawer
                    }) {
                        Icon(Icons.Default.Event, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
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
                Text(
                    text = "No upcoming events.\nAdd a birthday or anniversary!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events, key = { it.event.id }) { item ->
                    EventItem(
                        item = item,
                        onDelete = { viewModel.deleteEvent(item.event) }
                    )
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
fun EventItem(
    item: EventUiModel,
    onDelete: () -> Unit
) {
    val event = item.event
    val daysRemaining = item.daysRemaining

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on type
            val icon = when (event.type) {
                EventType.BIRTHDAY -> Icons.Default.Cake
                EventType.ANNIVERSARY -> Icons.Default.Favorite
                EventType.MEETING -> Icons.Default.DateRange
                else -> Icons.Default.Event
            }
            val iconColor = when (event.type) {
                EventType.BIRTHDAY -> Color(0xFFFF6F00) // Orange
                EventType.ANNIVERSARY -> Color(0xFFE91E63) // Pink
                EventType.MEETING -> Color(0xFF2196F3) // Blue
                else -> MaterialTheme.colorScheme.primary
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = LocalDate.of(2024, event.month, event.day).format(DateTimeFormatter.ofPattern("MMMM d")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (daysRemaining == 0L) "Today!" else "$daysRemaining days",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (daysRemaining == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (daysRemaining == 0L) FontWeight.Bold else FontWeight.Normal
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Int, Int, Int?, EventType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedType by remember { mutableStateOf(EventType.BIRTHDAY) }
    var showDatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
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
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(type.name.lowercase().capitalize()) },
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    )
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
