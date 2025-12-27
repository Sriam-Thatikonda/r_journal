package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.data.local.QuickNotesPreferences
import com.baverika.r_journal.data.local.entity.QuickNote
import com.baverika.r_journal.ui.viewmodel.QuickNoteViewModel
import com.baverika.r_journal.utils.ColorUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuickNotesScreen(
    viewModel: QuickNoteViewModel,
    navController: NavController
) {
    val notes by viewModel.allNotes.collectAsState(initial = emptyList())
    val layoutType by viewModel.layoutType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // State to hold the note currently being edited
    var noteToEdit by remember { mutableStateOf<QuickNote?>(null) }
    var noteToDelete by remember { mutableStateOf<QuickNote?>(null) }

    // Determine if we are in edit mode
    val isEditing = noteToEdit != null

    Scaffold(
        // No TopAppBar - direct content
        floatingActionButton = {
            if (!isEditing) {
                FloatingActionButton(
                    onClick = { navController.navigate("new_quick_note") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Quick Note")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // --- Main List Content ---
            if (!isEditing) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search Bar and Layout Toggle in same row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Bar (70% width)
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Search...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.weight(0.7f),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        
                        // Layout toggle button
                        IconButton(
                            onClick = {
                                val newLayout = if (layoutType == QuickNotesPreferences.LAYOUT_MASONRY) {
                                    QuickNotesPreferences.LAYOUT_LIST
                                } else {
                                    QuickNotesPreferences.LAYOUT_MASONRY
                                }
                                viewModel.setLayoutType(newLayout)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (layoutType == QuickNotesPreferences.LAYOUT_MASONRY) {
                                    Icons.Default.ViewAgenda // List icon
                                } else {
                                    Icons.Default.GridView // Grid icon
                                },
                                contentDescription = "Toggle Layout"
                            )
                        }
                    }

                    if (notes.isEmpty()) {
                        if (searchQuery.isNotEmpty()) {
                            // Empty search results
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No notes found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // Empty state (no notes at all)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NoteAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(120.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = "No Notes Yet",
                                    style = MaterialTheme.typography.headlineMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Capture quick thoughts and ideas",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = { navController.navigate("new_quick_note") }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Create Note")
                                }
                            }
                        }
                    } else {
                        // Display notes based on layout type
                        if (layoutType == QuickNotesPreferences.LAYOUT_MASONRY) {
                            // Masonry (Staggered Grid) Layout
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 8.dp,
                                    end = 8.dp,
                                    bottom = 80.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalItemSpacing = 8.dp
                            ) {
                                items(notes, key = { it.id }) { note ->
                                    QuickNoteCard(
                                        note = note,
                                        onDelete = { noteToDelete = it },
                                        onClick = { noteToEdit = note },
                                        modifier = Modifier.animateItemPlacement()
                                    )
                                }
                            }
                        } else {
                            // List Layout
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 80.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(notes, key = { it.id }) { note ->
                                    QuickNoteCard(
                                        note = note,
                                        onDelete = { noteToDelete = it },
                                        onClick = { noteToEdit = note },
                                        modifier = Modifier.animateItemPlacement()
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // --- End Main List Content ---

            // --- Edit Note UI (Interactive like creation screen) ---
            if (isEditing && noteToEdit != null) {
                EditNoteScreen(
                    note = noteToEdit!!,
                    onSave = { updatedNote ->
                        viewModel.updateNote(updatedNote)
                        noteToEdit = null
                    },
                    onCancel = { noteToEdit = null }
                )
            }
            // --- End Edit Note UI ---

            if (noteToDelete != null) {
                AlertDialog(
                    onDismissRequest = { noteToDelete = null },
                    title = { Text("Delete Note") },
                    text = { Text("Are you sure you want to delete this note?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                noteToDelete?.let { viewModel.deleteNote(it) }
                                noteToDelete = null
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { noteToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Google Keep-style note card with support for:
 * - Variable height
 * - Colored backgrounds
 * - Checklists
 * - Bullet and numbered lists
 */
@Composable
fun QuickNoteCard(
    note: QuickNote,
    onDelete: (QuickNote) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = Color(note.color)
    val textColor = ColorUtils.getContrastingTextColor(cardColor)
    val secondaryTextColor = ColorUtils.getSecondaryTextColor(cardColor)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF808080), // Grey border
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Title and Delete Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor
                    )
                }
                IconButton(
                    onClick = { onDelete(note) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Note",
                        modifier = Modifier.size(18.dp),
                        tint = secondaryTextColor
                    )
                }
            }

            // Content with rich text parsing
            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                ParsedContent(
                    content = note.content,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor
                )
            }

            // Timestamp
            Text(
                text = formatTimestamp(note.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = secondaryTextColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Parses and renders content with support for:
 * - Checklists (lines starting with [ ] or [x])
 * - Bullet lists (lines starting with -, *, or •)
 * - Numbered lists (lines starting with 1., 2., etc.)
 * - Regular text
 */
@Composable
fun ParsedContent(
    content: String,
    textColor: Color,
    secondaryTextColor: Color
) {
    val lines = content.lines()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        lines.forEach { line ->
            when {
                // Checklist item - unchecked
                line.trimStart().startsWith("[ ]") -> {
                    ChecklistItem(
                        text = line.trimStart().removePrefix("[ ]").trim(),
                        isChecked = false,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                }
                // Checklist item - checked
                line.trimStart().startsWith("[x]") || line.trimStart().startsWith("[X]") -> {
                    ChecklistItem(
                        text = line.trimStart().removePrefix("[x]").removePrefix("[X]").trim(),
                        isChecked = true,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                }
                // Bullet list
                line.trimStart().startsWith("-") || 
                line.trimStart().startsWith("*") || 
                line.trimStart().startsWith("•") -> {
                    BulletItem(
                        text = line.trimStart().removePrefix("-").removePrefix("*").removePrefix("•").trim(),
                        textColor = textColor
                    )
                }
                // Numbered list
                line.trimStart().matches(Regex("^\\d+\\.\\s.*")) -> {
                    NumberedItem(
                        text = line.trimStart(),
                        textColor = textColor
                    )
                }
                // Regular text
                line.isNotBlank() -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun ChecklistItem(
    text: String,
    isChecked: Boolean,
    textColor: Color,
    secondaryTextColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isChecked) {
                secondaryTextColor
            } else {
                textColor
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (isChecked) TextDecoration.LineThrough else null,
            color = if (isChecked) {
                secondaryTextColor.copy(alpha = 0.7f)
            } else {
                textColor
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun BulletItem(
    text: String,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun NumberedItem(
    text: String,
    textColor: Color
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = textColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp)
    )
}

private fun formatTimestamp(timestamp: Long): String {
    return LocalDateTime
        .ofInstant(java.time.Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a"))
}