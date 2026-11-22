package com.baverika.r_journal.ui.screens.journal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.baverika.r_journal.R
import com.baverika.r_journal.data.local.entity.ChatMessage
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.LocalAppTheme
import com.baverika.r_journal.ui.viewmodel.JournalViewModel
import com.baverika.r_journal.utils.VoiceRecorderHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared chat engine scaffold powering both ChatInputScreen (Daily Journal)
 * and MineScreen (Personal Vault) with zero functionality loss.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun JournalChatScaffold(
    viewModel: JournalViewModel,
    navController: NavController,
    isCurrentEntryToday: Boolean = viewModel.isCurrentEntryToday,
    topHeaderContent: (@Composable () -> Unit)? = null,
    emptyStateContent: @Composable (onPromptSelected: (String) -> Unit) -> Unit
) {
    val entry = viewModel.currentEntry
    val isLoading by viewModel.isLoading.collectAsState()

    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Voice recording state
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableLongStateOf(0L) }

    val hasUnsavedText = textFieldValue.text.trim().isNotEmpty() || selectedImageUris.isNotEmpty() || isRecording

    // Dialog / sheet visibility
    var showExitConfirmation by remember { mutableStateOf(false) }
    var messageActionMenuForId by remember { mutableStateOf<String?>(null) }
    var editTextValue by remember { mutableStateOf(TextFieldValue("")) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var tempImageFile by remember { mutableStateOf<File?>(null) }

    // Reply & highlight state
    var replyToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    val editFocusRequester = remember { FocusRequester() }

    // Intercept back navigation when unsaved input exists
    BackHandler(enabled = hasUnsavedText) {
        showExitConfirmation = true
    }

    // Camera launcher
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageFile != null) {
            val photoURI: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempImageFile!!
            )
            selectedImageUris = selectedImageUris + photoURI
        } else {
            tempImageFile = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            tempImageFile = createTempImageFile(context)
            tempImageFile?.let { file ->
                val photoURI = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                takePictureLauncher.launch(photoURI)
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = selectedImageUris + uris
        }
    }

    // Voice Recorder Helper
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

    // Lifecycle observer for recording
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

    LaunchedEffect(isRecording, isPaused) {
        while (isRecording && !isPaused) {
            delay(100)
            recordingDuration = voiceRecorder.currentDuration
        }
    }

    // Scroll-to-bottom visibility
    val showScrollToBottom by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            entry.messages.size > 5 && lastVisibleItem < entry.messages.lastIndex - 1
        }
    }

    var previousMessageCount by remember { mutableIntStateOf(entry.messages.size) }
    LaunchedEffect(entry.messages.size) {
        if (entry.messages.size > previousMessageCount && entry.messages.isNotEmpty()) {
            listState.animateScrollToItem(entry.messages.lastIndex)
        }
        previousMessageCount = entry.messages.size
    }

    fun handleSend() {
        val text = textFieldValue.text.trim()
        if (text.isNotBlank() || selectedImageUris.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

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

    val isBlueSky = LocalAppTheme.current == AppTheme.BLUE_SKY

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
            // Screen-specific top header slot (Mood picker, event banners, etc.)
            if (topHeaderContent != null) {
                Surface(
                    color = if (isBlueSky) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    topHeaderContent()
                }
            }

            // Message Stream Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (entry.messages.isEmpty()) {
                    emptyStateContent { promptText ->
                        val formatted = "$promptText\n"
                        textFieldValue = TextFieldValue(
                            text = formatted,
                            selection = TextRange(formatted.length)
                        )
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 16.dp
                        )
                    ) {
                        itemsIndexed(entry.messages, key = { _, it -> it.id }) { index, message ->
                            val currentDateStr = formatWhatsAppDateHeader(message.timestamp)
                            val showDateHeader = if (index == 0) {
                                true
                            } else {
                                val prevDateStr = formatWhatsAppDateHeader(entry.messages[index - 1].timestamp)
                                currentDateStr != prevDateStr
                            }

                            if (showDateHeader) {
                                DateSeparator(dateText = currentDateStr)
                            }

                            val dismissState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance -> totalDistance * 0.25f },
                                confirmValueChange = { newValue ->
                                    if (newValue == SwipeToDismissBoxValue.StartToEnd) {
                                        replyToMessage = message
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        false
                                    } else {
                                        false
                                    }
                                }
                            )

                            val replied = entry.messages.find { it.id == message.replyToMessageId }

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = true,
                                enableDismissFromEndToStart = false,
                                backgroundContent = {
                                    val dragOffset = try {
                                        dismissState.requireOffset().coerceAtLeast(0f)
                                    } catch (e: Exception) {
                                        0f
                                    }
                                    val progress = (dragOffset / 140f).coerceIn(0f, 1f)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (progress > 0.05f) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (progress >= 0.85f) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .graphicsLayer {
                                                        scaleX = progress
                                                        scaleY = progress
                                                        alpha = progress
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Reply,
                                                        contentDescription = "Reply",
                                                        tint = if (progress >= 0.85f) {
                                                            MaterialTheme.colorScheme.onPrimary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSecondaryContainer
                                                        },
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                content = {
                                    ChatBubble(
                                        message = message,
                                        isCurrentEntryToday = isCurrentEntryToday,
                                        isAddedLater = false,
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
                                                    delay(1500)
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

                // Scroll-to-bottom Floating Button
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f) + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f) + slideOutVertically(targetOffsetY = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(entry.messages.lastIndex)
                            }
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to bottom",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Input Bar
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
                    if (voiceRecorder.pauseRecording()) {
                        isPaused = true
                    }
                },
                onResumeRecording = {
                    if (voiceRecorder.resumeRecording()) {
                        isPaused = false
                    }
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
        )

        // Dialogs & Sheets
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

        if (showMediaPicker) {
            MediaPickerSheet(
                onDismiss = { showMediaPicker = false },
                onTakePhoto = {
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                            tempImageFile = createTempImageFile(context)
                            tempImageFile?.let { file ->
                                val photoURI = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
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

        if (showExitConfirmation) {
            ExitConfirmationDialog(
                onDiscard = {
                    if (isRecording) {
                        voiceRecorder.cancelRecording()
                        isRecording = false
                        isPaused = false
                    }
                    showExitConfirmation = false
                    navController.popBackStack()
                },
                onKeepWriting = { showExitConfirmation = false }
            )
        }
    }
}

fun createTempImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
}
