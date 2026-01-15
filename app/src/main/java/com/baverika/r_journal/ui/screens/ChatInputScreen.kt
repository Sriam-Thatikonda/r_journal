package com.baverika.r_journal.ui.screens

// Kotlin / stdlib / coroutines
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

// Android / Core / Navigation
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController

// Compose runtime & foundation
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState

// Compose UI / graphics / resources
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Coil
import coil.compose.AsyncImage

// Activity result APIs (missing earlier)
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Material1 small imports REMOVED

// Material3 (primary UI) - alias Text/Icon to avoid ambiguity
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Divider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator

// Experimental annotation import
import androidx.compose.material3.ExperimentalMaterial3Api

// App classes
import com.baverika.r_journal.R
import com.baverika.r_journal.data.local.entity.ChatMessage
import com.baverika.r_journal.ui.viewmodel.JournalViewModel

// Icons (shared) - using material icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.blur

import com.baverika.r_journal.data.local.entity.Event
import com.baverika.r_journal.data.local.entity.EventType
import com.baverika.r_journal.ui.components.RecordingIndicator
import com.baverika.r_journal.ui.components.VoiceNotePlayer
import com.baverika.r_journal.utils.VoiceRecorderHelper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.baverika.r_journal.ui.theme.LocalAppTheme
import com.baverika.r_journal.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CompactMoodPicker(
    selectedMoods: Set<String>,
    onMoodToggle: (String) -> Unit,
    canEdit: Boolean,
    modifier: Modifier = Modifier
) {
    // Only show if we can edit OR if there are selected moods
    if (canEdit || selectedMoods.isNotEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                M3Text(
                    text = "Mood:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )

                val AVAILABLE_MOODS = listOf(
                    "happy" to "\uD83D\uDE0A",
                    "calm" to "\uD83D\uDE0C",
                    "anxious" to "\uD83D\uDE30",
                    "sad" to "\uD83D\uDE22",
                    "tired" to "\uD83D\uDE34"
                )

                // If editing, show all options. If not, show only selected.
                val moodsDisplay = if (canEdit) AVAILABLE_MOODS else AVAILABLE_MOODS.filter { it.first in selectedMoods }

                moodsDisplay.forEach { (mood, emoji) ->
                    val isSelected = mood in selectedMoods
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.2f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "scale"
                    )

                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .scale(scale)
                            .background(
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable(enabled = canEdit) { onMoodToggle(mood) }
                    ) {
                        M3Text(text = emoji, fontSize = 22.sp)
                    }
                }

                Spacer(Modifier.weight(1f))

                if (selectedMoods.isNotEmpty() && canEdit) {
                    M3Text(
                        text = "${selectedMoods.size}/3",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                if (!canEdit) {
                    M3Text(
                        text = "Past entry",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    } else {
        // Just show a small label if it's a past entry with no mood
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            M3Text(
                text = "Past Entry • No Mood Selected",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: ChatMessage,
    isCurrentEntryToday: Boolean,
    isAddedLater: Boolean,
    navController: NavController,
    onLongClick: (() -> Unit)? = null,
    repliedMessage: ChatMessage? = null,
    onQuoteClick: (() -> Unit)? = null,
    isHighlighted: Boolean = false

) {
    val isUser = message.role == "user"
    val timestamp = LocalDateTime
        .ofInstant(java.time.Instant.ofEpochMilli(message.timestamp), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
    ) {
        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isHighlighted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent)
                .padding(vertical = 6.dp)
                .then(
                    if (onLongClick != null) {
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = onLongClick
                        )
                    } else Modifier
                )
        ) {
            Column(
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                if (isAddedLater) {
                    M3Text(
                        text = "Added ${LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(message.timestamp), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                // quoted reply (if any)
                repliedMessage?.let { original ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .fillMaxWidth(0.82f)
                            .clickable { onQuoteClick?.invoke() }
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(40.dp)
                                    .background(
                                        if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                M3Text(
                                    text = if (original.role == "user") "You" else original.role.replaceFirstChar { it.uppercaseChar() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                M3Text(
                                    text = original.content.ifBlank { "[Image]" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                // image
                message.imageUri?.let { imagePath ->
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .size(200.dp)
                            .clickable {
                                val encodedPath = URLEncoder.encode(
                                    imagePath,
                                    StandardCharsets.UTF_8.toString()
                                )
                                navController.navigate("image_viewer/$encodedPath")
                            },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        AsyncImage(
                            model = Uri.fromFile(File(imagePath)),
                            contentDescription = "Attached Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.ic_launcher_foreground),
                            error = painterResource(R.drawable.ic_launcher_foreground)
                        )
                    }
                }

                // voice note
                message.voiceNoteUri?.let { voicePath ->
                    VoiceNotePlayer(
                        filePath = voicePath,
                        durationMs = message.voiceNoteDuration,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        isUserMessage = isUser
                    )
                }

                // text
                if (message.content.isNotBlank()) {
                    val isBlueSky = LocalAppTheme.current == AppTheme.BLUE_SKY
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isBlueSky) Color.Black.copy(alpha = 0.6f) else if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        M3Text(
                            text = message.content,
                            color = if (isBlueSky) Color.White else if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // timestamp
                M3Text(
                    text = timestamp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(
                        top = 2.dp,
                        start = if (isUser) 0.dp else 12.dp,
                        end = if (isUser) 12.dp else 0.dp
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatInputScreen(
    viewModel: JournalViewModel,
    navController: NavController
) {
    val entry = viewModel.currentEntry
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedMoods = viewModel.getSelectedMoods()
    val canEditMood = viewModel.canEditMood
    val isCurrentEntryToday = viewModel.isCurrentEntryToday
    val todaysEvents by viewModel.todaysEvents.collectAsState()

    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val hasUnsavedText = textFieldValue.text.trim().isNotEmpty() || selectedImageUris.isNotEmpty()

    var showExitConfirmation by remember { mutableStateOf(false) }
    var messageActionMenuForId by remember { mutableStateOf<String?>(null) }
    var editTextValue by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }

    var showMediaPicker by remember { mutableStateOf(false) }
    var tempImageFile by remember { mutableStateOf<File?>(null) }

    // reply state & coroutine scope
    var replyToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // inside ChatInputScreen near other remembers
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    var highlightedMessageId by remember { mutableStateOf<String?>(null) }



    // launchers
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
                val photoURI: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                takePictureLauncher.launch(photoURI)
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImageUris = selectedImageUris + uris
        }
    }

    // Voice Recording States
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) }
    
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

    // Microphone permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
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

    // Lifecycle observer - auto-save recording on pause/stop
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
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

    val isBlueSky = LocalAppTheme.current == AppTheme.BLUE_SKY

    Box(modifier = Modifier.fillMaxSize()) {
        if (isBlueSky) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.bg_journal_archive),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(6.dp),
                contentScale = ContentScale.Crop
            )
        }

    Column(modifier = Modifier.fillMaxSize()) {

        Box(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (hasUnsavedText) {
                    showExitConfirmation = true
                } else {
                    navController.popBackStack()
                }
            }) {
                M3Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Column(modifier = Modifier.weight(1f)) {
                M3Text(
                    text = if (isCurrentEntryToday) "Today's Journal" else "Journal Entry",
                    style = MaterialTheme.typography.titleLarge
                )
                M3Text(
                    text = entry.localDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            M3Text(
                text = "${entry.messages.size}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Event Banner (Option 1)
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

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        // Messages list
        Box(modifier = Modifier.weight(1f)) {
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
                    M3Icon(imageVector = Icons.Default.Create, contentDescription = null, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    M3Text(
                        text = if (isCurrentEntryToday) "Start writing..." else "No entries yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    M3Text(
                        text = if (isCurrentEntryToday) "What's on your mind?" else "Add a reflection",
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
                            confirmValueChange = { newValue ->
                                newValue == SwipeToDismissBoxValue.StartToEnd
                            }
                        )

                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
                                replyToMessage = message

                                // haptic + snackbar feedback
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Replying to message")
                                }

                                dismissState.reset()
                            }
                        }

                        val replied = entry.messages.find { it.id == message.replyToMessageId }



    // ... (inside LazyColumn items)
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = { Box(modifier = Modifier.fillMaxSize()) },
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
                                        editTextValue = message.content
                                        showOptionsDialog = true
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
        }

        // Auto-scroll to bottom when new messages added
        LaunchedEffect(entry.messages.size) {
            if (entry.messages.isNotEmpty()) {
                listState.animateScrollToItem(entry.messages.lastIndex)
            }
        }

        // Input area
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Reply preview
                replyToMessage?.let { replying ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(36.dp)
                                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                M3Text(text = "Replying", style = MaterialTheme.typography.labelSmall)
                                M3Text(text = replying.content.ifBlank { "[Image]" }, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                            IconButton(onClick = { replyToMessage = null }) {
                                M3Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel reply")
                            }
                        }
                    }
                }

                // Image preview (Horizontal List)
                if (selectedImageUris.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(selectedImageUris) { index, uri ->
                            Card(
                                modifier = Modifier.size(100.dp)
                            ) {
                                Box {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Selected image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = {
                                            selectedImageUris = selectedImageUris.toMutableList().apply { removeAt(index) }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(24.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                CircleShape
                                            )
                                    ) {
                                        M3Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Voice Recording Indicator
                if (isRecording) {
                    RecordingIndicator(
                        durationMs = recordingDuration,
                        isPaused = isPaused,
                        onPause = {
                            if (voiceRecorder.pauseRecording()) {
                                isPaused = true
                            }
                        },
                        onResume = {
                            if (voiceRecorder.resumeRecording()) {
                                isPaused = false
                            }
                        },
                        onStop = {
                            voiceRecorder.stopAndSave()
                        },
                        onCancel = {
                            voiceRecorder.cancelRecording()
                            isRecording = false
                            isPaused = false
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = {
                            M3Text(
                                if (isCurrentEntryToday) "Type a message..."
                                else "Add a reflection..."
                            )
                        },
                        maxLines = 5,
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isRecording
                    )

                    // Mic button (hidden while recording)
                    if (!isRecording) {
                        IconButton(
                            onClick = {
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
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    CircleShape
                                )
                        ) {
                            M3Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Note",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Spacer(Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = { showMediaPicker = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                CircleShape
                            ),
                        enabled = !isRecording
                    ) {
                        M3Icon(imageVector = Icons.Default.Image, contentDescription = "Attach Image")
                    }

                    Spacer(Modifier.width(8.dp))

                    val isEnabled = (textFieldValue.text.isNotBlank() || selectedImageUris.isNotEmpty()) && !isRecording
                    
                    val sendScale by animateFloatAsState(
                        targetValue = if (isEnabled) 1.1f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "sendScale"
                    )

                    IconButton(
                        onClick = {
                            val text = textFieldValue.text.trim()
                            if (text.isNotBlank() || selectedImageUris.isNotEmpty()) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                
                                // 1. Send text (if any)
                                if (text.isNotBlank()) {
                                    if (selectedImageUris.isNotEmpty()) {
                                        // Send text + 1st image
                                        viewModel.addMessageWithImage(text, selectedImageUris[0].toString(), replyTo = replyToMessage)
                                        
                                        // Send remaining images
                                        for (i in 1 until selectedImageUris.size) {
                                            viewModel.addMessageWithImage("", selectedImageUris[i].toString(), replyTo = null)
                                        }
                                    } else {
                                        // Text only
                                        viewModel.addMessageWithImage(text, null, replyTo = replyToMessage)
                                    }
                                } else {
                                    // No text, just images
                                    selectedImageUris.forEach { uri ->
                                        viewModel.addMessageWithImage("", uri.toString(), replyTo = replyToMessage)
                                    }
                                }

                                textFieldValue = TextFieldValue("")
                                selectedImageUris = emptyList()
                                tempImageFile = null
                                replyToMessage = null
                            }
                        },
                        enabled = isEnabled,
                        modifier = Modifier
                            .size(48.dp)
                            .scale(sendScale)
                            .background(
                                color = if (isEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    ) {
                        M3Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }

    // Message Options Dialog (Edit/Delete)
    if (showOptionsDialog && messageActionMenuForId != null) {
        AlertDialog(
            onDismissRequest = {
                showOptionsDialog = false
                messageActionMenuForId = null
            },
            title = { M3Text("Message Options") },
            text = {
                Column {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showOptionsDialog = false
                            showEditDialog = true
                            // messageActionMenuForId remains set
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        M3Text("Edit Message")
                    }
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showOptionsDialog = false
                            showDeleteDialog = true
                            // messageActionMenuForId remains set
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        M3Text("Delete Message", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material.TextButton(onClick = {
                    showOptionsDialog = false
                    messageActionMenuForId = null
                }) {
                    M3Text("Cancel")
                }
            }
        )
    }

    // Edit dialog
    if (showEditDialog && messageActionMenuForId != null) {
        val messageToEdit = entry.messages.find { it.id == messageActionMenuForId }
        if (messageToEdit != null) {
            AlertDialog(
                onDismissRequest = {
                    showEditDialog = false
                    messageActionMenuForId = null
                },
                title = { M3Text("Edit Message") },
                text = {
                    OutlinedTextField(
                        value = editTextValue,
                        onValueChange = { editTextValue = it },
                        label = { M3Text("Content") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                },
                confirmButton = {
                    androidx.compose.material.TextButton(
                        onClick = {
                            val trimmed = editTextValue.trim()
                            if (trimmed.isNotBlank()) {
                                viewModel.editMessage(messageToEdit.id, trimmed)
                            } else {
                                viewModel.deleteMessage(messageToEdit.id)
                            }
                            showEditDialog = false
                            messageActionMenuForId = null
                        }
                    ) {
                        M3Text("Save")
                    }
                },
                dismissButton = {
                    androidx.compose.material.TextButton(onClick = {
                        showEditDialog = false
                        messageActionMenuForId = null
                    }) {
                        M3Text("Cancel")
                    }
                }
            )
        } else {
            // Safety fallback if message not found
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
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    messageActionMenuForId = null
                },
                title = { M3Text("Delete Message") },
                text = { M3Text("Are you sure you want to delete this message?") },
                confirmButton = {
                    androidx.compose.material.TextButton(
                        onClick = {
                            viewModel.deleteMessage(messageToDelete.id)
                            showDeleteDialog = false
                            messageActionMenuForId = null
                        }
                    ) {
                        M3Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    androidx.compose.material.TextButton(onClick = {
                        showDeleteDialog = false
                        messageActionMenuForId = null
                    }) {
                        M3Text("Cancel")
                    }
                }
            )
        } else {
             // Safety fallback
            LaunchedEffect(Unit) {
                showDeleteDialog = false
                messageActionMenuForId = null
            }
        }
    }



    // Media picker dialog
    if (showMediaPicker) {
        AlertDialog(
            onDismissRequest = { showMediaPicker = false },
            title = { M3Text("Add Media") },
            text = {
                Column {
                    androidx.compose.material.TextButton(
                        onClick = {
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        M3Icon(imageVector = Icons.Default.Camera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        M3Text("Take Photo")
                    }
                    Divider()
                    androidx.compose.material.TextButton(
                        onClick = {
                            pickImageLauncher.launch("image/*")
                            showMediaPicker = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        M3Icon(imageVector = Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        M3Text("Choose from Gallery")
                    }
                }
            },
            confirmButton = {
                androidx.compose.material.TextButton(onClick = { showMediaPicker = false }) {
                    M3Text("Cancel")
                }
            }
        )
    }

    // Exit confirmation
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { M3Text("Discard Unsaved Changes?") },
            text = { M3Text("You have unsaved text. Are you sure you want to leave?") },
            confirmButton = {
                androidx.compose.material.TextButton(
                    onClick = {
                        showExitConfirmation = false
                        navController.popBackStack()
                    }
                ) {
                    M3Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material.TextButton(onClick = { showExitConfirmation = false }) {
                    M3Text("Keep Writing")
                }
            }
        )
    }
    }
}

// helper

// helper
fun createTempImageFile(context: android.content.Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
        .format(java.util.Date())
    val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
}

@Composable
fun EventBanner(event: Event) {
    var isVisible by remember { mutableStateOf(true) }

    if (isVisible) {
        val backgroundColor = when (event.type) {
            EventType.BIRTHDAY -> Color(0xFFFFD700).copy(alpha = 0.2f) // Gold
            EventType.ANNIVERSARY -> Color(0xFFFF69B4).copy(alpha = 0.2f) // Pink
            EventType.MEETING -> Color(0xFF2196F3).copy(alpha = 0.2f) // Blue
            EventType.CUSTOM -> MaterialTheme.colorScheme.surfaceVariant
        }

        val icon = when (event.type) {
            EventType.BIRTHDAY -> "🎂"
            EventType.ANNIVERSARY -> "❤️"
            EventType.MEETING -> "📅"
            EventType.CUSTOM -> "🎉"
        }

        val contentColor = MaterialTheme.colorScheme.onSurface

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                M3Text(
                    text = icon,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )

                M3Text(
                    text = event.title, // Using title as the custom message
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                IconButton(
                    onClick = { isVisible = false },
                    modifier = Modifier.size(24.dp)
                ) {
                    M3Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = contentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
