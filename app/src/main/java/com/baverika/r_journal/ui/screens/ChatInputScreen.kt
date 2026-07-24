package com.baverika.r_journal.ui.screens

import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController

import androidx.compose.runtime.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.shape.CircleShape

import com.baverika.r_journal.R
import com.baverika.r_journal.data.local.entity.ChatMessage
import com.baverika.r_journal.ui.viewmodel.JournalViewModel
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.LocalAppTheme
import com.baverika.r_journal.utils.VoiceRecorderHelper

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

// Journal sub-components
import com.baverika.r_journal.ui.screens.journal.ChatBubble
import com.baverika.r_journal.ui.screens.journal.CompactMoodPicker
import com.baverika.r_journal.ui.screens.journal.EventBanner
import com.baverika.r_journal.ui.screens.journal.InputBar
import com.baverika.r_journal.ui.screens.journal.MessageOptionsSheet
import com.baverika.r_journal.ui.screens.journal.EditMessageDialog
import com.baverika.r_journal.ui.screens.journal.DeleteMessageDialog
import com.baverika.r_journal.ui.screens.journal.MediaPickerSheet
import com.baverika.r_journal.ui.screens.journal.ExitConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatInputScreen(
    viewModel: JournalViewModel,
    navController: NavController
) {
    // ── State from ViewModel ──
    val entry = viewModel.currentEntry
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedMoods = viewModel.getSelectedMoods()
    val canEditMood = viewModel.canEditMood
    val isCurrentEntryToday = viewModel.isCurrentEntryToday
    val todaysEvents by viewModel.todaysEvents.collectAsState()

    // ── Local UI State ──
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Voice recording
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) }

    val hasUnsavedText = textFieldValue.text.trim().isNotEmpty() || selectedImageUris.isNotEmpty() || isRecording

    // Dialog/sheet visibility
    var showExitConfirmation by remember { mutableStateOf(false) }
    var messageActionMenuForId by remember { mutableStateOf<String?>(null) }
    var editTextValue by remember { mutableStateOf(TextFieldValue("")) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }
    var tempImageFile by remember { mutableStateOf<File?>(null) }

    // Reply & focus
    var replyToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    val editFocusRequester = remember { FocusRequester() }

    // ── Launchers ──
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageFile != null) {
            val photoURI: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempImageFile!!)
            selectedImageUris = selectedImageUris + photoURI
        } else {
            tempImageFile = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            tempImageFile = createTempImageFile(context)
            tempImageFile?.let { file ->
                val photoURI = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                takePictureLauncher.launch(photoURI)
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = selectedImageUris + uris
        }
    }

    // ── Voice Recorder ──
    val voiceRecorder = remember {
        VoiceRecorderHelper(context).apply {
            onRecordingComplete = { path, duration ->
                viewModel.addVoiceNoteMessage(path, duration, replyToMessage)
                replyToMessage = null
                isRecording = false
                isPaused = false
            }
            onError = { error ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Recording failed: $error")
                }
                isRecording = false
                isPaused = false
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            if (voiceRecorder.startRecording()) {
                isRecording = true
                isPaused = false
                recordingDuration = 0L
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Microphone permission required for voice notes")
            }
        }
    }

    // ── Lifecycle: auto-save recording ──
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    if (voiceRecorder.isCurrentlyRecording) {
                        voiceRecorder.forceStopAndSave()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            voiceRecorder.release()
        }
    }

    // Update recording duration
    LaunchedEffect(isRecording, isPaused) {
        while (isRecording && !isPaused) {
            kotlinx.coroutines.delay(100)
            recordingDuration = voiceRecorder.currentDuration
        }
    }

    // Scroll-to-bottom detection
    val showScrollToBottom by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            entry.messages.size > 5 && lastVisibleItem < entry.messages.lastIndex - 2
        }
    }

    // ── Send handler ──
    fun handleSend() {
        val text = textFieldValue.text.trim()
        if (text.isNotBlank() || selectedImageUris.isNotEmpty()) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)

            if (text.isNotBlank()) {
                if (selectedImageUris.isNotEmpty()) {
                    viewModel.addMessageWithImage(text, selectedImageUris[0].toString(), replyTo = replyToMessage)
                    for (i in 1 until selectedImageUris.size) {
                        viewModel.addMessageWithImage("", selectedImageUris[i].toString(), replyTo = null)
                    }
                } else {
                    viewModel.addMessageWithImage(text, null, replyTo = replyToMessage)
                }
            } else {
                selectedImageUris.forEach { uri ->
                    viewModel.addMessageWithImage("", uri.toString(), replyTo = replyToMessage)
                }
            }

            textFieldValue = TextFieldValue("")
            selectedImageUris = emptyList()
            tempImageFile = null
            replyToMessage = null
        }
    }

    // ── UI Layout ──
    val isBlueSky = LocalAppTheme.current == AppTheme.BLUE_SKY

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image for Blue Sky theme
        if (isBlueSky) {
            Image(
                painter = painterResource(id = R.drawable.bg_journal_archive),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(6.dp),
                contentScale = ContentScale.Crop
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {

            // Snackbar host
            Box(modifier = Modifier.fillMaxWidth()) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // ── Header Section ──
            if (todaysEvents.isNotEmpty()) {
                todaysEvents.forEach { event ->
                    EventBanner(event = event)
                }
            }

            CompactMoodPicker(
                selectedMoods = selectedMoods,
                onMoodToggle = { mood -> viewModel.toggleMood(mood) },
                canEdit = canEditMood
            )

            // ── Messages List ──
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (entry.messages.isEmpty()) {
                    // Empty state with writing prompts
                    val prompts = listOf(
                        "What made you smile today?",
                        "What are you grateful for?",
                        "Describe your morning...",
                        "What's on your mind?",
                        "How did you feel today?",
                        "What challenged you today?",
                        "Write about a small win.",
                        "What are you looking forward to?"
                    )
                    val dayOfYear = java.time.LocalDate.now().dayOfYear
                    val dailyPrompt = prompts[dayOfYear % prompts.size]

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isCurrentEntryToday) "Start writing..." else "No entries yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isCurrentEntryToday) dailyPrompt else "Add a reflection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        itemsIndexed(entry.messages, key = { _, it -> it.id }) { index, message ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance -> totalDistance * 0.25f },
                                confirmValueChange = { newValue ->
                                    if (newValue == SwipeToDismissBoxValue.StartToEnd) {
                                        replyToMessage = message
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        false // Don't dismiss off screen, just trigger action & bounce back
                                    } else false
                                }
                            )

                            val replied = entry.messages.find { it.id == message.replyToMessageId }

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        val isSwiping = try {
                                            dismissState.requireOffset() > 10f
                                        } catch (e: Exception) {
                                            false
                                        }
                                        if (isSwiping) {
                                            Icon(
                                                imageVector = Icons.Default.Reply,
                                                contentDescription = "Reply",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                },
                                enableDismissFromStartToEnd = true,
                                enableDismissFromEndToStart = false,
                                content = {
                                    ChatBubble(
                                        message = message,
                                        isCurrentEntryToday = isCurrentEntryToday,
                                        isAddedLater = viewModel.isMessageAddedLater(message),
                                        navController = navController,
                                        onLongClick = {
                                            messageActionMenuForId = message.id
                                            editTextValue = TextFieldValue(
                                                text = message.content,
                                                selection = TextRange(message.content.length)
                                            )
                                            showOptionsSheet = true
                                        },
                                        repliedMessage = replied,
                                        isHighlighted = (message.id == highlightedMessageId),
                                        onQuoteClick = {
                                            val targetIndex = entry.messages.indexOfFirst { it.id == message.replyToMessageId }
                                            if (targetIndex >= 0) {
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(targetIndex)
                                                    highlightedMessageId = entry.messages[targetIndex].id
                                                    kotlinx.coroutines.delay(1500)
                                                    highlightedMessageId = null
                                                }
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                // Scroll-to-bottom FAB — placed as direct Box child
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(entry.messages.lastIndex)
                            }
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to bottom",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Auto-scroll to bottom on new messages
            LaunchedEffect(entry.messages.size) {
                if (entry.messages.isNotEmpty()) {
                    listState.animateScrollToItem(entry.messages.lastIndex)
                }
            }

            // ── Input Area ──
            InputBar(
                textFieldValue = textFieldValue,
                onTextChange = { textFieldValue = it },
                isCurrentEntryToday = isCurrentEntryToday,
                isRecording = isRecording,
                isPaused = isPaused,
                recordingDuration = recordingDuration,
                selectedImageUris = selectedImageUris,
                replyToMessage = replyToMessage,
                onSend = ::handleSend,
                onMediaPickerOpen = { showMediaPicker = true },
                onStartRecording = {
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) -> {
                            if (voiceRecorder.startRecording()) {
                                isRecording = true
                                isPaused = false
                                recordingDuration = 0L
                            }
                        }
                        else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onPauseRecording = {
                    if (voiceRecorder.pauseRecording()) { isPaused = true }
                },
                onResumeRecording = {
                    if (voiceRecorder.resumeRecording()) { isPaused = false }
                },
                onStopRecording = { voiceRecorder.stopAndSave() },
                onCancelRecording = {
                    voiceRecorder.cancelRecording()
                    isRecording = false
                    isPaused = false
                },
                onCancelReply = { replyToMessage = null },
                onRemoveImage = { index ->
                    selectedImageUris = selectedImageUris.toMutableList().apply { removeAt(index) }
                }
            )
        }

        // ── Sheets & Dialogs ──

        // Message Options Bottom Sheet
        if (showOptionsSheet && messageActionMenuForId != null) {
            MessageOptionsSheet(
                isCurrentEntryToday = isCurrentEntryToday,
                onDismiss = {
                    showOptionsSheet = false
                    messageActionMenuForId = null
                },
                onCopy = {
                    val messageToCopy = entry.messages.find { it.id == messageActionMenuForId }
                    messageToCopy?.let { clipboardManager.setText(AnnotatedString(it.content)) }
                    showOptionsSheet = false
                    messageActionMenuForId = null
                },
                onEdit = {
                    showOptionsSheet = false
                    showEditDialog = true
                },
                onDelete = {
                    showOptionsSheet = false
                    showDeleteDialog = true
                }
            )
        }

        // Edit dialog
        if (showEditDialog && messageActionMenuForId != null) {
            val messageToEdit = entry.messages.find { it.id == messageActionMenuForId }
            if (messageToEdit != null) {
                EditMessageDialog(
                    editTextValue = editTextValue,
                    onTextChange = { editTextValue = it },
                    focusRequester = editFocusRequester,
                    onSave = {
                        val trimmed = editTextValue.text.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.editMessage(messageToEdit.id, trimmed)
                        } else {
                            viewModel.deleteMessage(messageToEdit.id)
                        }
                        showEditDialog = false
                        messageActionMenuForId = null
                    },
                    onDismiss = {
                        showEditDialog = false
                        messageActionMenuForId = null
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    showEditDialog = false
                    messageActionMenuForId = null
                }
            }
        }

        // Delete dialog
        if (showDeleteDialog && messageActionMenuForId != null) {
            val messageToDelete = entry.messages.find { it.id == messageActionMenuForId }
            if (messageToDelete != null) {
                DeleteMessageDialog(
                    onConfirm = {
                        viewModel.deleteMessage(messageToDelete.id)
                        showDeleteDialog = false
                        messageActionMenuForId = null
                    },
                    onDismiss = {
                        showDeleteDialog = false
                        messageActionMenuForId = null
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    showDeleteDialog = false
                    messageActionMenuForId = null
                }
            }
        }

        // Media Picker Bottom Sheet
        if (showMediaPicker) {
            MediaPickerSheet(
                onDismiss = { showMediaPicker = false },
                onTakePhoto = {
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                            tempImageFile = createTempImageFile(context)
                            tempImageFile?.let { file ->
                                val photoURI = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                takePictureLauncher.launch(photoURI)
                            }
                        }
                        else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    showMediaPicker = false
                },
                onChooseFromGallery = {
                    pickImageLauncher.launch("image/*")
                    showMediaPicker = false
                }
            )
        }

        // Exit confirmation
        if (showExitConfirmation) {
            ExitConfirmationDialog(
                onDiscard = {
                    showExitConfirmation = false
                    navController.popBackStack()
                },
                onKeepWriting = { showExitConfirmation = false }
            )
        }
    }
}

// helper
fun createTempImageFile(context: android.content.Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
}
