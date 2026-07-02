package com.baverika.r_journal.ui.challenge.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.ui.challenge.components.ChallengeCard
import com.baverika.r_journal.ui.challenge.components.ChallengeCardVariant
import com.baverika.r_journal.ui.challenge.viewmodel.ChallengeDetailUiEvent
import com.baverika.r_journal.ui.challenge.viewmodel.ChallengeDetailUiState
import com.baverika.r_journal.ui.challenge.viewmodel.ChallengeDetailViewModel

@Composable
fun ChallengeDetailScreen(
    viewModel: ChallengeDetailViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ChallengeDetailUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ChallengeDetailUiEvent.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ChallengeDetailUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ChallengeDetailUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ChallengeDetailUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    ChallengeCard(
                        challenge = state.challenge,
                        variant = ChallengeCardVariant.FULL,
                        onClick = { },
                        onMarkCompleteClick = viewModel::markChallengeCompleteToday
                    )

                    HorizontalDivider()
                    
                    Text(
                        text = "Actions",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedButton(
                        onClick = viewModel::abandonChallenge,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Abandon Challenge")
                    }

                    var showDeleteDialog by remember { mutableStateOf(false) }

                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Challenge")
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Challenge") },
                            text = { Text("Are you sure you want to delete this challenge? This action cannot be undone.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDeleteDialog = false
                                        viewModel.deleteChallenge()
                                    }
                                ) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
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
