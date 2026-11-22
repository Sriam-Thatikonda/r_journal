package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.LocalAppTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.ui.viewmodel.SearchCategory
import com.baverika.r_journal.ui.viewmodel.SearchViewModel
import com.baverika.r_journal.ui.viewmodel.UnifiedSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    navController: NavController,
    initialQuery: String = ""
) {
    val query by viewModel.query.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val isSpace = LocalAppTheme.current == AppTheme.ROSEWOOD

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotEmpty()) {
            viewModel.updateQuery(initialQuery)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isSpace) Color(0xE6000000) else MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSpace) Modifier
                                    .background(Color(0x8014161B), RoundedCornerShape(20.dp))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                else Modifier
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search everywhere...",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { viewModel.updateQuery(it) },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SearchCategory.values()) { cat ->
                    FilterChip(
                        selected = cat == selectedCategory,
                        onClick = { viewModel.selectCategory(cat) },
                        label = { Text(cat.label) },
                        leadingIcon = if (cat == selectedCategory) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                query.isBlank() -> {
                    com.baverika.r_journal.ui.components.EmptyState(
                        icon = Icons.Default.Search,
                        title = "Universal Global Search",
                        message = "Search journals, notes, tasks, trackers, and quotes"
                    )
                }

                results.isEmpty() -> {
                    com.baverika.r_journal.ui.components.EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No Results Found",
                        message = "Try different keywords or switch categories"
                    )
                }

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
                        items(results, key = { "${it.category.name}_${it.id}" }) { item ->
                            UnifiedSearchResultCard(
                                item = item,
                                onClick = {
                                    when (item) {
                                        is UnifiedSearchResult.JournalResult -> navController.navigate(
                                            "chat_input/${item.entry.id}"
                                        )

                                        is UnifiedSearchResult.NoteResult -> navController.navigate(
                                            "edit_quick_note/${item.note.id}"
                                        )

                                        is UnifiedSearchResult.TaskResult -> navController.navigate(
                                            "edit_task/${item.task.id}"
                                        )

                                        is UnifiedSearchResult.TrackerResult -> navController.navigate(
                                            "tracker_details/${item.tracker.id}"
                                        )

                                        is UnifiedSearchResult.QuoteResult -> navController.navigate(
                                            "quotes"
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnifiedSearchResultCard(
        item: UnifiedSearchResult,
        onClick: () -> Unit
    ) {
        val isSpace = LocalAppTheme.current == AppTheme.ROSEWOOD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(if (isSpace) 18.dp else 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSpace) Color(0xB3101216) else MaterialTheme.colorScheme.surface
            ),
            border = if (isSpace) BorderStroke(1.dp, Color(0x2EFFFFFF)) else null,
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSpace) 1.dp else 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (icon, tint) = when (item.category) {
                    SearchCategory.JOURNAL -> Icons.Default.Book to MaterialTheme.colorScheme.primary
                    SearchCategory.NOTES -> Icons.Default.Note to MaterialTheme.colorScheme.secondary
                    SearchCategory.TASKS -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.tertiary
                    SearchCategory.TRACKERS -> Icons.Default.BarChart to MaterialTheme.colorScheme.error
                    SearchCategory.QUOTES -> Icons.Default.FormatQuote to MaterialTheme.colorScheme.primary
                    SearchCategory.ALL -> Icons.Default.Search to MaterialTheme.colorScheme.primary
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSpace) Color(0x26FFFFFF) else tint.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (isSpace) Color(0xFFF3F4F6) else tint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (item.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }