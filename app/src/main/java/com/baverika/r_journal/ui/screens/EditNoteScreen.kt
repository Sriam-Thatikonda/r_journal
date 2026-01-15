package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.data.local.entity.QuickNote
import com.baverika.r_journal.utils.ColorUtils

// Google Keep-style colors
private val editNoteColors = listOf(
    0xFF000000, // Pure Black (default)
    0xFFF28B82, // Soft Red
    0xFFFBBC04, // Warm Orange
    0xFFFFF475, // Soft Yellow
    0xFFCCFF90, // Light Green
    0xFFA7FFEB, // Cyan
    0xFFAECBFA, // Soft Blue
    0xFFD7AEFB, // Lavender
    0xFFFDCFE8, // Soft Pink
    0xFFE6C9A8, // Beige
    0xFFE8EAED, // Light Gray
    0xFF1F1F1F  // Dark Gray
)

private data class EditNoteItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val type: EditItemType,
    val isChecked: Boolean = false
)

private enum class EditItemType {
    TEXT, CHECKBOX, BULLET, NUMBERED
}

@Composable
fun EditNoteScreen(
    note: QuickNote,
    onSave: (QuickNote) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var selectedColor by remember { mutableLongStateOf(note.color) }
    
    // Determine initial mode
    val initialItems = remember(note.content) { parseNoteContent(note.content) }
    val initialMode = remember(initialItems) { 
        if (initialItems.any { it.type != EditItemType.TEXT }) EditItemType.CHECKBOX else EditItemType.TEXT
    }

    var currentItemType by remember { mutableStateOf(initialMode) }
    
    // Content States
    // If text mode, body is just content. If list mode, body is empty initially.
    var noteBody by remember { mutableStateOf(if (initialMode == EditItemType.TEXT) note.content else "") }
    // If list mode, items are parsed. If text mode, items empty initially.
    var listItems by remember { mutableStateOf(if (initialMode != EditItemType.TEXT) initialItems else emptyList()) }

    var currentListItemText by remember { mutableStateOf("") }
    
    val backgroundColor = Color(selectedColor)
    val textColor = ColorUtils.getContrastingTextColor(backgroundColor)
    val secondaryTextColor = ColorUtils.getSecondaryTextColor(backgroundColor)
    var showColorPicker by remember { mutableStateOf(false) }
    
    // Helper to switch modes
    fun switchMode(newType: EditItemType) {
        if (currentItemType == newType) return

        if (newType == EditItemType.TEXT) {
            // Switching TO Text
            noteBody = listItems.joinToString("\n") { it.text }
            listItems = emptyList()
        } else {
            // Switching TO List
            if (currentItemType == EditItemType.TEXT) {
                if (noteBody.isNotBlank()) {
                    listItems = noteBody.lines().map { 
                        EditNoteItem(text = it, type = newType) 
                    }
                }
                noteBody = ""
            } else {
                listItems = listItems.map { it.copy(type = newType) }
            }
        }
        currentItemType = newType
    }

    // Auto-save function
    fun saveNote() {
        val finalContent = if (currentItemType == EditItemType.TEXT) {
            noteBody
        } else {
            val itemsToSave = if (currentListItemText.isNotBlank()) {
                listItems + EditNoteItem(text = currentListItemText, type = currentItemType)
            } else {
                listItems
            }
            
            itemsToSave.joinToString("\n") { item ->
                when (item.type) {
                    EditItemType.CHECKBOX -> if (item.isChecked) "[x] ${item.text}" else "[ ] ${item.text}"
                    EditItemType.BULLET -> "- ${item.text}"
                    EditItemType.NUMBERED -> "${itemsToSave.indexOf(item) + 1}. ${item.text}"
                    EditItemType.TEXT -> item.text
                }
            }
        }
            
        if (title.isNotBlank() || finalContent.isNotBlank()) {
            onSave(
                note.copy(
                    title = title.ifBlank { "Untitled" },
                    content = finalContent,
                    color = selectedColor
                )
            )
        }
    }

    // Minimal UI - No Scaffold, No TopAppBar
    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top action bar with Title field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    saveNote()
                    onCancel()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                }

                // Title Field in the Top Bar
                Box(modifier = Modifier.weight(1f)) {
                    if (title.isEmpty()) {
                        Text(
                            text = "Title",
                            color = secondaryTextColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = MaterialTheme.typography.titleLarge.copy(color = textColor),
                        cursorBrush = SolidColor(textColor),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                    )
                }
                
                IconButton(
                    onClick = {
                        saveNote()
                        onCancel()
                    },
                    enabled = title.isNotBlank() || noteBody.isNotBlank() || listItems.isNotEmpty()
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = textColor)
                }
            }

            // Content Area - Spacer added
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (currentItemType == EditItemType.TEXT) {
                    // TEXT MODE
                    Box(modifier = Modifier.fillMaxSize()) {
                         if (noteBody.isEmpty()) {
                            Text(
                                "Note",
                                color = secondaryTextColor.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        BasicTextField(
                            value = noteBody,
                            onValueChange = { noteBody = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                            cursorBrush = SolidColor(textColor),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // LIST MODE
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        listItems.forEachIndexed { index, item ->
                            EditNoteItemRow(
                                item = item,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                onCheckedChange = { checked ->
                                    listItems = listItems.toMutableList().apply {
                                        this[index] = item.copy(isChecked = checked)
                                    }
                                },
                                onTextChange = { newText ->
                                    listItems = listItems.toMutableList().apply {
                                        this[index] = item.copy(text = newText)
                                    }
                                }
                            )
                        }

                        // Input for new item
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (currentItemType) {
                                    EditItemType.CHECKBOX -> Icons.Default.CheckBox
                                    EditItemType.BULLET -> Icons.Default.Circle
                                    EditItemType.NUMBERED -> Icons.Default.FormatListNumbered
                                    else -> Icons.Default.Article
                                },
                                contentDescription = null,
                                tint = secondaryTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = currentListItemText,
                                onValueChange = { currentListItemText = it },
                                placeholder = { Text("List item", color = secondaryTextColor.copy(alpha = 0.5f)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true, // Checklist items are single line
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (currentListItemText.isNotBlank()) {
                                            listItems = listItems + EditNoteItem(
                                                text = currentListItemText,
                                                type = currentItemType
                                            )
                                            currentListItemText = ""
                                        }
                                    }
                                ),
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedTextColor = textColor,
                                    focusedTextColor = textColor,
                                    cursorColor = textColor,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = LocalTextStyle.current.copy(color = textColor)
                            )
                        }
                    }
                }
            }

            // Bottom toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (ColorUtils.isColorLight(backgroundColor)) {
                    Color.White.copy(alpha = 0.95f)
                } else {
                    Color(0xFF1A1A1A)
                },
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item type buttons - icon only
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EditTypeButton(
                            icon = Icons.Default.Article,
                            type = EditItemType.TEXT,
                            selected = currentItemType == EditItemType.TEXT,
                            color = textColor,
                            primary = MaterialTheme.colorScheme.primary,
                            onClick = { switchMode(EditItemType.TEXT) }
                        )
                        EditTypeButton(
                            icon = Icons.Default.CheckBox,
                            type = EditItemType.CHECKBOX,
                            selected = currentItemType == EditItemType.CHECKBOX,
                            color = textColor,
                            primary = MaterialTheme.colorScheme.primary,
                            onClick = { switchMode(EditItemType.CHECKBOX) }
                        )
                        EditTypeButton(
                            icon = Icons.Default.Circle,
                            type = EditItemType.BULLET,
                            selected = currentItemType == EditItemType.BULLET,
                            color = textColor,
                            primary = MaterialTheme.colorScheme.primary,
                            onClick = { switchMode(EditItemType.BULLET) }
                        )
                        EditTypeButton(
                            icon = Icons.Default.FormatListNumbered,
                            type = EditItemType.NUMBERED,
                            selected = currentItemType == EditItemType.NUMBERED,
                            color = textColor,
                            primary = MaterialTheme.colorScheme.primary,
                            onClick = { switchMode(EditItemType.NUMBERED) }
                        )
                    }
                    
                    // Color badge
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(selectedColor))
                            .border(
                                width = 2.dp,
                                color = if (ColorUtils.isColorLight(Color(selectedColor))) 
                                    Color.Gray.copy(alpha = 0.3f) 
                                else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { showColorPicker = !showColorPicker },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Color",
                            tint = ColorUtils.getContrastingTextColor(Color(selectedColor)),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Expandable color picker
                if (showColorPicker) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(editNoteColors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(
                                        width = if (color == selectedColor) 3.dp else 1.dp,
                                        color = if (color == selectedColor) 
                                            MaterialTheme.colorScheme.primary 
                                        else Color.Gray.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { 
                                        selectedColor = color
                                        showColorPicker = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (color == selectedColor) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = ColorUtils.getContrastingTextColor(Color(color)),
                                        modifier = Modifier.size(20.dp)
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

// Helper to reuse button style
@Composable
private fun EditTypeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    type: EditItemType,
    selected: Boolean,
    color: Color,
    primary: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(
                if (selected) primary.copy(alpha = 0.2f) else Color.Transparent,
                CircleShape
            )
    ) {
        Icon(
            icon,
            contentDescription = type.name,
            tint = if (selected) primary else color
        )
    }
}

private fun parseNoteContent(content: String): List<EditNoteItem> {
    if (content.isBlank()) return emptyList()
    
    return content.lines().mapNotNull { line ->
        when {
            line.trimStart().startsWith("[ ]") -> EditNoteItem(
                text = line.trimStart().removePrefix("[ ]").trim(),
                type = EditItemType.CHECKBOX,
                isChecked = false
            )
            line.trimStart().startsWith("[x]") || line.trimStart().startsWith("[X]") -> EditNoteItem(
                text = line.trimStart().removePrefix("[x]").removePrefix("[X]").trim(),
                type = EditItemType.CHECKBOX,
                isChecked = true
            )
            line.trimStart().startsWith("-") -> EditNoteItem(
                text = line.trimStart().removePrefix("-").trim(),
                type = EditItemType.BULLET
            )
            line.trimStart().matches(Regex("^\\d+\\.\\s.*")) -> EditNoteItem(
                text = line.trimStart().replaceFirst(Regex("^\\d+\\.\\s"), ""),
                type = EditItemType.NUMBERED
            )
            else -> EditNoteItem(
                text = line,
                type = EditItemType.TEXT
            )
        }
    }
}

@Composable
private fun EditNoteItemRow(
    item: EditNoteItem,
    textColor: Color,
    secondaryTextColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (item.type) {
            EditItemType.CHECKBOX -> {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = textColor,
                        uncheckedColor = secondaryTextColor
                    )
                )
            }
            EditItemType.BULLET -> {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier
                        .size(8.dp)
                        .padding(horizontal = 12.dp)
                )
            }
            EditItemType.NUMBERED -> {
                Text(
                    text = "•",
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            EditItemType.TEXT -> {
               // Should generally not happen in list mode unless mixed, but safe fallback
               Spacer(modifier = Modifier.width(8.dp))
            }
        }

        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f).padding(8.dp),
            textStyle = LocalTextStyle.current.copy(
                color = if (item.isChecked) secondaryTextColor else textColor,
                textDecoration = if (item.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )
        )
    }
}
