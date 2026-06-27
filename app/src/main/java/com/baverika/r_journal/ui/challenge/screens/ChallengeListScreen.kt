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
import com.baverika.r_journal.ui.challenge.viewmodel.ChallengeListUiEvent
import com.baverika.r_journal.ui.challenge.viewmodel.ChallengeListUiState
import com.baverika.r_journal.ui.challenge.viewmodel.ChallengeListViewModel

@Composable
fun ChallengeListScreen(
    viewModel: ChallengeListViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ChallengeListUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ChallengeListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ChallengeListUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ChallengeListUiState.Success -> {
                if (state.activeChallenges.isEmpty()) {
                    Text(
                        text = "No active challenges.\nTap + to start one!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.activeChallenges, key = { it.id }) { challenge ->
                            ChallengeCard(
                                challenge = challenge,
                                onClick = { navController.navigate("challenge_detail/${challenge.id}") },
                                onMarkCompleteClick = { viewModel.markChallengeCompleteToday(challenge.id) }
                            )
                        }
                    }
                }
            }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
