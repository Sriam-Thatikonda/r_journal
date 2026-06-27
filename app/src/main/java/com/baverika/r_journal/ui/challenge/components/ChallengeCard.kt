package com.baverika.r_journal.ui.challenge.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.ui.challenge.model.Challenge

enum class ChallengeCardVariant {
    FULL, COMPACT
}

@Composable
fun ChallengeCard(
    challenge: Challenge,
    variant: ChallengeCardVariant = ChallengeCardVariant.FULL,
    onClick: () -> Unit,
    onMarkCompleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompletedToday = challenge.isCompletedToday
    val isFinished = challenge.status.name == "COMPLETED" || challenge.status.name == "ARCHIVED" || challenge.status.name == "ABANDONED" || (challenge.completedDays >= challenge.totalDays)

    val cardColors = if (isCompletedToday || isFinished) {
        CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    } else {
        CardDefaults.elevatedCardColors()
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardDefaults.elevatedShape)
            .clickable { onClick() },
        colors = cardColors,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isCompletedToday || isFinished) 1.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = challenge.emoji ?: "🎯",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = challenge.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (variant == ChallengeCardVariant.FULL && !challenge.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = challenge.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (!isFinished) {
                    IconButton(
                        onClick = onMarkCompleteClick,
                        enabled = !isCompletedToday
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Mark Complete",
                            modifier = Modifier.size(32.dp),
                            tint = if (isCompletedToday) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ChallengeProgressBar(
                completedDays = challenge.completedDays,
                totalDays = challenge.totalDays
            )
        }
    }
}
