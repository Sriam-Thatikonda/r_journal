package com.baverika.r_journal.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.data.local.entity.LifeTracker
import com.baverika.r_journal.data.local.entity.LifeTrackerEntry
import com.baverika.r_journal.ui.viewmodel.TrackerDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerDetailScreen(
    viewModel: TrackerDetailViewModel,
    onBack: () -> Unit
) {
    val tracker by viewModel.tracker.collectAsState()
    val entries by viewModel.entries.collectAsState()
    
    // Dialog States
    var showEntryDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<LifeTrackerEntry?>(null) }
    
    var showEditTrackerDialog by remember { mutableStateOf(false) }
    var showDeleteTrackerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tracker?.name ?: "Tracker Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditTrackerDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Tracker")
                    }
                    IconButton(onClick = { showDeleteTrackerDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Tracker")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                entryToEdit = null
                showEntryDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Header Info
            tracker?.let { t ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(t.color).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(t.icon, style = MaterialTheme.typography.displaySmall)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "${entries.size} Entries", 
                        style = MaterialTheme.typography.titleMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Timeline
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No history yet.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) { // spacing handled by item padding
                    items(entries) { entry ->
                        EntryCard(
                            entry = entry, 
                            onClick = { 
                                entryToEdit = entry
                                showEntryDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Entry Dialog (Add/Edit)
    if (showEntryDialog) {
        EntryDialog(
            entry = entryToEdit,
            onDismiss = { showEntryDialog = false },
            onSave = { date, note ->
                if (entryToEdit == null) {
                    viewModel.addEntry(date, note)
                } else {
                    viewModel.updateEntry(entryToEdit!!.copy(dateMillis = date, note = note))
                }
                showEntryDialog = false
            },
            onDelete = if (entryToEdit != null) { {
                viewModel.deleteEntry(entryToEdit!!)
                showEntryDialog = false
            } } else null
        )
    }

    // Edit Tracker Dialog
    if (showEditTrackerDialog && tracker != null) {
        TrackerEditDialog(
            tracker = tracker!!,
            onDismiss = { showEditTrackerDialog = false },
            onSave = { name, icon, color ->
                viewModel.updateTracker(tracker!!.copy(name = name, icon = icon, color = color))
                showEditTrackerDialog = false
            }
        )
    }

    // Delete Tracker Confirmation
    if (showDeleteTrackerDialog && tracker != null) {
        AlertDialog(
            onDismissRequest = { showDeleteTrackerDialog = false },
            title = { Text("Delete Tracker") },
            text = { Text("Are you sure you want to delete this tracker? All entries will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTracker(tracker!!)
                        showDeleteTrackerDialog = false
                        onBack() // Navigate back after deletion
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTrackerDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun EntryCard(entry: LifeTrackerEntry, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Timeline line
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
             Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
             Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
        }
        
        // Content
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    formatDate(entry.dateMillis),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (entry.note.isNotBlank()) {
                    Text(
                        entry.note,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun EntryDialog(
    entry: LifeTrackerEntry?,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var note by remember { mutableStateOf(entry?.note ?: "") }
    
    val calendar = Calendar.getInstance()
    if (entry != null) calendar.timeInMillis = entry.dateMillis
    
    var selectedDate by remember { mutableStateOf(calendar.timeInMillis) }
    
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Add Entry" else "Edit Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(formatDate(selectedDate))
                }
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete")
                    }
                }
                Button(onClick = { onSave(selectedDate, note) }) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerEditDialog(
    tracker: LifeTracker,
    onDismiss: () -> Unit, 
    onSave: (String, String, Long) -> Unit
) {
    var name by remember { mutableStateOf(tracker.name) }
    var selectedIcon by remember { mutableStateOf(tracker.icon) }
    var selectedColor by remember { mutableStateOf(tracker.color) }
    
    val icons = listOf("⚽", "\uD83D\uDE97", "\uD83D\uDCBB", "\uD83D\uDCAA", "\uD83E\uDE7A", "\uD83D\uDCD6", "\uD83C\uDFA8", "\uD83C\uDFB8", "✂️", "\uD83D\uDEEC")
    val colors = listOf(0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 0xFF2196F3, 0xFF009688, 0xFF4CAF50, 0xFFFFC107, 0xFFFF5722)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tracker") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                   icons.take(5).forEach { icon ->
                       SelectionChip1(selected = selectedIcon == icon, onClick = { selectedIcon = icon }, label = icon)
                   }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                   icons.drop(5).forEach { icon ->
                       SelectionChip1(selected = selectedIcon == icon, onClick = { selectedIcon = icon }, label = icon)
                   }
                }

                Text("Color", style = MaterialTheme.typography.labelLarge)
                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    colors.take(5).forEach { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .clickable { selectedColor = colorHex }
                                .then(if (selectedColor == colorHex) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, selectedIcon, selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionChip1(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = null
    )
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}
