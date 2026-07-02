package com.baverika.r_journal.ui.challenge.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.ui.challenge.components.ChallengeCard
import com.baverika.r_journal.ui.challenge.components.ChallengeCardVariant
import com.baverika.r_journal.ui.challenge.viewmodel.ChallengeHistoryUiState
import com.baverika.r_journal.ui.challenge.viewmodel.ChallengeHistoryViewModel

@Composable
fun ChallengeHistoryScreen(
    viewModel: ChallengeHistoryViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ChallengeHistoryUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ChallengeHistoryUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ChallengeHistoryUiState.Success -> {
                if (state.historyChallenges.isEmpty()) {
                    Text(
                        text = "No history yet.\nCompleted and abandoned challenges will appear here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.historyChallenges, key = { it.id }) { challenge ->
                            ChallengeCard(
                                challenge = challenge,
                                variant = ChallengeCardVariant.COMPACT,
                                onClick = { navController.navigate("challenge_detail/${challenge.id}") },
                                onMarkCompleteClick = { } // Actions are disabled for history items in the card itself
                            )
                        }
                    }
                }
            }
        }
    }
}
