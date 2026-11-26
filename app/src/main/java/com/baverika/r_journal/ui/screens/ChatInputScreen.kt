package com.baverika.r_journal.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.ui.screens.journal.CompactMoodPicker
import com.baverika.r_journal.ui.screens.journal.EventBanner
import com.baverika.r_journal.ui.screens.journal.JournalChatScaffold
import com.baverika.r_journal.ui.viewmodel.JournalViewModel
import java.time.LocalDate

/**
 * Daily Journal entry screen.
 * Delegates core messaging plumbing to JournalChatScaffold while providing
 * daily mood picking, event banners, and reflection prompt empty state.
 */
@Composable
fun ChatInputScreen(
    viewModel: JournalViewModel,
    navController: NavController
) {
    val moodSelection = viewModel.getMoodSelection()
    val canEditMood = viewModel.canEditMood
    val isCurrentEntryToday = viewModel.isCurrentEntryToday
    val eventBanners by viewModel.eventBanners.collectAsState()

    JournalChatScaffold(
        viewModel = viewModel,
        navController = navController,
        isCurrentEntryToday = isCurrentEntryToday,
        topHeaderContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (eventBanners.isNotEmpty()) {
                    eventBanners.forEach { item ->
                        EventBanner(item = item)
                    }
                }

                CompactMoodPicker(
                    moodSelection = moodSelection,
                    onPrimaryMoodTap = { mood -> viewModel.setPrimaryMood(mood) },
                    onSecondaryMoodToggle = { mood -> viewModel.toggleSecondaryMood(mood) },
                    onClearMoods = { viewModel.clearMoods() },
                    canEdit = canEditMood
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.5.dp
                )
            }
        },
        emptyStateContent = { onPromptSelected ->
            JournalEmptyState(
                isCurrentEntryToday = isCurrentEntryToday,
                onPromptSelected = onPromptSelected
            )
        }
    )
}

@Composable
private fun JournalEmptyState(
    isCurrentEntryToday: Boolean,
    onPromptSelected: (String) -> Unit
) {
    val prompts = remember {
        listOf(
            "What made you smile today?",
            "What are you grateful for right now?",
            "Describe a small win from today.",
            "What's occupying your mind?",
            "How did your energy feel today?",
            "What challenged you today?",
            "One thing I want to remember about today...",
            "What are you looking forward to next?"
        )
    }

    val initialIndex = remember { LocalDate.now().dayOfYear % prompts.size }
    var currentPromptIndex by remember { mutableIntStateOf(initialIndex) }
    var spinCount by remember { mutableIntStateOf(0) }
    val spinAngle by animateFloatAsState(
        targetValue = spinCount * 180f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "shuffleSpin"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCurrentEntryToday) Icons.Default.AutoAwesome else Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (isCurrentEntryToday) "Today's Reflection" else "Past Day Archive",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (isCurrentEntryToday) {
                        "Your space is clear. Capture a quick thought, photo, or voice note—or start with a prompt:"
                    } else {
                        "No messages were logged on this day. You can still add a retrospective reflection below."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (isCurrentEntryToday) {
                    val activePrompt = prompts[currentPromptIndex]

                    Surface(
                        onClick = { onPromptSelected(activePrompt) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TAP TO USE PROMPT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = activePrompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = {
                                    spinCount++
                                    currentPromptIndex = (currentPromptIndex + 1) % prompts.size
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Shuffle prompt",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = spinAngle }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
