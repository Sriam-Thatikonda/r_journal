package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.ui.viewmodel.SearchViewModel
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    navController: NavController
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateQuery(it) },
            label = { Text("Search journals") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            // No search query entered yet
            query.isBlank() -> {
                com.baverika.r_journal.ui.components.EmptyState(
                    icon = Icons.Default.Search,
                    title = "Search Your Journals",
                    message = "Find entries by keywords, moods, or tags"
                )
            }

            // Search performed but no results
            query.isNotBlank() && results.isEmpty() -> {
                com.baverika.r_journal.ui.components.EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No Results Found",
                    message = "Try different keywords or check your spelling"
                )
            }

            // Results found
            else -> {
                Text(
                    text = "${results.size} ${if (results.size == 1) "result" else "results"} found",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(results) { entry ->
                        SearchResultCard(
                            entry = entry,
                            onClick = {
                                navController.navigate("chat_input/${entry.id}")
                            },
                            query = query
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchResultCard(entry: JournalEntry, onClick: () -> Unit, query: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Date
            Text(
                text = entry.localDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Tags
            if (entry.tags.isNotEmpty()) {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    entry.tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(text = tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Preview text - show matching message if possible
            val previewText = if (query.isNotEmpty()) {
                entry.messages.firstOrNull { msg ->
                    msg.content.contains(query, ignoreCase = true)
                }?.content
            } else {
                entry.messages.firstOrNull()?.content
            }

            if (previewText != null) {
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "No messages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}