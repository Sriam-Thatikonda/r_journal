package com.baverika.r_journal.ui.screens.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.data.local.entity.Event
import com.baverika.r_journal.ui.viewmodel.EventDisplayItem

enum class BannerSeverity {
    NEUTRAL,
    POSITIVE,
    WARNING
}

@Composable
fun EventBanner(
    item: EventDisplayItem,
    severity: BannerSeverity = BannerSeverity.NEUTRAL
) {
    val event = item.event
    var isVisible by remember { mutableStateOf(true) }

    if (isVisible) {
        val containerColor = if (item.isTomorrow) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            when (severity) {
                BannerSeverity.WARNING -> MaterialTheme.colorScheme.errorContainer
                BannerSeverity.NEUTRAL, BannerSeverity.POSITIVE -> MaterialTheme.colorScheme.tertiaryContainer
            }
        }

        val contentColor = if (item.isTomorrow) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            when (severity) {
                BannerSeverity.WARNING -> MaterialTheme.colorScheme.onErrorContainer
                BannerSeverity.NEUTRAL, BannerSeverity.POSITIVE -> MaterialTheme.colorScheme.onTertiaryContainer
            }
        }

        val labelTag = if (item.isTomorrow) "TOMORROW" else "TODAY"

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (item.isTomorrow) Icons.Default.NotificationsActive else Icons.Default.Event,
                    contentDescription = "Event",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = contentColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = labelTag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (item.isTomorrow) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Added to tasks",
                                    tint = contentColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Added to Tasks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { isVisible = false },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
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

// Backwards-compatible overload for raw Event object
@Composable
fun EventBanner(
    event: Event,
    severity: BannerSeverity = BannerSeverity.NEUTRAL
) {
    EventBanner(
        item = EventDisplayItem(event = event, isToday = true, isTomorrow = false),
        severity = severity
    )
}
