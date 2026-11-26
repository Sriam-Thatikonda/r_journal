package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.baverika.r_journal.ui.components.HighlightedText
import com.baverika.r_journal.ui.theme.LocalDesignTokens
import com.baverika.r_journal.ui.theme.pressScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.baverika.r_journal.ui.viewmodel.SearchCategory
import com.baverika.r_journal.ui.viewmodel.SearchViewModel
import com.baverika.r_journal.ui.viewmodel.UnifiedSearchResult

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val tokens = LocalDesignTokens.current

    val searchSuggestions = listOf(
        "Today", "#gratitude", "#work", "Health", "#ideas", "Goal", "Habit"
    )

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotEmpty()) {
            viewModel.updateQuery(initialQuery)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tokens.barGlass
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
                            .clip(CircleShape)
                            .background(tokens.inputContainer)
                            .border(1.dp, tokens.borderDefault, CircleShape)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        com.baverika.r_journal.ui.components.EmptyState(
                            icon = Icons.Default.Search,
                            title = "Universal Global Search",
                            message = "Find entries across journals, notes, tasks, and trackers"
                        )

                        Text(
                            text = "QUICK SUGGESTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            searchSuggestions.forEach { tag ->
                                SuggestionChip(
                                    onClick = { viewModel.updateQuery(tag) },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }

                results.isEmpty() -> {
                    com.baverika.r_journal.ui.components.EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No Results Found",
                        message = "No matches for \"$query\". Try different keywords or switch categories."
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
                                query = query,
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
    query: String = "",
    onClick: () -> Unit
) {
    val tokens = LocalDesignTokens.current

    val (icon, badgeEmoji, tint) = when (item.category) {
        SearchCategory.JOURNAL -> Triple(Icons.Default.Book, "📖", MaterialTheme.colorScheme.primary)
        SearchCategory.NOTES -> Triple(Icons.Default.Note, "📝", MaterialTheme.colorScheme.secondary)
        SearchCategory.TASKS -> Triple(Icons.Default.CheckCircle, "☑️", MaterialTheme.colorScheme.tertiary)
        SearchCategory.TRACKERS -> Triple(Icons.Default.BarChart, "📊", MaterialTheme.colorScheme.error)
        SearchCategory.QUOTES -> Triple(Icons.Default.FormatQuote, "💬", MaterialTheme.colorScheme.primary)
        SearchCategory.ALL -> Triple(Icons.Default.Search, "🔍", MaterialTheme.colorScheme.primary)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .clickable { onClick() },
        shape = RoundedCornerShape(tokens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = tokens.cardContainer
        ),
        border = tokens.strokeSubtle,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tint.copy(alpha = 0.14f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
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
                    HighlightedText(
                        text = item.title,
                        query = query,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        highlightColor = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = tint.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$badgeEmoji ${item.category.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HighlightedText(
                        text = item.subtitle,
                        query = query,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        highlightColor = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
