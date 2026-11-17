package com.baverika.r_journal.ui.screens.journal

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.baverika.r_journal.R
import com.baverika.r_journal.data.local.entity.ChatMessage
import com.baverika.r_journal.ui.components.VoiceNotePlayer
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.LocalAppTheme
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: ChatMessage,
    isCurrentEntryToday: Boolean = true,
    isAddedLater: Boolean = false,
    navController: NavController,
    onLongClick: (() -> Unit)? = null,
    repliedMessage: ChatMessage? = null,
    onQuoteClick: (() -> Unit)? = null,
    isHighlighted: Boolean = false
) {
    val isUser = message.role == "user"
    val currentTheme = LocalAppTheme.current
    val isBlueSky = currentTheme == AppTheme.BLUE_SKY
    val isMidnight = currentTheme == AppTheme.MIDNIGHT
    val isSpace = currentTheme == AppTheme.ROSEWOOD

    val timestamp = remember(message.timestamp) {
        LocalDateTime
            .ofInstant(Instant.ofEpochMilli(message.timestamp), ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    val highlightColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 350),
        label = "quoteHighlight"
    )

    val bubbleContainerColor = when {
        isSpace -> if (isUser) Color(0xCC1A1C22) else Color(0xB3101216)
        isBlueSky -> Color.Black.copy(alpha = 0.62f)
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        isSpace -> Color(0xFFF3F4F6)
        isBlueSky -> Color(0xFFECECEC)
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val timeColor = when {
        isSpace -> Color(0xFF9CA3AF)
        isBlueSky -> Color(0xFFD6D6D6).copy(alpha = 0.7f)
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    }

    val bubbleBorder = when {
        isSpace -> BorderStroke(1.dp, if (isUser) Color(0x38FFFFFF) else Color(0x26FFFFFF))
        isMidnight -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        else -> null
    }

    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isUser) 20.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 20.dp
    )

    Row(
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(highlightColor)
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isAddedLater) {
                val addedDate = remember(message.timestamp) {
                    LocalDateTime
                        .ofInstant(Instant.ofEpochMilli(message.timestamp), ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                }
                Text(
                    text = "Added $addedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            // Unified Message Bubble Container
            Surface(
                shape = bubbleShape,
                color = bubbleContainerColor,
                border = bubbleBorder,
                tonalElevation = if (isUser) 1.dp else 0.dp,
                modifier = Modifier
                    .widthIn(min = 84.dp, max = 300.dp)
                    .clip(bubbleShape)
                    .then(
                        if (onLongClick != null) {
                            Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = onLongClick
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    // 1. Embedded Quoted Reply Block
                    repliedMessage?.let { original ->
                        val previewLabel = when {
                            original.content.isNotBlank() -> original.content
                            original.voiceNoteUri != null -> "🎤 Voice Note"
                            original.imageUri != null -> "📷 Photo"
                            else -> "Attachment"
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = if (isSpace || isBlueSky) 0.2f else 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onQuoteClick?.invoke() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = if (original.role == "user") "You" else original.role.replaceFirstChar { it.uppercaseChar() },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = previewLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor.copy(alpha = 0.85f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // 2. Attached Image
                    message.imageUri?.let { imagePath ->
                        val hasCaption = message.content.isNotBlank()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp, max = 260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = {
                                        val encodedPath = URLEncoder.encode(
                                            imagePath,
                                            StandardCharsets.UTF_8.toString()
                                        )
                                        navController.navigate("image_viewer/$encodedPath")
                                    },
                                    onLongClick = onLongClick
                                )
                        ) {
                            AsyncImage(
                                model = Uri.fromFile(File(imagePath)),
                                contentDescription = "Attached Image",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                                error = painterResource(R.drawable.ic_launcher_foreground)
                            )

                            // Only show floating timestamp pill on image if there is no text caption below it
                            if (!hasCaption) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = timestamp,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Voice Note Attachment
                    message.voiceNoteUri?.let { voicePath ->
                        Box(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            VoiceNotePlayer(
                                filePath = voicePath,
                                durationMs = message.voiceNoteDuration,
                                modifier = Modifier,
                                isUserMessage = isUser
                            )
                            if (message.content.isBlank()) {
                                Text(
                                    text = timestamp,
                                    color = timeColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 8.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }

                    // 4. Text Content + Bottom-End Timestamp
                    if (message.content.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .padding(
                                    start = 10.dp,
                                    end = 10.dp,
                                    top = if (message.imageUri != null || repliedMessage != null) 6.dp else 4.dp,
                                    bottom = 4.dp
                                )
                        ) {
                            Text(
                                text = message.content,
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 20.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = timestamp,
                                color = timeColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
    }
}