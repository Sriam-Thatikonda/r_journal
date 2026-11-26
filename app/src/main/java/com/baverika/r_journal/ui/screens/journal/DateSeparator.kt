package com.baverika.r_journal.ui.screens.journal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.ui.theme.LocalDesignTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DateSeparator(dateText: String) {
    val tokens = LocalDesignTokens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = tokens.cardElevated,
            shape = CircleShape,
            border = tokens.strokeSubtle,
            tonalElevation = 0.dp
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
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
