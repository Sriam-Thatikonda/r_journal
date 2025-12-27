package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.ui.viewmodel.HabitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun AddEditHabitScreen(
    viewModel: HabitViewModel,
    navController: NavController,
    habitId: String? = null
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFFFF6B9D.toInt()) }
    // Default: All days selected
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }

    // Load habit if editing
    LaunchedEffect(habitId) {
        if (habitId != null) {
            val habit = viewModel.getHabitById(habitId)
            if (habit != null) {
                title = habit.title
                description = habit.description
                selectedColor = habit.color
                selectedDays = habit.frequency.toSet()
            }
        }
    }

    val colors = listOf(
        0xFFFF6B9D.toInt(), // Vibrant Pink
        0xFF4ECDC4.toInt(), // Turquoise
        0xFFFFA07A.toInt(), // Light Salmon
        0xFF98D8C8.toInt(), // Mint Green
        0xFF6C5CE7.toInt(), // Soft Purple
        0xFFFD79A8.toInt(), // Hot Pink
        0xFF00D2FF.toInt(), // Bright Cyan
        0xFFFFBE76.toInt(), // Peach
        0xFFFF7979.toInt(), // Coral Red
        0xFF95E1D3.toInt()  // Aqua Green
    )

    val daysOfWeek = listOf(
        "M" to 1, "T" to 2, "W" to 3, "T" to 4, "F" to 5, "S" to 6, "S" to 7
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (habitId == null) "New Habit" else "Edit Habit") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (habitId != null) {
                        IconButton(onClick = {
                            viewModel.deleteHabitById(habitId)
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    TextButton(onClick = {
                        if (title.isNotBlank()) {
                            if (habitId == null) {
                                viewModel.addHabit(
                                    title = title,
                                    description = description,
                                    frequency = selectedDays.toList(),
                                    color = selectedColor
                                )
                            } else {
                                viewModel.updateHabit(
                                    id = habitId,
                                    title = title,
                                    description = description,
                                    frequency = selectedDays.toList(),
                                    color = selectedColor
                                )
                            }
                            navController.popBackStack()
                        }
                    }) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Habit Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Frequency Selector
            Column {
                Text("Frequency", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysOfWeek.forEach { (label, dayValue) ->
                        val isSelected = selectedDays.contains(dayValue)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    selectedDays = if (isSelected) {
                                        if (selectedDays.size > 1) selectedDays - dayValue else selectedDays
                                    } else {
                                        selectedDays + dayValue
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Color Selector
            Column {
                Text("Color", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    colors.forEach { colorInt ->
                        val isSelected = selectedColor == colorInt
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .clickable { selectedColor = colorInt }
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
