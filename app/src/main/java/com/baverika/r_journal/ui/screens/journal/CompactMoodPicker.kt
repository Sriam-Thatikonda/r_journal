package com.baverika.r_journal.ui.screens.journal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baverika.r_journal.data.model.MoodCatalog
import com.baverika.r_journal.data.model.MoodDefinition
import com.baverika.r_journal.data.model.MoodSelectionState
import com.baverika.r_journal.ui.theme.JetBrainsMonoFamily
import com.baverika.r_journal.ui.theme.LocalAppTheme
import com.baverika.r_journal.ui.theme.LocalDesignTokens
import com.baverika.r_journal.ui.theme.MotionSpec
import com.baverika.r_journal.ui.theme.pressScale

/**
 * Zero-scroll hybrid mood selector:
 * - Inline full-width 6-slot orb bar (5 core moods + More/Excited orb) with 1-tap Primary toggle.
 * - Expandable Mood Palette bottom sheet for choosing Primary Mood + up to 2 Secondary Feelings.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CompactMoodPicker(
    moodSelection: MoodSelectionState,
    onPrimaryMoodTap: (String) -> Unit,
    onSecondaryMoodToggle: (String) -> Unit,
    onClearMoods: () -> Unit,
    canEdit: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val tokens = LocalDesignTokens.current
    val isDark = LocalAppTheme.current.isDark
    var showMoodSheet by remember { mutableStateOf(false) }

    val primaryDef = remember(moodSelection.primaryMood) {
        MoodCatalog.findById(moodSelection.primaryMood)
    }
    val ambientTint by animateColorAsState(
        targetValue = primaryDef?.accentColor(isDark)?.copy(alpha = if (isDark) 0.08f else 0.06f)
            ?: Color.Transparent,
        animationSpec = tween(320),
        label = "moodHeaderTint"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ambientTint)
            .then(if (!canEdit && !moodSelection.hasSelection) Modifier.alpha(0.55f) else Modifier)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Zero-scroll 6-column equal-weight dock: 5 core moods + More/Excited
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoodCatalog.coreInline.forEach { mood ->
                val isPrimary = moodSelection.primaryMood == mood.id
                val isSecondary = mood.id in moodSelection.secondaryMoods
                MoodOrbButton(
                    emoji = mood.emoji,
                    label = mood.label,
                    accentColor = mood.accentColor(isDark),
                    isPrimary = isPrimary,
                    isSecondary = isSecondary,
                    enabled = canEdit,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPrimaryMoodTap(mood.id)
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMoodSheet = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 6th slot: Excited when active, otherwise More/Palette trigger
            val excitedDef = remember { MoodCatalog.findById("excited")!! }
            val isExcitedPrimary = moodSelection.primaryMood == excitedDef.id
            val isExcitedSecondary = excitedDef.id in moodSelection.secondaryMoods
            val showExcitedInline = isExcitedPrimary || isExcitedSecondary

            if (showExcitedInline) {
                MoodOrbButton(
                    emoji = excitedDef.emoji,
                    label = excitedDef.label,
                    accentColor = excitedDef.accentColor(isDark),
                    isPrimary = isExcitedPrimary,
                    isSecondary = isExcitedSecondary,
                    enabled = canEdit,
                    onClick = { showMoodSheet = true },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMoodSheet = true
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                MoreMoodOrb(
                    secondaryCount = moodSelection.secondaryMoods.size,
                    enabled = canEdit,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showMoodSheet = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Subtle secondary feelings & refine bar when moods are active
        AnimatedVisibility(
            visible = moodSelection.hasSelection,
            enter = fadeIn(tween(200)) + expandVertically(),
            exit = fadeOut(tween(150)) + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    primaryDef?.let { primary ->
                        val pColor = primary.accentColor(isDark)
                        Surface(
                            shape = CircleShape,
                            color = pColor.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, pColor.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(enabled = canEdit) { showMoodSheet = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = primary.emoji, fontSize = 11.sp)
                                Text(
                                    text = primary.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = pColor
                                )
                            }
                        }
                    }

                    moodSelection.secondaryMoods.forEach { secId ->
                        MoodCatalog.findById(secId)?.let { sec ->
                            Surface(
                                shape = CircleShape,
                                color = tokens.cardElevated,
                                border = tokens.strokeSubtle,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(enabled = canEdit) { showMoodSheet = true }
                            ) {
                                Text(
                                    text = "+ ${sec.emoji} ${sec.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                if (canEdit) {
                    Surface(
                        shape = CircleShape,
                        color = tokens.highlightFill,
                        border = tokens.strokeSubtle,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showMoodSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Layer feelings",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Layer",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMoodSheet) {
        MoodPaletteSheet(
            moodSelection = moodSelection,
            onSelectPrimary = { id ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPrimaryMoodTap(id)
            },
            onToggleSecondary = { id ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSecondaryMoodToggle(id)
            },
            onClearAll = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClearMoods()
            },
            onDismiss = { showMoodSheet = false }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MoodOrbButton(
    emoji: String,
    label: String,
    accentColor: Color,
    isPrimary: Boolean,
    isSecondary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalDesignTokens.current
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = when {
            isPrimary -> 1.07f
            isSecondary -> 1.02f
            else -> 1.0f
        },
        animationSpec = MotionSpec.tactileSpring(),
        label = "orbScale_$label"
    )

    val fill by animateColorAsState(
        targetValue = when {
            isPrimary -> accentColor.copy(alpha = 0.22f)
            isSecondary -> accentColor.copy(alpha = 0.11f)
            else -> tokens.cardElevated
        },
        animationSpec = tween(220),
        label = "orbFill_$label"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isPrimary -> accentColor
            isSecondary -> accentColor.copy(alpha = 0.55f)
            else -> tokens.borderSubtle
        },
        animationSpec = tween(220),
        label = "orbBorder_$label"
    )

    Column(
        modifier = modifier
            .pressScale(interactionSource = interactionSource, pressedScale = 0.92f, enabled = enabled)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isPrimary) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.34f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(fill)
                    .border(
                        width = if (isPrimary) 1.6.dp else 1.dp,
                        color = borderColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = if (isPrimary) 19.sp else 17.sp
                )
            }

            if (isSecondary && !isPrimary) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            fontWeight = when {
                isPrimary -> FontWeight.Bold
                isSecondary -> FontWeight.SemiBold
                else -> FontWeight.Medium
            },
            color = when {
                isPrimary -> accentColor
                isSecondary -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MoreMoodOrb(
    secondaryCount: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalDesignTokens.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .pressScale(interactionSource = interactionSource, pressedScale = 0.92f, enabled = enabled)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tokens.cardContainer)
                .border(1.dp, tokens.borderDefault, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "More moods",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            if (secondaryCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = "More",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoodPaletteSheet(
    moodSelection: MoodSelectionState,
    onSelectPrimary: (String) -> Unit,
    onToggleSecondary: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalDesignTokens.current
    val isDark = LocalAppTheme.current.isDark

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.sheetContainer,
        shape = RoundedCornerShape(topStart = tokens.sheetRadius, topEnd = tokens.sheetRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "How are you feeling?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pick 1 primary mood and optionally layer up to 2 nuances",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (moodSelection.hasSelection) {
                    TextButton(onClick = onClearAll) {
                        Text(
                            text = "Clear",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Section 1: Primary Mood 2x3 Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PRIMARY MOOD",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                val rows = remember { MoodCatalog.all.chunked(3) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { rowMoods ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowMoods.forEach { mood ->
                                PrimaryMoodGridTile(
                                    mood = mood,
                                    isSelected = moodSelection.primaryMood == mood.id,
                                    isDark = isDark,
                                    onClick = { onSelectPrimary(mood.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = tokens.borderSubtle)

            // Section 2: Optional Secondary Feelings (up to 2)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SECONDARY FEELINGS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = CircleShape,
                        color = tokens.highlightFill,
                        border = tokens.strokeSubtle
                    ) {
                        Text(
                            text = "${moodSelection.secondaryMoods.size}/${MoodCatalog.MAX_SECONDARY_MOODS}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoodCatalog.all.forEach { mood ->
                        val isPrimary = moodSelection.primaryMood == mood.id
                        val isSecondary = mood.id in moodSelection.secondaryMoods
                        val atLimit = moodSelection.secondaryMoods.size >= MoodCatalog.MAX_SECONDARY_MOODS && !isSecondary
                        val accent = mood.accentColor(isDark)

                        Surface(
                            onClick = { onToggleSecondary(mood.id) },
                            enabled = !isPrimary && !atLimit,
                            shape = CircleShape,
                            color = when {
                                isSecondary -> accent.copy(alpha = 0.2f)
                                else -> tokens.cardElevated
                            },
                            border = BorderStroke(
                                width = if (isSecondary) 1.4.dp else 1.dp,
                                color = when {
                                    isSecondary -> accent
                                    else -> tokens.borderSubtle
                                }
                            ),
                            modifier = Modifier
                                .pressScale(pressedScale = 0.95f, enabled = !isPrimary && !atLimit)
                                .alpha(if (isPrimary || atLimit) 0.4f else 1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = mood.emoji, fontSize = 14.sp)
                                Text(
                                    text = mood.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSecondary) accent else MaterialTheme.colorScheme.onSurface
                                )
                                AnimatedContent(
                                    targetState = isSecondary,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "secIcon_${mood.id}"
                                ) { active ->
                                    if (active) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove",
                                            tint = accent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else if (!isPrimary) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Add",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryMoodGridTile(
    mood: MoodDefinition,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalDesignTokens.current
    val accent = mood.accentColor(isDark)
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accent.copy(alpha = 0.20f) else tokens.cardElevated,
        animationSpec = tween(220),
        label = "tileBg_${mood.id}"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accent else tokens.borderSubtle,
        animationSpec = tween(220),
        label = "tileBorder_${mood.id}"
    )

    Surface(
        modifier = modifier
            .pressScale(interactionSource = interactionSource, pressedScale = 0.95f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 1.6.dp else 1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(text = mood.emoji, fontSize = 26.sp)
                Text(
                    text = mood.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
