package com.baverika.r_journal.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.baverika.r_journal.R
import com.baverika.r_journal.data.local.entity.ChatMessage
import com.baverika.r_journal.ui.screens.journal.*
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.LocalAppTheme
import com.baverika.r_journal.ui.viewmodel.JournalViewModel
import com.baverika.r_journal.utils.VoiceRecorderHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MineScreen(
    viewModel: JournalViewModel,
    navController: NavController
) {
    // ── State from ViewModel ──
    val entry = viewModel.currentEntry
    val isLoading by viewModel.isLoading.collectAsState()

    // ── Local UI State ──
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Reply & Dialog States
    var replyToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var editTextValue by remember { mutableStateOf(TextFieldValue("")) }
    var deletingMessageId by remember { mutableStateOf<String?>(null) }
    var messageActionMenuForId by remember { mutableStateOf<String?>(null) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Media Sheet State
    var showMediaSheet by remember { mutableStateOf(false) }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    // Voice Recorder Helper
    val voiceRecorder = remember { VoiceRecorderHelper(context) }
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableLongStateOf(0L) }

    // Attach callbacks to voiceRecorder
    DisposableEffect(voiceRecorder) {
        voiceRecorder.onRecordingComplete = { path, duration ->
            viewModel.addVoiceNoteMessage(path, duration, replyToMessage)
            replyToMessage = null
        }
        onDispose {
            voiceRecorder.release()
        }
    }

    // Auto update recording duration
    LaunchedEffect(isRecording, isPaused) {
        while (isRecording && !isPaused) {
            delay(100)
            recordingDuration = voiceRecorder.currentDuration
        }
    }

    // Lifecycle observer for voice recorder
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (voiceRecorder.isCurrentlyRecording) {
                    voiceRecorder.forceStopAndSave()
                    isRecording = false
                    isPaused = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Highlighted message for quote scrolling
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }

    // Floating action button for scroll to bottom
    val showScrollToBottom by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            entry.messages.size > 5 && lastVisibleItem < entry.messages.lastIndex - 2
        }
    }

    // Scroll to bottom when new message arrives
    LaunchedEffect(entry.messages.size) {
        if (entry.messages.isNotEmpty()) {
            listState.animateScrollToItem(entry.messages.size - 1)
        }
    }

    // Media Pickers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = selectedImageUris + uris
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTempUri != null) {
            selectedImageUris = selectedImageUris + cameraTempUri!!
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceRecorder.startRecording()
            isRecording = true
            isPaused = false
        }
    }

    fun startOrResumeRecording() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            if (!voiceRecorder.isCurrentlyRecording) {
                voiceRecorder.startRecording()
                isRecording = true
                isPaused = false
            } else if (isPaused) {
                voiceRecorder.resumeRecording()
                isPaused = false
            }
        } else {
            audioPermissionLauncher.launch(permission)
        }
    }

    fun handleSend() {
        val text = textFieldValue.text.trim()
        if (text.isNotBlank() || selectedImageUris.isNotEmpty()) {
            if (selectedImageUris.isNotEmpty()) {
                selectedImageUris.forEach { uri ->
                    viewModel.addMessageWithImage(text, uri.toString(), replyTo = replyToMessage)
                }
            } else {
                viewModel.addMessage(text, replyTo = replyToMessage)
            }
            textFieldValue = TextFieldValue("")
            selectedImageUris = emptyList()
            replyToMessage = null
            coroutineScope.launch {
                if (entry.messages.isNotEmpty()) {
                    listState.animateScrollToItem(entry.messages.size - 1)
                }
            }
        }
    }

    val isBlueSkyTheme = LocalAppTheme.current == AppTheme.BLUE_SKY

    Box(modifier = Modifier.fillMaxSize()) {
        if (isBlueSkyTheme) {
            Image(
                painter = painterResource(id = R.drawable.bg_journal_archive),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(6.dp)
            )
        }

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                InputBar(
                    textFieldValue = textFieldValue,
                    onTextChange = { textFieldValue = it },
                    isCurrentEntryToday = true,
                    isRecording = isRecording,
                    isPaused = isPaused,
                    recordingDuration = recordingDuration,
                    selectedImageUris = selectedImageUris,
                    replyToMessage = replyToMessage,
                    onSend = { handleSend() },
                    onMediaPickerOpen = { showMediaSheet = true },
                    onStartRecording = { startOrResumeRecording() },
                    onPauseRecording = {
                        voiceRecorder.pauseRecording()
                        isPaused = true
                    },
                    onResumeRecording = {
                        voiceRecorder.resumeRecording()
                        isPaused = false
                    },
                    onStopRecording = {
                        voiceRecorder.stopAndSave()
                        isRecording = false
                        isPaused = false
                    },
                    onCancelRecording = {
                        voiceRecorder.cancelRecording()
                        isRecording = false
                        isPaused = false
                    },
                    onCancelReply = { replyToMessage = null },
                    onRemoveImage = { index ->
                        selectedImageUris = selectedImageUris.filterIndexed { i, _ -> i != index }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (entry.messages.isEmpty()) {
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
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Mine Stream",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add thoughts, notes, and photos anytime.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        itemsIndexed(entry.messages, key = { _, it -> it.id }) { index, message ->
                            // ── WhatsApp-style Date Header ──
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
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        false
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
                                        } catch (_: Exception) {
                                            false
                                        }
                                        if (isSwiping) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Reply,
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
                                        isCurrentEntryToday = true,
                                        isAddedLater = false, // Clean look: no "Added on date" tag
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

                // Scroll-to-bottom FAB
                AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                if (entry.messages.isNotEmpty()) {
                                    listState.animateScrollToItem(entry.messages.size - 1)
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to bottom"
                        )
                    }
                }
            }
        }

        // Bottom Sheets & Dialogs
        if (showOptionsSheet && messageActionMenuForId != null) {
            val selectedMessage = entry.messages.find { it.id == messageActionMenuForId }
            MessageOptionsSheet(
                isCurrentEntryToday = true,
                onDismiss = {
                    showOptionsSheet = false
                    messageActionMenuForId = null
                },
                onCopy = {
                    selectedMessage?.let { msg ->
                        clipboardManager.setText(AnnotatedString(msg.content))
                    }
                    showOptionsSheet = false
                },
                onEdit = {
                    editingMessageId = selectedMessage?.id
                    showOptionsSheet = false
                },
                onDelete = {
                    deletingMessageId = selectedMessage?.id
                    showOptionsSheet = false
                }
            )
        }

        if (editingMessageId != null) {
            EditMessageDialog(
                editTextValue = editTextValue,
                onTextChange = { editTextValue = it },
                focusRequester = focusRequester,
                onSave = {
                    val msgId = editingMessageId
                    if (msgId != null) {
                        viewModel.editMessage(msgId, editTextValue.text)
                    }
                    editingMessageId = null
                },
                onDismiss = { editingMessageId = null }
            )
        }

        if (deletingMessageId != null) {
            DeleteMessageDialog(
                onConfirm = {
                    val msgId = deletingMessageId
                    if (msgId != null) {
                        viewModel.deleteMessage(msgId)
                    }
                    deletingMessageId = null
                },
                onDismiss = { deletingMessageId = null }
            )
        }

        if (showMediaSheet) {
            MediaPickerSheet(
                onDismiss = { showMediaSheet = false },
                onTakePhoto = {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val photoFile = File(context.cacheDir, "JPEG_${timeStamp}_.jpg")
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
                    cameraTempUri = uri
                    cameraLauncher.launch(uri)
                    showMediaSheet = false
                },
                onChooseFromGallery = {
                    galleryLauncher.launch("image/*")
                    showMediaSheet = false
                }
            )
        }
    }
}

@Composable
fun DateSeparator(dateText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

fun formatWhatsAppDateHeader(timestamp: Long): String {
    val messageDate = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    return when (messageDate) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> {
            if (messageDate.year == today.year) {
                messageDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
            } else {
                messageDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
            }
        }
    }
}
