package com.baverika.r_journal.ui.screens.journal

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
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
                    Text(
                        text = "Added ${LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(message.timestamp), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                // quoted reply (if any)
                repliedMessage?.let { original ->
                    Surface(
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, topStart = 0.dp, bottomStart = 0.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .fillMaxWidth(0.82f)
                            .clickable { onQuoteClick?.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 0.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .heightIn(min = 36.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (original.role == "user") "Replying to you" else original.role.replaceFirstChar { it.uppercaseChar() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = original.content.ifBlank { "[Image]" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // image attachment with embedded timestamp pill
                message.imageUri?.let { imagePath ->
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .size(220.dp)
                            .clickable {
                                val encodedPath = URLEncoder.encode(
                                    imagePath,
                                    StandardCharsets.UTF_8.toString()
                                )
                                navController.navigate("image_viewer/$encodedPath")
                            },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = Uri.fromFile(File(imagePath)),
                                contentDescription = "Attached Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                                error = painterResource(R.drawable.ic_launcher_foreground)
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = timestamp,
                                    color = Color(0xFFD6D6D6),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // voice note with embedded timestamp
                message.voiceNoteUri?.let { voicePath ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        VoiceNotePlayer(
                            filePath = voicePath,
                            durationMs = message.voiceNoteDuration,
                            modifier = Modifier,
                            isUserMessage = isUser
                        )
                        Text(
                            text = timestamp,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 6.dp)
                        )
                    }
                }

                // text bubble with inline/bottom-end timestamp (WhatsApp / Telegram style)
                if (message.content.isNotBlank()) {
                    val currentTheme = LocalAppTheme.current
                    val isBlueSky = currentTheme == AppTheme.BLUE_SKY
                    val isMidnight = currentTheme == AppTheme.MIDNIGHT
                    val textColor = if (isBlueSky) Color(0xFFD6D6D6) else if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    val timeColor = if (isBlueSky) Color(0xFFD6D6D6).copy(alpha = 0.7f) else if (isUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        color = if (isBlueSky) Color.Black.copy(alpha = 0.6f) else if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isMidnight) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) else null,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 6.dp)
                        ) {
                            Text(
                                text = message.content,
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = timestamp,
                                color = timeColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
