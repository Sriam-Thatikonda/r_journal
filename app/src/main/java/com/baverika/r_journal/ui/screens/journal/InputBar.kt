package com.baverika.r_journal.ui.screens.journal

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.baverika.r_journal.data.local.entity.ChatMessage
import com.baverika.r_journal.ui.components.RecordingIndicator
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.LocalAppTheme

/**
 * The bottom input area for the journal chat screen.
 * Contains: animated reply preview, compact image tray, in-place recording swap,
 * pill composer with embedded media button, and primary Mic/Send action button.
 */
@Composable
fun InputBar(
    textFieldValue: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    isCurrentEntryToday: Boolean,
    isRecording: Boolean,
    isPaused: Boolean,
    recordingDuration: Long,
    selectedImageUris: List<Uri>,
    replyToMessage: ChatMessage?,
    onSend: () -> Unit,
    onMediaPickerOpen: () -> Unit,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onCancelReply: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSpace = LocalAppTheme.current == AppTheme.ROSEWOOD

    Surface(
        color = if (isSpace) Color(0xE608090C) else MaterialTheme.colorScheme.surface,
        border = if (isSpace) BorderStroke(0.5.dp, Color(0x26FFFFFF)) else null,
        tonalElevation = if (isSpace) 0.dp else 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Animated Reply Banner
            AnimatedVisibility(
                visible = replyToMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                replyToMessage?.let { replying ->
                    val replyPreview = when {
                        replying.content.isNotBlank() -> replying.content
                        replying.voiceNoteUri != null -> "🎤 Voice Note"
                        replying.imageUri != null -> "📷 Photo"
                        else -> "Attachment"
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isSpace) Color(0xFF14171F) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .padding(end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.Default.Reply,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Replying to entry",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = replyPreview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSpace) Color(0xFFD1D5DB) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = onCancelReply,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Animated Selected Image Thumbnail Strip
            AnimatedVisibility(
                visible = selectedImageUris.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    itemsIndexed(selectedImageUris) { index, uri ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                onClick = { onRemoveImage(index) },
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove image",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. In-Place Swap: Voice Recording Bar vs. Text Composer Row
            AnimatedContent(
                targetState = isRecording,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 2 })
                        .togetherWith(fadeOut() + slideOutVertically { it / 2 })
                },
                label = "ComposerRecordingSwap"
            ) { recordingActive ->
                if (recordingActive) {
                    RecordingIndicator(
                        durationMs = recordingDuration,
                        isPaused = isPaused,
                        onPause = onPauseRecording,
                        onResume = onResumeRecording,
                        onStop = onStopRecording,
                        onCancel = onCancelRecording,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Integrated Capsule Text Field with Embedded Attachment Button
                        OutlinedTextField(
                            value = textFieldValue,
                            onValueChange = onTextChange,
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    text = if (isCurrentEntryToday) "Write a thought..." else "Add a reflection...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = onMediaPickerOpen) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Attach Image",
                                        tint = if (selectedImageUris.isNotEmpty()) {
                                            MaterialTheme.colorScheme.primary
                                        } else if (isSpace) {
                                            Color(0xFF9CA3AF)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            },
                            maxLines = 5,
                            shape = RoundedCornerShape(26.dp),
                            colors = if (isSpace) {
                                OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF14161B),
                                    unfocusedContainerColor = Color(0xFF101216),
                                    focusedBorderColor = Color(0x55FFFFFF),
                                    unfocusedBorderColor = Color(0x22FFFFFF),
                                    focusedTextColor = Color(0xFFF3F4F6),
                                    unfocusedTextColor = Color(0xFFF3F4F6),
                                    focusedPlaceholderColor = Color(0xFF9CA3AF),
                                    unfocusedPlaceholderColor = Color(0xFF6B7280),
                                    cursorColor = Color(0xFFEEEEEE)
                                )
                            } else {
                                OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            }
                        )

                        // Primary Action Button: Smoothly morphs between Mic and Send
                        val hasContent = textFieldValue.text.isNotBlank() || selectedImageUris.isNotEmpty()
                        AnimatedContent(
                            targetState = hasContent,
                            transitionSpec = {
                                (fadeIn() + scaleIn(
                                    initialScale = 0.75f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )).togetherWith(fadeOut() + scaleOut(targetScale = 0.75f))
                            },
                            label = "MicSendSwap"
                        ) { canSend ->
                            if (canSend) {
                                IconButton(
                                    onClick = onSend,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .then(
                                            if (isSpace) Modifier.border(1.dp, Color(0x66FFFFFF), CircleShape) else Modifier
                                        )
                                        .background(
                                            color = if (isSpace) Color(0xFFEDEDED) else MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = if (isSpace) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = onStartRecording,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .then(
                                            if (isSpace) Modifier.border(1.dp, Color(0x33FFFFFF), CircleShape) else Modifier
                                        )
                                        .background(
                                            color = if (isSpace) Color(0x26FFFFFF) else MaterialTheme.colorScheme.secondaryContainer,
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Record Voice Note",
                                        tint = if (isSpace) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(24.dp)
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