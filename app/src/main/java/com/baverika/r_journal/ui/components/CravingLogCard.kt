package com.baverika.r_journal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.data.local.entity.CravingLogEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.selection.toggleable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CravingLogCard(
    log: CravingLogEntity,
    isReadOnly: Boolean,
    onQuestToggle: () -> Unit,
    onFoodToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("d MMM • h:mm a")
    val timeStr = Instant.ofEpochMilli(log.createdAt)
        .atZone(ZoneId.systemDefault())
        .format(formatter)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = log.food,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                DifficultyBadge(log.difficulty)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Quest: ${log.quest}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusToggle(
                    label = "Quest",
                    isChecked = log.questCompleted,
                    enabled = !isReadOnly,
                    onToggle = onQuestToggle
                )
                Spacer(modifier = Modifier.width(16.dp))
                StatusToggle(
                    label = "Ate",
                    isChecked = log.foodEaten,
                    enabled = !isReadOnly,
                    onToggle = onFoodToggle
                )
            }
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: String) {
    val color = when (difficulty) {
        "Easy" -> Color(0xFF4CAF50)
        "Medium" -> Color(0xFFFFC107)
        "Hard" -> Color(0xFFFF9800)
        "Boss" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        contentColor = color,
        shape = FilterChipDefaults.shape,
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = color, selectedBorderColor = color, borderWidth = 1.dp)
    ) {
        Text(
            text = difficulty,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusToggle(
    label: String,
    isChecked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = enabled) { onToggle() }
    ) {
        Icon(
            imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = label,
            tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isChecked) {
             Icon(
                 imageVector = Icons.Filled.Check,
                 contentDescription = null,
                 tint = MaterialTheme.colorScheme.primary,
                 modifier = Modifier.size(16.dp)
             )
        }
    }
}

// Helper extension for clickable without ripple if needed, but standard is fine
@Composable
private fun Modifier.clickable(enabled: Boolean, onClick: () -> Unit): Modifier =
    this.then(Modifier.padding(4.dp)).then(if (enabled) Modifier.toggleable(value = false, onValueChange = { onClick() }) else Modifier)


