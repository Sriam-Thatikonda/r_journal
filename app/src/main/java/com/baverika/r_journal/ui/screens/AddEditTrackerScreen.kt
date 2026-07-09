package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.data.local.entity.Tracker
import com.baverika.r_journal.ui.viewmodel.TrackerViewModel
import com.baverika.r_journal.utils.ColorUtils

private val trackerColors = listOf(
    0xFF000000, // Pure Black (default)
    0xFFF28B82, // Soft Red
    0xFFFBBC04, // Warm Orange
    0xFFFFF475, // Soft Yellow
    0xFFCCFF90, // Light Green
    0xFFA7FFEB, // Cyan
    0xFFAECBFA, // Soft Blue
    0xFFD7AEFB, // Lavender
    0xFFFDCFE8, // Soft Pink
    0xFFE6C9A8, // Beige
    0xFFE8EAED, // Light Gray
    0xFF1F1F1F  // Dark Gray
)

private val presetEmojis = listOf("🕉", "🙏", "💧", "💊", "📖", "🏋", "🚶", "☕", "🍎", "📝", "🧘", "🎨")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTrackerScreen(
    trackerId: String?,
    viewModel: TrackerViewModel,
    navController: NavController
) {
    var tracker by remember { mutableStateOf<Tracker?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(trackerId) {
        if (trackerId != null) {
            isLoading = true
            tracker = viewModel.getTrackerById(trackerId)
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Form States
    val existingTracker = tracker
    var title by remember(existingTracker) { mutableStateOf(existingTracker?.title ?: "") }
    var emoji by remember(existingTracker) { mutableStateOf(existingTracker?.emoji ?: "🕉") }
    var selectedColor by remember(existingTracker) { mutableLongStateOf(existingTracker?.color ?: 0xFF000000) }
    var goalStr by remember(existingTracker) { mutableStateOf(existingTracker?.goal?.toString() ?: "108") }
    var stepStr by remember(existingTracker) { mutableStateOf(existingTracker?.incrementStep?.toString() ?: "1") }
    var resetFrequency by remember(existingTracker) { mutableStateOf(existingTracker?.resetFrequency ?: "Never") }
    var notes by remember(existingTracker) { mutableStateOf(existingTracker?.notes ?: "") }

    var showColorPicker by remember { mutableStateOf(false) }

    val backgroundColor = Color(selectedColor)
    val textColor = ColorUtils.getContrastingTextColor(backgroundColor)
    val secondaryTextColor = ColorUtils.getSecondaryTextColor(backgroundColor)

    val isFormValid = title.isNotBlank() && emoji.isNotBlank() && goalStr.toIntOrNull() != null && stepStr.toIntOrNull() != null

    fun saveTracker() {
        if (!isFormValid) return
        val goal = goalStr.toIntOrNull() ?: 100
        val step = stepStr.toIntOrNull() ?: 1

        if (existingTracker != null) {
            val updated = existingTracker.copy(
                title = title,
                emoji = emoji,
                color = selectedColor,
                goal = goal,
                incrementStep = step,
                resetFrequency = resetFrequency,
                notes = notes.ifBlank { null },
                updatedDate = System.currentTimeMillis()
            )
            viewModel.updateTracker(updated)
        } else {
            viewModel.addTracker(
                title = title,
                emoji = emoji,
                color = selectedColor,
                goal = goal,
                incrementStep = step,
                resetFrequency = resetFrequency,
                notes = notes.ifBlank { null }
            )
        }
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingTracker == null) "New Tracker" else "Edit Tracker", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    IconButton(onClick = ::saveTracker, enabled = isFormValid) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = if (isFormValid) textColor else textColor.copy(alpha = 0.4f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title", color = secondaryTextColor) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = textColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                    focusedLabelColor = textColor,
                    unfocusedLabelColor = secondaryTextColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            // Emoji Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Emoji", color = textColor, fontWeight = FontWeight.Bold)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { if (it.length <= 4) emoji = it },
                        label = { Text("Custom Emoji", color = secondaryTextColor) },
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = textColor,
                            unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                            focusedLabelColor = textColor,
                            unfocusedLabelColor = secondaryTextColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(presetEmojis) { preset ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (emoji == preset) textColor.copy(alpha = 0.25f) else Color.Transparent)
                                    .border(1.dp, textColor.copy(alpha = 0.3f), CircleShape)
                                    .clickable { emoji = preset },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(preset, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            // Goal & Increment Step Inputs
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = goalStr,
                    onValueChange = { goalStr = it },
                    label = { Text("Target Goal", color = secondaryTextColor) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = secondaryTextColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )

                OutlinedTextField(
                    value = stepStr,
                    onValueChange = { stepStr = it },
                    label = { Text("Increment Step", color = secondaryTextColor) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = secondaryTextColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )
            }

            // Reset Frequency dropdown/segmented
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Reset Frequency", color = textColor, fontWeight = FontWeight.Bold)
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Never", "Daily", "Weekly").forEach { freq ->
                        val isSelected = resetFrequency == freq
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) textColor else Color.Transparent)
                                .border(1.dp, textColor, RoundedCornerShape(20.dp))
                                .clickable { resetFrequency = freq },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                freq,
                                color = if (isSelected) backgroundColor else textColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Color badge & picker
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Card Background Color", color = textColor, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(selectedColor))
                        .border(
                            width = 2.dp,
                            color = if (ColorUtils.isColorLight(Color(selectedColor))) Color.Gray.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .clickable { showColorPicker = !showColorPicker },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "Color",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showColorPicker) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(trackerColors) { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(
                                    width = if (color == selectedColor) 3.dp else 1.dp,
                                    color = if (color == selectedColor) textColor else Color.Gray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColor = color
                                    showColorPicker = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == selectedColor) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = ColorUtils.getContrastingTextColor(Color(color)),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Notes Input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)", color = secondaryTextColor) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = textColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                    focusedLabelColor = textColor,
                    unfocusedLabelColor = secondaryTextColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )
        }
    }
}
