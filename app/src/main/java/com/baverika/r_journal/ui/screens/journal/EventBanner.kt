package com.baverika.r_journal.ui.screens.journal

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.data.local.entity.Event
import com.baverika.r_journal.data.local.entity.EventType

enum class BannerSeverity {
    NEUTRAL,
    POSITIVE,
    WARNING
}

@Composable
fun EventBanner(
    event: Event,
    severity: BannerSeverity = BannerSeverity.NEUTRAL
) {
    var isVisible by remember { mutableStateOf(true) }

    if (isVisible) {
        val containerColor = when (severity) {
            BannerSeverity.WARNING -> MaterialTheme.colorScheme.errorContainer
            BannerSeverity.NEUTRAL, BannerSeverity.POSITIVE -> MaterialTheme.colorScheme.tertiaryContainer
        }

        val contentColor = when (severity) {
            BannerSeverity.WARNING -> MaterialTheme.colorScheme.onErrorContainer
            BannerSeverity.NEUTRAL, BannerSeverity.POSITIVE -> MaterialTheme.colorScheme.onTertiaryContainer
        }

        val icon = when (event.type) {
            EventType.BIRTHDAY -> "🎂"
            EventType.ANNIVERSARY -> "💍"
            EventType.MEETING -> "📅"
            EventType.CUSTOM -> "🎉"
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

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
