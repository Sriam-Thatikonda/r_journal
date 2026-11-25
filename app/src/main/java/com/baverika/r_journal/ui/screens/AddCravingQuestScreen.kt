package com.baverika.r_journal.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.data.local.entity.CravingLogEntity
import com.baverika.r_journal.ui.components.UrgeSurfingDialog
import com.baverika.r_journal.ui.viewmodel.CravingQuestViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCravingQuestScreen(
    viewModel: CravingQuestViewModel,
    navController: NavController
) {
    var foodName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var showLocationSelector by remember { mutableStateOf(false) }
    var showUrgeSurfingDialog by remember { mutableStateOf(false) }
    var questLog by remember { mutableStateOf<CravingLogEntity?>(null) }

    val isJunk = remember(foodName) {
        if (foodName.length > 2) viewModel.isJunkFood(foodName) else false
    }

    val quickCravings = listOf(
        "🍫 Chocolate",
        "🍟 Salty Chips",
        "🍕 Pizza",
        "🍔 Burger",
        "🥤 Soda / Cola",
        "🍦 Ice Cream",
        "🧁 Pastry / Donut",
        "🌙 Late Night Snack"
    )

    if (showUrgeSurfingDialog) {
        UrgeSurfingDialog(onDismiss = { showUrgeSurfingDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log a Craving") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (questLog == null) {
                Text(
                    text = "What is tempting you?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Text(
                    text = "Type or tap a common craving below. If it's junk food, a mindful quest will be generated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Food Item or Craving") },
                    placeholder = { Text("e.g. Potato Chips, Chocolate Bar...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Quick Select Chips
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quick Select:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickCravings.forEach { item ->
                            val rawName = item.substringAfter(" ").trim()
                            val isSelected = foodName.equals(rawName, ignoreCase = true)

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    foodName = rawName
                                },
                                label = { Text(item) }
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = isJunk) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Junk detected! Accepting will generate a 15-minute challenge before indulging.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Urge Surfing Relief Button
                OutlinedButton(
                    onClick = { showUrgeSurfingDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Waves, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pause & Breathe (60s Urge Surf)")
                }

                Button(
                    onClick = {
                        if (isJunk) {
                            showLocationSelector = true
                        } else if (foodName.isNotBlank()) {
                            viewModel.addSimpleCraving(foodName) {
                                navController.popBackStack()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = foodName.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isJunk) "Accept Quest & Delay" else "Log Entry",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                QuestResultView(questLog!!, onDismiss = { navController.popBackStack() })
            }
        }
    }

    if (showLocationSelector) {
        LocationSelectorDialog(
            onLocationSelected = { loc ->
                location = loc
                showLocationSelector = false
                viewModel.addCraving(foodName, loc) { log ->
                    questLog = log
                }
            },
            onDismiss = { showLocationSelector = false }
        )
    }
}

@Composable
fun LocationSelectorDialog(
    onLocationSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Where are you right now?") },
        text = {
            Text("Quests are tailored to your current physical surroundings so they are easy to execute immediately.")
        },
        confirmButton = {
            Button(onClick = { onLocationSelected("Indoor") }) {
                Text("🏠 Indoor")
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = { onLocationSelected("Outdoor") }) {
                Text("🌳 Outdoor")
            }
        }
    )
}

@Composable
fun QuestResultView(
    log: CravingLogEntity,
    onDismiss: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            text = "Quest Assigned!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${log.difficulty.uppercase()} DIFFICULTY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = log.quest,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Got it — Challenge Accepted!")
        }
    }
}
