package com.baverika.r_journal.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baverika.r_journal.BuildConfig
import com.baverika.r_journal.MainActivity
import com.baverika.r_journal.data.local.QuickNotesPreferences
import com.baverika.r_journal.ui.theme.LocalDesignTokens
import com.baverika.r_journal.ui.theme.MotionSpec
import com.baverika.r_journal.ui.theme.pressScale

/**
 * Redesigned biometric lock screen with subtle breathing halo and sleek vault aesthetics.
 */
@Composable
fun PremiumLockScreen(
    errorMessage: String?,
    onUnlockClick: () -> Unit
) {
    val tokens = LocalDesignTokens.current
    val infiniteTransition = rememberInfiniteTransition(label = "lockHalo")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    AmbientBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .scale(haloScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = haloAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Surface(
                    shape = CircleShape,
                    color = tokens.cardElevated,
                    border = tokens.strokeDefault,
                    modifier = Modifier.size(88.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "R-Journal Vault",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage ?: "Private reflections & personal trackers protected",
                style = MaterialTheme.typography.bodyMedium,
                color = if (errorMessage != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onUnlockClick,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.pressScale()
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Unlock Journal",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Animated global TopAppBar supporting inline search pill expansion and smooth title transitions.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PremiumTopBar(
    currentRoute: String?,
    screenTitle: String,
    isSearchActive: Boolean,
    topBarSearchQuery: String,
    searchFocusRequester: FocusRequester,
    isBiometricEnabled: Boolean,
    quickNotesLayoutType: String?,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onActivateSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSubmitGlobalSearch: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    onHideKeyboard: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onToggleQuickNotesLayout: () -> Unit,
    onOpenQuoteSettings: () -> Unit,
    onOpenChallengeHistory: () -> Unit
) {
    val tokens = LocalDesignTokens.current
    val haptic = LocalHapticFeedback.current

    Column {
        AnimatedContent(
            targetState = isSearchActive,
            transitionSpec = {
                (fadeIn(tween(200)) + slideInVertically { -it / 6 }) togetherWith
                    (fadeOut(tween(160)) + slideOutVertically { -it / 6 })
            },
            label = "topBarSearchSwap"
        ) { searchMode ->
            if (searchMode) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = tokens.barGlass
                    ),
                    navigationIcon = {
                        IconButton(onClick = onCloseSearch) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
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
                            if (topBarSearchQuery.isEmpty()) {
                                val placeholderText = when (currentRoute) {
                                    "archive" -> "Search journals..."
                                    "quick_notes" -> "Search notes..."
                                    "tasks" -> "Search tasks..."
                                    "trackers" -> "Search trackers..."
                                    else -> "Search..."
                                }
                                Text(
                                    text = placeholderText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            }
                            BasicTextField(
                                value = topBarSearchQuery,
                                onValueChange = onSearchQueryChange,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { onHideKeyboard() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onSubmitGlobalSearch) {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = "Search Everywhere (Global Search)",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (topBarSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                        if (currentRoute == "quick_notes" && quickNotesLayoutType != null) {
                            IconButton(onClick = onToggleQuickNotesLayout) {
                                Icon(
                                    imageVector = if (quickNotesLayoutType == QuickNotesPreferences.LAYOUT_MASONRY) {
                                        Icons.Default.ViewAgenda
                                    } else {
                                        Icons.Default.GridView
                                    },
                                    contentDescription = "Toggle Layout"
                                )
                            }
                        }
                    }
                )
            } else {
                val showBackButton = currentRoute != "archive"
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = tokens.barGlass
                    ),
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    title = {
                        AnimatedContent(
                            targetState = screenTitle,
                            transitionSpec = {
                                (fadeIn(tween(220)) + slideInVertically { it / 5 }) togetherWith
                                    (fadeOut(tween(150)) + slideOutVertically { -it / 5 })
                            },
                            label = "topBarTitleTransition"
                        ) { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        if (currentRoute == "archive" || currentRoute == "quick_notes" || currentRoute == "tasks" || currentRoute == "trackers") {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .combinedClickable(
                                        onClick = onActivateSearch,
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onOpenGlobalSearch()
                                        }
                                    )
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search (Hold for Global Search)",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (currentRoute == "quotes") {
                            IconButton(onClick = onOpenQuoteSettings) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Widget Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (currentRoute == "archive") {
                            IconToggleButton(
                                checked = isBiometricEnabled,
                                onCheckedChange = onToggleBiometric
                            ) {
                                Icon(
                                    imageVector = if (isBiometricEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (isBiometricEnabled) "Biometric Lock On" else "Biometric Lock Off",
                                    tint = if (isBiometricEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (currentRoute == "quick_notes" && quickNotesLayoutType != null) {
                            IconButton(onClick = onToggleQuickNotesLayout) {
                                Icon(
                                    imageVector = if (quickNotesLayoutType == QuickNotesPreferences.LAYOUT_MASONRY) {
                                        Icons.Default.ViewAgenda
                                    } else {
                                        Icons.Default.GridView
                                    },
                                    contentDescription = "Toggle Layout"
                                )
                            }
                        }
                        if (currentRoute == "challenges") {
                            IconButton(onClick = onOpenChallengeHistory) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Challenge History",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }
        }
        HorizontalDivider(
            thickness = 0.6.dp,
            color = tokens.borderSubtle
        )
    }
}

/**
 * Floating glass-styled bottom navigation dock with animated pill indicator and haptic feedback.
 */
@Composable
fun FloatingBottomDock(
    visible: Boolean,
    items: List<MainActivity.BottomNavItem>,
    currentRoute: String?,
    onSelect: (MainActivity.BottomNavItem) -> Unit
) {
    val tokens = LocalDesignTokens.current
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(260, easing = MotionSpec.EmphasizedDecelerate),
            initialOffsetY = { it }
        ) + fadeIn(tween(220)),
        exit = slideOutVertically(
            animationSpec = tween(200, easing = MotionSpec.EmphasizedAccelerate),
            targetOffsetY = { it }
        ) + fadeOut(tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = tokens.barGlass,
                border = tokens.strokeDefault,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        val interactionSource = remember { MutableInteractionSource() }

                        val pillBg by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = tween(240),
                            label = "dockPillBg_${item.route}"
                        )
                        val tintColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(220),
                            label = "dockTint_${item.route}"
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1f,
                            animationSpec = MotionSpec.tactileSpring(),
                            label = "dockScale_${item.route}"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .pressScale(interactionSource = interactionSource, pressedScale = 0.94f)
                                .clip(CircleShape)
                                .background(pillBg, CircleShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = {
                                        if (!isSelected) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelect(item)
                                        }
                                    }
                                )
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = tintColor,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .scale(iconScale)
                                )
                                AnimatedVisibility(visible = isSelected) {
                                    Row {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = tintColor,
                                            maxLines = 1
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

/**
 * Liquid-glass primary FAB with smooth icon transition and tactile spring feedback.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PremiumFab(
    fabAction: (() -> Unit)?,
    fabIcon: ImageVector?,
    fabDesc: String?,
    onLongPressSpeedDial: () -> Unit
) {
    val tokens = LocalDesignTokens.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    AnimatedVisibility(
        visible = fabAction != null && fabIcon != null,
        enter = scaleIn(MotionSpec.tactileSpring()) + fadeIn(tween(180)),
        exit = scaleOut(tween(150)) + fadeOut(tween(140))
    ) {
        if (fabAction != null && fabIcon != null) {
            val glassBorder = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (tokens.isGlassTheme) 0.55f else 0.45f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    Color.White.copy(alpha = 0.16f)
                )
            )
            Surface(
                modifier = Modifier
                    .padding(bottom = 4.dp, end = 2.dp)
                    .size(64.dp)
                    .pressScale(interactionSource = interactionSource, pressedScale = 0.92f)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.3.dp, glassBorder, RoundedCornerShape(22.dp))
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            fabAction()
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPressSpeedDial()
                        }
                    ),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (tokens.isGlassTheme) 0.24f else 0.92f),
                contentColor = if (tokens.isGlassTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 8.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (tokens.isGlassTheme) 0.18f else 0.14f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    AnimatedContent(
                        targetState = fabIcon,
                        transitionSpec = {
                            (scaleIn(initialScale = 0.8f) + fadeIn(tween(160))) togetherWith
                                (scaleOut(targetScale = 0.8f) + fadeOut(tween(120)))
                        },
                        label = "fabIconSwap"
                    ) { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = fabDesc,
                            tint = if (tokens.isGlassTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSpeedDialSheet(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tokens = LocalDesignTokens.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.sheetContainer,
        shape = RoundedCornerShape(topStart = tokens.sheetRadius, topEnd = tokens.sheetRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Quick Create",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Jump straight into any workspace",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = tokens.borderSubtle)

            val speedDialItems = listOf(
                Triple("New Journal Entry", Icons.AutoMirrored.Filled.Chat, "chat_input"),
                Triple("New Quick Note", Icons.AutoMirrored.Filled.NoteAdd, "new_quick_note"),
                Triple("New Task", Icons.Default.CheckCircle, "add_task"),
                Triple("New Tracker", Icons.Default.BarChart, "add_tracker"),
                Triple("Log Craving", Icons.Default.FitnessCenter, "add_craving"),
                Triple("New Habit", Icons.Default.Add, "add_habit")
            )

            speedDialItems.forEach { (label, icon, route) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = tokens.cardElevated,
                    border = tokens.strokeSubtle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onDismiss()
                            onNavigate(route)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun PremiumDrawerContent(
    currentRoute: String?,
    onScreenSelected: (String) -> Unit
) {
    val tokens = LocalDesignTokens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 14.dp, horizontal = 12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            shape = RoundedCornerShape(22.dp),
            color = tokens.cardElevated,
            border = tokens.strokeDefault
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "R-Journal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Personal Life OS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        DrawerSectionLabel("Overview")
        PremiumDrawerItem(
            icon = Icons.Filled.BarChart,
            label = "Dashboard",
            isSelected = currentRoute == "dashboard",
            onClick = { onScreenSelected("dashboard") }
        )

        DrawerDivider()

        DrawerSectionLabel("Habits & Goals")
        PremiumDrawerItem(
            icon = Icons.Filled.CheckCircle,
            label = "Habit Tracker",
            isSelected = currentRoute == "habits",
            onClick = { onScreenSelected("habits") }
        )
        PremiumDrawerItem(
            icon = Icons.Filled.Star,
            label = "Challenge Tracker",
            isSelected = currentRoute == "challenges",
            onClick = { onScreenSelected("challenges") }
        )
        PremiumDrawerItem(
            icon = Icons.Filled.FitnessCenter,
            label = "Craving Quest",
            isSelected = currentRoute == "craving_quest",
            onClick = { onScreenSelected("craving_quest") }
        )
        PremiumDrawerItem(
            icon = Icons.Filled.AddCircle,
            label = "Trackers",
            isSelected = currentRoute == "trackers",
            onClick = { onScreenSelected("trackers") }
        )

        DrawerDivider()

        DrawerSectionLabel("Memory & Time")
        PremiumDrawerItem(
            icon = Icons.Filled.CalendarMonth,
            label = "Calendar",
            isSelected = currentRoute == "calendar",
            onClick = { onScreenSelected("calendar") }
        )
        PremiumDrawerItem(
            icon = Icons.Filled.Event,
            label = "Special Dates",
            isSelected = currentRoute == "events",
            onClick = { onScreenSelected("events") }
        )
        PremiumDrawerItem(
            icon = Icons.Filled.DateRange,
            label = "Life Milestones",
            isSelected = currentRoute == "life_trackers",
            onClick = { onScreenSelected("life_trackers") }
        )
        PremiumDrawerItem(
            icon = Icons.Filled.FormatQuote,
            label = "Quotes",
            isSelected = currentRoute == "quotes",
            onClick = { onScreenSelected("quotes") }
        )

        DrawerDivider()

        DrawerSectionLabel("Vault & System")
        PremiumDrawerItem(
            icon = Icons.Filled.VpnKey,
            label = "Password Vault",
            isSelected = currentRoute == "password_generator",
            onClick = { onScreenSelected("password_generator") }
        )
        PremiumDrawerItem(
            icon = Icons.Filled.FolderSpecial,
            label = "Mine",
            isSelected = currentRoute == "chat_input/mine",
            onClick = { onScreenSelected("chat_input/mine") }
        )
        PremiumDrawerItem(
            icon = Icons.Filled.Settings,
            label = "Settings",
            isSelected = currentRoute == "settings",
            onClick = { onScreenSelected("settings") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun DrawerSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.0.sp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun DrawerDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        color = LocalDesignTokens.current.borderSubtle
    )
}

@Composable
private fun PremiumDrawerItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalDesignTokens.current
    val interactionSource = remember { MutableInteractionSource() }

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) tokens.highlightFill else Color.Transparent,
        animationSpec = tween(200),
        label = "drawerItemBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "drawerItemColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .pressScale(interactionSource = interactionSource, pressedScale = 0.97f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = if (isSelected) tokens.strokeSubtle else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else contentColor
            )
        }
    }
}
