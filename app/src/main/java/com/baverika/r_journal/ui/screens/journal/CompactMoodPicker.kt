package com.baverika.r_journal.ui.screens.journal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MoodItem(
    val id: String,
    val emoji: String,
    val label: String,
    val color: Color
)

@Composable
fun CompactMoodPicker(
    selectedMoods: Set<String>,
    onMoodToggle: (String) -> Unit,
    canEdit: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Accessible, vibrant accent colors that remain legible across both light and dark surfaces
    val moods = remember {
        listOf(
            MoodItem("happy", "😊", "Happy", Color(0xFFF59E0B)),
            MoodItem("calm", "😌", "Calm", Color(0xFF10B981)),
            MoodItem("anxious", "😰", "Anxious", Color(0xFFF97316)),
            MoodItem("sad", "😔", "Low", Color(0xFF3B82F6)),
            MoodItem("tired", "😴", "Tired", Color(0xFFA855F7))
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!canEdit && selectedMoods.isEmpty()) Modifier.alpha(0.55f) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MOOD",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.padding(start = 16.dp, end = 8.dp)
        )

        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(moods, key = { it.id }) { item ->
                val isSelected = item.id in selectedMoods

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.04f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "moodScale_${item.id}"
                )

                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        item.color.copy(alpha = 0.22f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    label = "moodBg_${item.id}"
                )

                Surface(
                    onClick = {
                        if (canEdit) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMoodToggle(item.id)
                        }
                    },
                    enabled = canEdit,
                    shape = CircleShape,
                    color = containerColor,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) {
                            item.color.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        }
                    ),
                    modifier = Modifier.scale(scale)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = item.emoji,
                            fontSize = 14.sp
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                item.color
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}