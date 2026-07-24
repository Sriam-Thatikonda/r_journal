package com.baverika.r_journal.ui.screens.journal

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.baverika.r_journal.data.local.entity.ChatMessage
import com.baverika.r_journal.ui.components.RecordingIndicator

/**
 * The bottom input area for the journal chat screen.
 * Contains: reply preview, image previews, recording indicator, text field, and action buttons.
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
    val isEnabled = (textFieldValue.text.isNotBlank() || selectedImageUris.isNotEmpty()) && !isRecording

    Surface(
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
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
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Replying", style = MaterialTheme.typography.labelSmall)
                            Text(text = replying.content.ifBlank { "[Image]" }, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                        IconButton(onClick = onCancelReply) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel reply")
                        }
                    }
                }
            }

            // Image preview (Horizontal List)
            if (selectedImageUris.isNotEmpty()) {
                LazyRow(
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
                                    onClick = { onRemoveImage(index) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(24.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
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
                    onPause = onPauseRecording,
                    onResume = onResumeRecording,
                    onStop = onStopRecording,
                    onCancel = onCancelRecording,
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
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = {
                        Text(
                            if (isCurrentEntryToday) "Type a message..."
                            else "Add a reflection..."
                        )
                    },
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp),
                    enabled = !isRecording
                )

                // Media attach button (Camera / Gallery)
                IconButton(
                    onClick = onMediaPickerOpen,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                            CircleShape
                        ),
                    enabled = !isRecording
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Attach Image",
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(6.dp))

                // Dynamic Mic vs Send button swap (WhatsApp / Telegram style)
                val hasContent = isEnabled
                AnimatedContent(
                    targetState = hasContent,
                    transitionSpec = {
                        (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                    },
                    label = "MicSendSwap"
                ) { canSend ->
                    if (canSend) {
                        val sendScale by animateFloatAsState(
                            targetValue = 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "sendScale"
                        )
                        IconButton(
                            onClick = onSend,
                            modifier = Modifier
                                .size(44.dp)
                                .scale(sendScale)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else if (!isRecording) {
                        IconButton(
                            onClick = onStartRecording,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Note",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
