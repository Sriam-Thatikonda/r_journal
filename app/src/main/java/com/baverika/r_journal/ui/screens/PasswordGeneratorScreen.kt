package com.baverika.r_journal.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baverika.r_journal.data.local.entity.Password
import com.baverika.r_journal.data.local.entity.PasswordType
import com.baverika.r_journal.ui.viewmodel.PasswordViewModel
import com.baverika.r_journal.utils.PassphraseGenerator
import com.baverika.r_journal.utils.SecurityUtils
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PasswordGeneratorScreen(
    viewModel: PasswordViewModel
) {
    val passwords by viewModel.passwords.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var siteName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Generator State
    var isPinMode by remember { mutableStateOf(false) }
    var numberLength by remember { mutableIntStateOf(4) }
    var generatedPassword by remember { mutableStateOf("") }
    var isGeneratorExpanded by remember { mutableStateOf(true) }
    var isSavedExpanded by remember { mutableStateOf(true) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Initial load and regeneration logic
    LaunchedEffect(numberLength, isPinMode) {
        generatedPassword = if (isPinMode) {
            PassphraseGenerator.generatePin(numberLength)
        } else {
            PassphraseGenerator.generate(numberLength)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Sticky Controls (Search + Mode)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Sticky Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search passwords...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(22.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Mode Toggle (Segmented Control)
            SegmentedControl(
                options = listOf("Passphrase", "PIN"),
                selectedIndex = if (isPinMode) 1 else 0,
                onOptionSelected = { index ->
                    isPinMode = index == 1
                    numberLength = if (isPinMode) 6 else 4
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Generator Section
            item {
                CollapsibleSection(
                    title = "Generate Password",
                    isExpanded = isGeneratorExpanded,
                    onToggle = { isGeneratorExpanded = !isGeneratorExpanded },
                    collapsedContent = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (generatedPassword.isNotEmpty()) "••••••••••••" else "Ready to generate",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Site & Username Inputs
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                placeholder = "Username",
                                icon = Icons.Default.Person,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Password Display Card with Internal Picker
                        UnifiedGeneratorCard(
                            password = generatedPassword,
                            isVisible = isPasswordVisible,
                            onToggleVisibility = { isPasswordVisible = !isPasswordVisible },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(generatedPassword))
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
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

                        // Save Button
                        Button(
                            onClick = {
                                if (siteName.isNotBlank() && username.isNotBlank() && generatedPassword.isNotBlank()) {
                                    val securePassword = SecurityUtils.encrypt(generatedPassword)
                                    val type = if (isPinMode) PasswordType.PIN else PasswordType.PASSWORD
                                    viewModel.addPassword(siteName.trim(), username.trim(), securePassword, type)
                                    siteName = ""
                                    username = ""
                                    generatedPassword = if (isPinMode) PassphraseGenerator.generatePin(numberLength) else PassphraseGenerator.generate(numberLength)
                                    Toast.makeText(context, "Saved to Vault", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            enabled = siteName.isNotBlank() && username.isNotBlank() && generatedPassword.isNotBlank(),
                            shape = RoundedCornerShape(20.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Save to Vault", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Saved Passwords Section
            item {
                CollapsibleSection(
                    title = "Saved Passwords (${passwords.size})",
                    isExpanded = isSavedExpanded,
                    onToggle = { isSavedExpanded = !isSavedExpanded }
                ) {
                    if (passwords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isEmpty()) "No passwords saved yet" else "No matches found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            if (isSavedExpanded) {
                items(passwords, key = { it.id }) { password ->
                    PasswordListItem(
                        password = password,
                        onDelete = { viewModel.deletePassword(password) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
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
            .height(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(4.dp)
    ) {
        val maxWidth = maxWidth
        val itemWidth = maxWidth / options.size
        val offset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "offset"
        )

        Box(
            modifier = Modifier
                .width(itemWidth)
                .fillMaxHeight()
                .offset(x = offset)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .shadow(elevation = 2.dp, shape = CircleShape)
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
fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    collapsedContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onToggle() }
                )
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            content()
        }
        
        if (!isExpanded) {
            collapsedContent()
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
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) },
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    )
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
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SECTION: Vertical Wheel Picker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(72.dp)
            ) {
                Text(
                    text = if (isPinMode) "Digits" else "Words",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val range = if (isPinMode) 4..16 else 2..6
                val items = range.toList()
                
                VerticalWheelPicker(
                    items = items,
                    initialIndex = items.indexOf(currentLength).coerceAtLeast(0),
                    onValueChange = onLengthChange,
                    itemHeight = 44.dp,
                    visibleItems = 3,
                    width = 60.dp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // RIGHT SECTION: Main Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = onCopy,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleVisibility()
                        }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Password Text
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isVisible) password else "••••••••••••",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = if (isVisible) 0.sp else 4.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.animateContentSize()
                    )
                }

                // Action Buttons Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Regenerate
                    FilledTonalIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRegenerate()
                        },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate", modifier = Modifier.size(20.dp))
                    }
                    
                    // Copy
                    Button(
                        onClick = onCopy,
                        shape = CircleShape,
                        modifier = Modifier.height(44.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy", style = MaterialTheme.typography.labelLarge)
                    }

                    // Toggle Visibility
                    FilledTonalIconButton(
                        onClick = { onToggleVisibility() },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Icon(
                            if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Visibility",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Text(
                    "Tap to copy • Long press to reveal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalWheelPicker(
    items: List<Int>,
    initialIndex: Int,
    onValueChange: (Int) -> Unit,
    itemHeight: Dp,
    visibleItems: Int,
    width: Dp
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    // Track scroll and update value
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val newValue = items.getOrNull(index) ?: items.first()
                onValueChange(newValue)
            }
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(itemHeight * visibleItems)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        // Center Indicator Highlight
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        )

        // Fading Edges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.2f to Color.Black,
                            0.8f to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = snapBehavior,
                contentPadding = PaddingValues(vertical = itemHeight),
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(items.size) { index ->
                    val isSelected = listState.firstVisibleItemIndex == index
                    val item = items[index]
                    
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.scale(if (isSelected) 1.2f else 0.9f).animateContentSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordListItem(
    password: Password,
    onDelete: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    val decryptedPassword = remember(password.passwordValue) {
        SecurityUtils.decrypt(password.passwordValue)
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Password?") },
            text = { Text("Are you sure you want to delete the password for \"${password.siteName}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirmation = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = password.siteName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = password.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { isVisible = !isVisible }) {
                        Icon(
                            if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(decryptedPassword))
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = decryptedPassword,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
