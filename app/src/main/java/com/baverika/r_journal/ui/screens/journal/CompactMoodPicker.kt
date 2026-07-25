package com.baverika.r_journal.ui.screens.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class MoodItem(
    val id: String,
    val emoji: String,
    val label: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactMoodPicker(
    selectedMoods: Set<String>,
    onMoodToggle: (String) -> Unit,
    canEdit: Boolean,
    modifier: Modifier = Modifier
) {
    val moods = listOf(
        MoodItem("happy", "😊", "happy", Color(0xFFFFD700)),
        MoodItem("calm", "😌", "calm", Color(0xFF81C784)),
        MoodItem("anxious", "😰", "anxious", Color(0xFFFFB74D)),
        MoodItem("sad", "😔", "low", Color(0xFF64B5F6)),
        MoodItem("tired", "😴", "tired", Color(0xFFBA68C8))
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!canEdit) Modifier.alpha(0.5f) else Modifier),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(moods, key = { it.id }) { item ->
            val isSelected = item.id in selectedMoods
            FilterChip(
                selected = isSelected,
                onClick = { if (canEdit) onMoodToggle(item.id) },
                label = {
                    Text(text = "${item.emoji} ${item.label}")
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = item.color.copy(alpha = 0.2f),
                    selectedLabelColor = item.color
                ),
                enabled = canEdit || isSelected
            )
        }
    }
}
