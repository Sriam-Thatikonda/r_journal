package com.baverika.r_journal.ui.screens.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.baverika.r_journal.data.model.BlockType
import com.baverika.r_journal.ui.editor.components.RichBlockItem
import com.baverika.r_journal.ui.editor.components.RichFormattingToolbar
import com.baverika.r_journal.ui.viewmodel.QuickNoteEditorViewModel
import com.baverika.r_journal.utils.ColorUtils
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val NoteBackgroundColors = listOf(
    0xFF000000, // Pure Black
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

@Composable
fun QuickNoteEditorScreen(
    viewModel: QuickNoteEditorViewModel,
    navController: NavController
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val titleFocusRequester = remember { FocusRequester() }
    val blockFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Guaranteed lifecycle autosave on pause, stop, and screen exit
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.saveImmediately()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.saveImmediately()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Intercept system back gestures
    BackHandler {
        viewModel.saveImmediately()
        navController.popBackStack()
    }

    // Autofocus title on new note if empty
    LaunchedEffect(state.isLoaded) {
        if (state.isLoaded && state.isNewNote && state.title.isEmpty() && state.blocks.firstOrNull()?.text.isNullOrEmpty()) {
            titleFocusRequester.requestFocus()
        }
    }

    // Auto-focus active block when user splits block or adds new block
    LaunchedEffect(state.activeBlockIndex, state.blocks.size) {
        if (state.isLoaded && !(state.isNewNote && state.title.isEmpty() && state.blocks.firstOrNull()?.text.isNullOrEmpty())) {
            val activeIndex = state.activeBlockIndex
            if (activeIndex in state.blocks.indices) {
                val block = state.blocks[activeIndex]
                kotlinx.coroutines.delay(50)
                try {
                    blockFocusRequesters[block.id]?.requestFocus()
                } catch (_: Exception) {}
            }
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = Color(state.color),
        animationSpec = tween(200),
        label = "bgColor"
    )
    val textColor = ColorUtils.getContrastingTextColor(backgroundColor)
    val secondaryTextColor = ColorUtils.getSecondaryTextColor(backgroundColor)

    val formattedDate = remember(state.updatedAt) {
        val dt = Instant.ofEpochMilli(state.updatedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        dt.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy • h:mm a"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- TOP ACTION BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.saveImmediately()
                    navController.popBackStack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }

                // Title Input
                Box(modifier = Modifier.weight(1f)) {
                    if (state.title.isEmpty()) {
                        Text(
                            text = "Title",
                            color = secondaryTextColor.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    BasicTextField(
                        value = state.title,
                        onValueChange = { viewModel.onTitleChanged(it) },
                        textStyle = MaterialTheme.typography.titleLarge.copy(color = textColor),
                        cursorBrush = SolidColor(textColor),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                            .focusRequester(titleFocusRequester)
                    )
                }

                // Pin Button
                IconButton(onClick = { viewModel.togglePin() }) {
                    Icon(
                        imageVector = if (state.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (state.isPinned) "Unpin Note" else "Pin Note",
                        tint = if (state.isPinned) Color.Yellow else secondaryTextColor
                    )
                }

                // Note Background Color Selector
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .border(
                            width = 2.dp,
                            color = if (ColorUtils.isColorLight(backgroundColor))
                                Color.Gray.copy(alpha = 0.4f)
                            else Color.White.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { showColorPicker = !showColorPicker },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "Note Color",
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Button (if not brand new)
                if (!state.isNewNote) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Note",
                            tint = secondaryTextColor
                        )
                    }
                }

                // Explicit Save Checkmark
                IconButton(onClick = {
                    viewModel.saveImmediately()
                    navController.popBackStack()
                }) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Save Note",
                        tint = textColor
                    )
                }
            }

            // --- Expandable Note Background Color Picker ---
            if (showColorPicker) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    items(NoteBackgroundColors) { colorLong ->
                        val isSelected = colorLong == state.color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorLong))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.setNoteColor(colorLong)
                                    showColorPicker = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = ColorUtils.getContrastingTextColor(Color(colorLong)),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- Date & Autosave Status Subtitle ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edited $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryTextColor.copy(alpha = 0.6f)
                )

                // Subtle Autosave status
                Text(
                    text = if (state.isSaving) "Saving..." else "Saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.isSaving) MaterialTheme.colorScheme.primary else secondaryTextColor.copy(alpha = 0.5f),
                    fontWeight = if (state.isSaving) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // --- RICH TEXT CONTENT BLOCKS ---
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            // Focus last block or append block if last has text
                            val lastIndex = state.blocks.lastIndex
                            if (lastIndex >= 0) {
                                val lastBlock = state.blocks[lastIndex]
                                if (lastBlock.text.isNotBlank()) {
                                    viewModel.addNewBlockAfter(lastIndex)
                                    scope.launch {
                                        listState.animateScrollToItem(lastIndex + 1)
                                    }
                                } else {
                                    blockFocusRequesters[lastBlock.id]?.requestFocus()
                                }
                            }
                        }
                    )
            ) {
                itemsIndexed(
                    items = state.blocks,
                    key = { _, block -> block.id }
                ) { index, block ->
                    val requester = blockFocusRequesters.getOrPut(block.id) { FocusRequester() }

                    RichBlockItem(
                        block = block,
                        index = index,
                        isFocused = state.activeBlockIndex == index,
                        focusRequester = requester,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        onTextChanged = { newText, selection ->
                            viewModel.onBlockTextChanged(index, newText, selection)
                        },
                        onFocusGained = { selection ->
                            viewModel.onBlockFocusChanged(index, selection)
                        },
                        onToggleChecked = { blockId ->
                            viewModel.toggleChecklist(blockId)
                        },
                        onEnterPressed = { splitPos ->
                            viewModel.onEnterPressed(index, splitPos)
                            scope.launch {
                                listState.animateScrollToItem(index + 1)
                            }
                        },
                        onBackspaceOnEmpty = {
                            viewModel.onBackspaceOnEmpty(index)
                        }
                    )
                }

                // Comfortable bottom spacing above keyboard-docked toolbar
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }

            // --- BOTTOM FORMATTING TOOLBAR ---
            val activeType = state.blocks.getOrNull(state.activeBlockIndex)?.type ?: BlockType.PARAGRAPH
            RichFormattingToolbar(
                activeBlockType = activeType,
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                onFormatClick = { spanType, colorHex ->
                    viewModel.applyFormatting(spanType, colorHex)
                },
                onListTypeClick = { blockType ->
                    viewModel.setBlockType(blockType)
                },
                onUndoClick = { viewModel.undo() },
                onRedoClick = { viewModel.redo() }
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(text = "Delete Note", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to delete this note?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteNote {
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
