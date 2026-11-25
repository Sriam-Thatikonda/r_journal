package com.baverika.r_journal.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baverika.r_journal.data.local.entity.Password
import com.baverika.r_journal.data.local.entity.PasswordHistoryItem
import com.baverika.r_journal.data.local.entity.PasswordType
import com.baverika.r_journal.ui.components.HighlightedText
import com.baverika.r_journal.ui.viewmodel.PasswordViewModel
import com.baverika.r_journal.ui.viewmodel.SortOrder
import com.baverika.r_journal.utils.ClipboardUtils
import com.baverika.r_journal.utils.PassphraseGenerator
import com.baverika.r_journal.utils.SecurityUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PasswordGeneratorScreen(
    viewModel: PasswordViewModel
) {
    val passwords by viewModel.passwords.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var siteName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Generator State
    var isPinMode by remember { mutableStateOf(false) }
    var numberLength by remember { mutableIntStateOf(4) }
    var generatedPassword by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isPasswordVisible by remember { mutableStateOf(true) } // Visible by default when generating

    // Initial load and regeneration logic
    LaunchedEffect(numberLength, isPinMode) {
        generatedPassword = if (isPinMode) {
            PassphraseGenerator.generatePin(numberLength)
        } else {
            PassphraseGenerator.generate(numberLength)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Tab Navigation: Generator vs Vault
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Generator", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Vault", fontWeight = FontWeight.Bold)
                        if (passwords.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = if (selectedTabIndex == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${passwords.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedTabIndex == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            )
        }

        if (selectedTabIndex == 0) {
            // --- TAB 0: GENERATOR & SAVE ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode Toggle
                SegmentedControl(
                    options = listOf("Passphrase", "PIN"),
                    selectedIndex = if (isPinMode) 1 else 0,
                    onOptionSelected = { index ->
                        val newMode = index == 1
                        if (isPinMode != newMode) {
                            isPinMode = newMode
                            numberLength = if (isPinMode) 6 else 4
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                )

                // Hero Display Card + Length Chips + Strength Bar
                UnifiedGeneratorCard(
                    password = generatedPassword,
                    isVisible = isPasswordVisible,
                    onToggleVisibility = { isPasswordVisible = !isPasswordVisible },
                    onCopy = {
                        ClipboardUtils.copySensitiveText(context, "Generated Password", generatedPassword)
                        Toast.makeText(context, "Copied to clipboard! (Auto-clears in 30s)", Toast.LENGTH_SHORT).show()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onRegenerate = {
                        generatedPassword = if (isPinMode) {
                            PassphraseGenerator.generatePin(numberLength)
                        } else {
                            PassphraseGenerator.generate(numberLength)
                        }
                    },
                    isPinMode = isPinMode,
                    currentLength = numberLength,
                    onLengthChange = {
                        if (numberLength != it) {
                            numberLength = it
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                )

                // Quick Save Section Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SaveAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "SAVE TO VAULT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ModernInput(
                                value = siteName,
                                onValueChange = { siteName = it },
                                placeholder = "Site / App",
                                icon = Icons.Default.Public,
                                modifier = Modifier.weight(1f)
                            )
                            ModernInput(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = "Username / Email",
                                icon = Icons.Default.Person,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = {
                                if (siteName.isNotBlank() && username.isNotBlank() && generatedPassword.isNotBlank()) {
                                    try {
                                        val securePassword = SecurityUtils.encrypt(generatedPassword)
                                        val type = if (isPinMode) PasswordType.PIN else PasswordType.PASSWORD
                                        viewModel.addPassword(siteName.trim(), username.trim(), securePassword, type)
                                        siteName = ""
                                        username = ""
                                        generatedPassword = if (isPinMode) {
                                            PassphraseGenerator.generatePin(numberLength)
                                        } else {
                                            PassphraseGenerator.generate(numberLength)
                                        }
                                        Toast.makeText(context, "Saved to Vault", Toast.LENGTH_SHORT).show()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedTabIndex = 1
                                    } catch (e: SecurityException) {
                                        Toast.makeText(context, "Encryption failed. Could not save password safely.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = siteName.isNotBlank() && username.isNotBlank() && generatedPassword.isNotBlank(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (siteName.isBlank() || username.isBlank()) "Enter Site & Username to Save" else "Save to Vault",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        } else {
            // --- TAB 1: SAVED VAULT LIST ---
            Column(modifier = Modifier.fillMaxSize()) {
                // Search & Sort bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Search vault by site or username...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "${passwords.size} credentials" else "${passwords.size} matches",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (passwords.isNotEmpty()) {
                                SortControlsDropdown(
                                    sortOrder = sortOrder,
                                    onSortChange = { viewModel.onSortOrderChanged(it) }
                                )
                            }
                        }
                    }
                }

                // Vault LazyColumn
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (passwords.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (searchQuery.isEmpty()) Icons.Default.Lock else Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = if (searchQuery.isEmpty()) "Your vault is empty" else "No matching credentials",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (searchQuery.isEmpty()) "Generate a strong password or PIN to save it securely." else "Try a different site name or username.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                if (searchQuery.isEmpty()) {
                                    Button(
                                        onClick = { selectedTabIndex = 0 },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Generate a Password")
                                    }
                                }
                            }
                        }
                    } else {
                        items(passwords, key = { it.id }) { password ->
                            PasswordListItem(
                                password = password,
                                searchQuery = searchQuery,
                                onDelete = { viewModel.deletePassword(password) },
                                onUpdate = { viewModel.updatePassword(it) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(4.dp)
    ) {
        val itemWidth = maxWidth / options.size
        val offset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "segmentOffset"
        )

        Box(
            modifier = Modifier
                .width(itemWidth)
                .fillMaxHeight()
                .offset(x = offset)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .shadow(elevation = 1.dp, shape = CircleShape)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onOptionSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedIndex == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ModernInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        },
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    )
}

@Composable
private fun PasswordStrengthBar(
    isPinMode: Boolean,
    currentLength: Int
) {
    val (score, label, color) = remember(isPinMode, currentLength) {
        if (isPinMode) {
            when {
                currentLength < 6 -> Triple(1, "Weak", Color(0xFFE53935))
                currentLength < 8 -> Triple(2, "Fair", Color(0xFFFB8C00))
                currentLength < 12 -> Triple(3, "Strong", Color(0xFF43A047))
                else -> Triple(4, "Very Strong", Color(0xFF1E88E5))
            }
        } else {
            when {
                currentLength <= 2 -> Triple(1, "Weak", Color(0xFFE53935))
                currentLength == 3 -> Triple(2, "Fair", Color(0xFFFB8C00))
                currentLength == 4 -> Triple(3, "Strong", Color(0xFF43A047))
                else -> Triple(4, "Very Strong", Color(0xFF1E88E5))
            }
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(350),
        label = "strengthColor"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..4) {
                val filled = i <= score
                val fillFraction by animateFloatAsState(
                    targetValue = if (filled) 1f else 0f,
                    animationSpec = tween(durationMillis = 300),
                    label = "bar_$i"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillFraction)
                            .clip(CircleShape)
                            .background(animatedColor)
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = animatedColor
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnifiedGeneratorCard(
    password: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    isPinMode: Boolean,
    currentLength: Int,
    onLengthChange: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var spinTrigger by remember { mutableIntStateOf(0) }
    val spinRotation by animateFloatAsState(
        targetValue = spinTrigger * 360f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "spinRotation"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Output Display Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .combinedClickable(
                    onClick = onCopy,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleVisibility()
                    }
                ),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Full-width responsive password text (never clips multi-word passphrases)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val dynamicFontSize = when {
                        !isVisible -> 22.sp
                        password.length > 28 -> 16.sp
                        password.length > 20 -> 19.sp
                        else -> 23.sp
                    }

                    Crossfade(
                        targetState = if (isVisible) password else "••••••••••••",
                        animationSpec = tween(220),
                        label = "pwCrossfade"
                    ) { displayText ->
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = dynamicFontSize,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = if (isVisible) 0.5.sp else 4.sp,
                                lineHeight = (dynamicFontSize.value + 6).sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                PasswordStrengthBar(isPinMode = isPinMode, currentLength = currentLength)

                // Quick Actions inside Hero Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap to copy • Hold to mask",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(
                            onClick = onToggleVisibility,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            )
                        ) {
                            Icon(
                                if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                spinTrigger++
                                onRegenerate()
                            },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            )
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = spinRotation }
                            )
                        }

                        Button(
                            onClick = onCopy,
                            shape = CircleShape,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Conflict-Free Length Selector Row (Replaces nested VerticalWheelPicker)
        val lengthOptions = if (isPinMode) listOf(4, 6, 8, 10, 12, 16) else listOf(2, 3, 4, 5, 6)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isPinMode) "PIN Digits" else "Word Count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                lengthOptions.forEach { length ->
                    val selected = length == currentLength
                    FilterChip(
                        selected = selected,
                        onClick = { onLengthChange(length) },
                        label = {
                            Text(
                                text = "$length",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SortControlsDropdown(
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (sortOrder) {
        SortOrder.NEWEST_FIRST -> "Newest"
        SortOrder.NAME_A_Z -> "A–Z"
        SortOrder.NAME_Z_A -> "Z–A"
        SortOrder.TYPE_PIN_FIRST -> "PINs First"
    }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Sort,
                    contentDescription = "Sort",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                SortOrder.NEWEST_FIRST to "Newest first",
                SortOrder.NAME_A_Z to "Name A–Z",
                SortOrder.NAME_Z_A to "Name Z–A",
                SortOrder.TYPE_PIN_FIRST to "PINs first"
            ).forEach { (order, orderLabel) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            orderLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (order == sortOrder) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSortChange(order)
                        expanded = false
                    },
                    leadingIcon = if (order == sortOrder) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordListItem(
    password: Password,
    searchQuery: String = "",
    onDelete: () -> Unit,
    onUpdate: (Password) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val decryptedPassword = remember(password.passwordValue) {
        SecurityUtils.decrypt(password.passwordValue)
    }

    // Auto-hide revealed password after 10 seconds
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(10_000L)
            isVisible = false
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Credential?") },
            text = { Text("Remove \"${password.siteName}\" (${password.username}) permanently from your vault?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modern ModalBottomSheet for Editing, Regenerating & Viewing History
    if (showEditSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var editSite by remember { mutableStateOf(password.siteName) }
        var editUsername by remember { mutableStateOf(password.username) }
        var editPassword by remember { mutableStateOf(decryptedPassword) }
        var editPwVisible by remember { mutableStateOf(false) }

        var editSpinTrigger by remember { mutableIntStateOf(0) }
        val editSpinRotation by animateFloatAsState(
            targetValue = editSpinTrigger * 360f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "editSpinRotation"
        )

        val isPasswordChanged = editPassword != decryptedPassword

        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sheet Header with Delete Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Credential Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            showEditSheet = false
                            showDeleteConfirmation = true
                        }
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete entry",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                OutlinedTextField(
                    value = editSite,
                    onValueChange = { editSite = it },
                    label = { Text("Site / App") },
                    leadingIcon = { Icon(Icons.Default.Public, null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editUsername,
                    onValueChange = { editUsername = it },
                    label = { Text("Username / Email") },
                    leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(editUsername))
                            Toast.makeText(context, "Username copied!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, "Copy username", modifier = Modifier.size(18.dp))
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editPassword,
                    onValueChange = { editPassword = it },
                    label = { Text("Password / PIN") },
                    leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { editPwVisible = !editPwVisible }) {
                                Icon(
                                    if (editPwVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = {
                                editPassword = if (password.type == PasswordType.PIN) {
                                    PassphraseGenerator.generatePin(6)
                                } else {
                                    PassphraseGenerator.generate(4)
                                }
                                editPwVisible = true
                                editSpinTrigger++
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Regenerate",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = editSpinRotation }
                                )
                            }
                        }
                    },
                    visualTransformation = if (editPwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                )

                AnimatedVisibility(visible = isPasswordChanged) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Previous password will be archived in history (up to 3).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Password History List
                if (password.history.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Text(
                        text = "Previous Passwords (${password.history.size}/3)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        password.history.forEach { historyItem ->
                            val decryptedPastPw = remember(historyItem.passwordValue) {
                                try {
                                    SecurityUtils.decrypt(historyItem.passwordValue)
                                } catch (e: Exception) {
                                    historyItem.passwordValue
                                }
                            }
                            val formattedDate = remember(historyItem.changedAt) {
                                val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                                sdf.format(Date(historyItem.changedAt))
                            }
                            var isPastPwVisible by remember { mutableStateOf(false) }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = formattedDate,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = if (isPastPwVisible) decryptedPastPw else "••••••••••••",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = { isPastPwVisible = !isPastPwVisible },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                if (isPastPwVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                ClipboardUtils.copySensitiveText(context, "Vault Password", decryptedPastPw)
                                                Toast.makeText(context, "Copied past password! (Auto-clears in 30s)", Toast.LENGTH_SHORT).show()
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copy past password",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Save & Cancel Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showEditSheet = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        enabled = editSite.isNotBlank() && editUsername.isNotBlank() && editPassword.isNotBlank(),
                        onClick = {
                            val updatedHistory = if (isPasswordChanged) {
                                val newHistoryItem = PasswordHistoryItem(
                                    passwordValue = password.passwordValue,
                                    changedAt = System.currentTimeMillis()
                                )
                                (listOf(newHistoryItem) + password.history).take(3)
                            } else {
                                password.history
                            }

                            try {
                                val encryptedEditPassword = SecurityUtils.encrypt(editPassword)
                                onUpdate(
                                    password.copy(
                                        siteName = editSite.trim(),
                                        username = editUsername.trim(),
                                        passwordValue = encryptedEditPassword,
                                        history = updatedHistory,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                                showEditSheet = false
                            } catch (e: SecurityException) {
                                Toast.makeText(context, "Encryption failed. Password not updated.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Streamlined Vault Item Card
    Card(
        onClick = { showEditSheet = true },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Monogram Site Avatar
                val initial = password.siteName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (password.type == PasswordType.PIN)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (password.type == PasswordType.PIN)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Site Name, Badges & Tap-to-Copy Username
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HighlightedText(
                            text = password.siteName,
                            query = searchQuery,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            highlightColor = MaterialTheme.colorScheme.primary,
                            highlightBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Surface(
                            color = if (password.type == PasswordType.PIN)
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (password.type == PasswordType.PIN) "PIN" else "PASS",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (password.type == PasswordType.PIN)
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else
                                    MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (password.history.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "${password.history.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Tap username to copy directly
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(password.username))
                                Toast.makeText(context, "Username copied!", Toast.LENGTH_SHORT).show()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        HighlightedText(
                            text = password.username,
                            query = searchQuery,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            highlightColor = MaterialTheme.colorScheme.primary,
                            highlightBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy username",
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                // Streamlined 2-Action Cluster (Reveal + Copy Password)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { isVisible = !isVisible },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            ClipboardUtils.copySensitiveText(context, "Vault Password", decryptedPassword)
                            Toast.makeText(context, "Password copied! (Auto-clears in 30s)", Toast.LENGTH_SHORT).show()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier.size(38.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy password",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Revealed Password Tray
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = decryptedPassword,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "10s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}