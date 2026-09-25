package com.baverika.r_journal.ui.screens.journal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
