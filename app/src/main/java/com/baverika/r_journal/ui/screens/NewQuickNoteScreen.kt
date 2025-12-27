// app/src/main/java/com/baverika/r_journal/ui/screens/NewQuickNoteScreen.kt

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.ui.viewmodel.QuickNoteViewModel
import com.baverika.r_journal.utils.ColorUtils

// Google Keep-style colors
private val noteColors = listOf(
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

data class NoteItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val type: ItemType,
    val isChecked: Boolean = false
)

enum class ItemType {
    TEXT, CHECKBOX, BULLET, NUMBERED
}

@Composable
fun NewQuickNoteScreen(
    viewModel: QuickNoteViewModel,
    navController: NavController
) {
    var title by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(noteColors.first()) }
    
    // Mode Switch State
    var currentItemType by remember { mutableStateOf(ItemType.TEXT) }
    
    // Content States
    var noteBody by remember { mutableStateOf("") } // For single block TEXT mode
    var listItems by remember { mutableStateOf<List<NoteItem>>(emptyList()) } // For List modes
    
    // Input for adding new list items
    var currentListItemText by remember { mutableStateOf("") }

    val backgroundColor = Color(selectedColor)
    val textColor = ColorUtils.getContrastingTextColor(backgroundColor)
    val secondaryTextColor = ColorUtils.getSecondaryTextColor(backgroundColor)
    
    var showColorPicker by remember { mutableStateOf(false) }

    // Helper to switch modes
    fun switchMode(newType: ItemType) {
        if (currentItemType == newType) return

        if (newType == ItemType.TEXT) {
            // Switching TO Text: Join items into body
            // We strip markdown symbols logically here or just keep text?
            // Usually Keep strips the checkboxes when going to text.
            noteBody = listItems.joinToString("\n") { it.text }
            listItems = emptyList()
        } else {
            // Switching TO List (from Text or other List)
            if (currentItemType == ItemType.TEXT) {
                // Parse body into items
                if (noteBody.isNotBlank()) {
                    listItems = noteBody.lines().map { 
                        NoteItem(text = it, type = newType) 
                    }
                }
                noteBody = ""
            } else {
                // Switching between list types (e.g. Bullet -> Checkbox)
                // Just update type of all items? Or just new ones? 
                // Google Keep updates ALL items to the new type usually.
                listItems = listItems.map { it.copy(type = newType) }
            }
        }
        currentItemType = newType
    }

    // Save Note
    fun saveNote() {
        val finalContent = if (currentItemType == ItemType.TEXT) {
            noteBody
        } else {
            // Combine pending input if exist
            val itemsToSave = if (currentListItemText.isNotBlank()) {
                listItems + NoteItem(text = currentListItemText, type = currentItemType)
            } else {
                listItems
            }
            
            itemsToSave.joinToString("\n") { item ->
                when (item.type) {
                    ItemType.CHECKBOX -> if (item.isChecked) "[x] ${item.text}" else "[ ] ${item.text}"
                    ItemType.BULLET -> "- ${item.text}"
                    ItemType.NUMBERED -> "${itemsToSave.indexOf(item) + 1}. ${item.text}"
                    ItemType.TEXT -> item.text
                }
            }
        }

        if (title.isNotBlank() || finalContent.isNotBlank()) {
            viewModel.addNote(
                title = title.ifBlank { "Untitled" },
                content = finalContent,
                color = selectedColor
            )
        }
    }

    // UI
    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // --- TOP ACTION BAR (with Title) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    saveNote()
                    navController.popBackStack() 
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }

                // Title Field
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
                        navController.popBackStack()
                    },
                    enabled = title.isNotBlank() || noteBody.isNotBlank() || listItems.isNotEmpty() || currentListItemText.isNotBlank()
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = textColor)
                }
            }

            // --- CONTENT AREA ---
            // Space between Title and Content
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    // Only scroll if in List mode or if Text mode needs it (BasicTextField handles its own scroll usually if max lines not set, but better to wrap)
            ) {
                if (currentItemType == ItemType.TEXT) {
                    // SINGLE BLOCK TEXT EDITOR
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
                    // LIST EDITOR (Checkboxes, etc.)
                   Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        listItems.forEachIndexed { index, item ->
                            NoteItemRow(
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
                                    ItemType.CHECKBOX -> Icons.Default.CheckBox
                                    ItemType.BULLET -> Icons.Default.Circle
                                    ItemType.NUMBERED -> Icons.Default.FormatListNumbered
                                    else -> Icons.Default.Add // Should not happen
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
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedTextColor = textColor,
                                    focusedTextColor = textColor,
                                    cursorColor = textColor,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (currentListItemText.isNotBlank()) {
                                        listItems = listItems + NoteItem(text = currentListItemText, type = currentItemType)
                                        currentListItemText = ""
                                    }
                                })
                            )
                        }
                   }
                }
            }

            // --- BOTTOM TOOLBAR ---
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
                    // Item type buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TypeButton(
                            icon = Icons.Default.Notes,
                            type = ItemType.TEXT,
                            selected = currentItemType == ItemType.TEXT,
                            color = textColor,
                            primary = MaterialTheme.colorScheme.primary,
                            onClick = { switchMode(ItemType.TEXT) }
                        )
                        TypeButton(
                            icon = Icons.Default.CheckBox,
                            type = ItemType.CHECKBOX,
                            selected = currentItemType == ItemType.CHECKBOX,
                            color = textColor,
                            primary = MaterialTheme.colorScheme.primary,
                            onClick = { switchMode(ItemType.CHECKBOX) }
                        )
                        TypeButton(
                            icon = Icons.Default.FormatListBulleted,
                            type = ItemType.BULLET,
                            selected = currentItemType == ItemType.BULLET,
                            color = textColor,
                            primary = MaterialTheme.colorScheme.primary,
                            onClick = { switchMode(ItemType.BULLET) }
                        )
                        TypeButton(
                            icon = Icons.Default.FormatListNumbered,
                            type = ItemType.NUMBERED,
                            selected = currentItemType == ItemType.NUMBERED,
                            color = textColor,
                            primary = MaterialTheme.colorScheme.primary,
                            onClick = { switchMode(ItemType.NUMBERED) }
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
                        items(noteColors) { color ->
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

@Composable
fun TypeButton(
    icon:  androidx.compose.ui.graphics.vector.ImageVector,
    type: ItemType,
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

@Composable
fun NoteItemRow(
    item: NoteItem,
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
            ItemType.CHECKBOX -> {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = textColor,
                        uncheckedColor = secondaryTextColor
                    )
                )
            }
            ItemType.BULLET -> {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(8.dp).padding(horizontal = 12.dp)
                )
            }
            ItemType.NUMBERED -> {
                 Text(
                    text = "•",
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            else -> {}
        }

        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            textStyle = LocalTextStyle.current.copy(
                color = if (item.isChecked) secondaryTextColor else textColor,
                textDecoration = if (item.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            ),
            modifier = Modifier.weight(1f).padding(8.dp)
        )
    }
}