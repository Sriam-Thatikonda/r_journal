package com.baverika.r_journal.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.data.local.entity.CravingLogEntity
import com.baverika.r_journal.ui.viewmodel.CravingQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCravingQuestScreen(
    viewModel: CravingQuestViewModel,
    navController: NavController
) {
    var foodName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") } // "Indoor" or "Outdoor"
    var showLocationSelector by remember { mutableStateOf(false) }
    var questLog by remember { mutableStateOf<CravingLogEntity?>(null) }
    
    val isJunk = remember(foodName) { 
        if (foodName.length > 2) viewModel.isJunkFood(foodName) else false 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Craving") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (questLog == null) {
                Text(
                    text = "What are you craving?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Food Item") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = isJunk) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Junk detected! A quest will be assigned.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { 
                        if (isJunk) {
                            showLocationSelector = true 
                        } else {
                            // Non-junk tracking or simple log
                            // For simplicity in this feature, let's say every entry gets a quest if it matches the keyword.
                            // If they enter "Apple", maybe no quest.
                            if (foodName.isNotBlank()) {
                                navController.popBackStack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = foodName.isNotBlank()
                ) {
                    Text(if (isJunk) "Accept Challenge" else "Log Entry")
                }
            } else {
                // Quest Result View
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
        title = { Text("Where are you?") },
        text = { Text("Quests are tailored to your current environment.") },
        confirmButton = {
            TextButton(onClick = { onLocationSelected("Indoor") }) {
                Text("Indoor")
            }
        },
        dismissButton = {
            TextButton(onClick = { onLocationSelected("Outdoor") }) {
                Text("Outdoor")
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
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Quest Assigned!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = log.difficulty.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = log.quest,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Got it!")
        }
    }
}
