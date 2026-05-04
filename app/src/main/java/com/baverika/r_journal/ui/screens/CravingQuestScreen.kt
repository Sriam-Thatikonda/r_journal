package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.ui.components.CravingLogCard
import com.baverika.r_journal.ui.viewmodel.CravingQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CravingQuestScreen(
    viewModel: CravingQuestViewModel,
    navController: NavController
) {
    val logs by viewModel.uiState.collectAsState()
    var todayStats by remember { mutableStateOf(0 to "Easy") }

    LaunchedEffect(logs) {
        todayStats = viewModel.getTodayStats()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_craving") }) {
                Icon(Icons.Default.Add, contentDescription = "Log Craving")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Today's Stats Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Today's Cravings",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "${todayStats.first} entries",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Current Difficulty",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = todayStats.second,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No cravings logged yet.\nTap + to start a quest.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(logs) { log ->
                        CravingLogCard(
                            log = log,
                            isReadOnly = viewModel.isReadOnly(log),
                            onQuestToggle = { viewModel.toggleQuestCompleted(log) },
                            onFoodToggle = { viewModel.toggleFoodEaten(log) },
                            onClick = { navController.navigate("craving_detail/${log.id}") }
                        )
                    }
                }
            }
        }
    }
}
