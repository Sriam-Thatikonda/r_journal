package com.baverika.r_journal.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baverika.r_journal.data.local.entity.Password
import com.baverika.r_journal.data.local.entity.PasswordType // Import added
import com.baverika.r_journal.ui.viewmodel.PasswordViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.baverika.r_journal.utils.PassphraseGenerator
import com.baverika.r_journal.utils.SecurityUtils

import androidx.compose.runtime.mutableIntStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen(
    viewModel: PasswordViewModel
) {
    val passwords by viewModel.passwords.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var siteName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Generator State
    var isPinMode by remember { mutableStateOf(false) }
    var numberLength by remember { mutableIntStateOf(4) }
    
    var generatedPassword by remember { mutableStateOf("") }
    var isGeneratorExpanded by remember { mutableStateOf(true) }

    // Initial load
    LaunchedEffect(Unit) {
        generatedPassword = if (isPinMode) {
             PassphraseGenerator.generatePin(numberLength)
        } else {
             PassphraseGenerator.generate(numberLength)
        }
    }

    // Regenerate when length or mode changes
    LaunchedEffect(numberLength, isPinMode) {
        generatedPassword = if (isPinMode) {
             PassphraseGenerator.generatePin(numberLength)
        } else {
             PassphraseGenerator.generate(numberLength)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar
        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search passwords...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }

        // Generator Section
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Toggles (Row 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = !isPinMode,
                        onClick = { 
                            isPinMode = false 
                            numberLength = 4
                        },
                        label = { 
                            Text(
                                "Passphrase", 
                                modifier = Modifier.fillMaxWidth(), 
                                textAlign = TextAlign.Center
                            ) 
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = CircleShape,
                        leadingIcon = { if (!isPinMode) Icon(Icons.Default.Check, null) else null }
                    )
                    FilterChip(
                        selected = isPinMode,
                        onClick = { 
                            isPinMode = true 
                            numberLength = 6
                        },
                        label = { 
                            Text(
                                "PIN", 
                                modifier = Modifier.fillMaxWidth(), 
                                textAlign = TextAlign.Center
                            ) 
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = CircleShape,
                        leadingIcon = { if (isPinMode) Icon(Icons.Default.Check, null) else null }
                    )
                }

                // 2. Inputs (Row 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = siteName,
                        onValueChange = { siteName = it },
                        placeholder = { Text("Site / App") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text("Username") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // 3. Generator Card (Standard Theme Box)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Subtle card background
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Length Picker Strip
                        val range = if (isPinMode) 4..16 else 2..6
                        
                        // Vertical Strip styling for wheel
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface, // Standard surface for contrast
                            modifier = Modifier.width(48.dp)
                        ) {
                             // Re-using CompactWheelPicker logic but stripped down visually
                             Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
                                 // We use the VerticalWheelPicker directly here for cleaner look in strip
                                 val items = range.toList()
                                 VerticalWheelPicker(
                                     items = items,
                                     initialIndex = items.indexOf(numberLength).coerceAtLeast(0),
                                     onValueChange = { numberLength = it },
                                     itemHeight = 32.dp,
                                     visibleItems = 5, // Taller strip
                                     width = 48.dp
                                 )
                             }
                        }

                        // Right: Password & Actions
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Text(
                                text = "Generated password",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // The Password
                            Text(
                                text = generatedPassword.ifEmpty { "..." },
                                style = MaterialTheme.typography.headlineSmall, // Large text
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                lineHeight = 32.sp
                            )

                            // Actions Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Regenerate (Pill Button)
                                FilledTonalButton(
                                    onClick = {
                                        generatedPassword = if (isPinMode) {
                                             PassphraseGenerator.generatePin(numberLength)
                                        } else {
                                             PassphraseGenerator.generate(numberLength)
                                        }
                                    },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Regenerate")
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))

                                // Copy (Circle Button)
                                FilledIconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(generatedPassword))
                                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                            }
                        }
                    }
                }

                // 4. Save Button (Bottom)
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
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = siteName.isNotBlank() && username.isNotBlank() && generatedPassword.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save to Vault", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Saved Passwords Header
        if (passwords.isNotEmpty()) {
            item {
                Text(
                    "Saved (${passwords.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Password List
        items(passwords, key = { it.id }) { password ->
            PasswordListItem(
                password = password,
                onDelete = { viewModel.deletePassword(password) }
            )
        }

        // Empty State
        if (passwords.isEmpty() && searchQuery.isBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No passwords saved yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}



@Composable
private fun CompactWheelPicker(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange = 0..12
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        val items = range.toList()
        VerticalWheelPicker(
            items = items,
            initialIndex = items.indexOf(value).coerceAtLeast(0),
            onValueChange = onValueChange,
            itemHeight = 28.dp,
            visibleItems = 3,
            width = 48.dp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerticalWheelPicker(
    items: List<Int>,
    initialIndex: Int,
    onValueChange: (Int) -> Unit,
    itemHeight: Dp,
    visibleItems: Int,
    width: Dp
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val fadingEdgeHeight = itemHeight

    // Sync scroll when items/initialIndex change
    LaunchedEffect(items, initialIndex) {
        if (listState.firstVisibleItemIndex != initialIndex) {
            listState.scrollToItem(initialIndex)
        }
    }

    // Track scroll and update value
    LaunchedEffect(listState, items) { // Restart when items change
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
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        // Fading edges effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // Top fade
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.3f to Color.Black,
                            0.7f to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = itemHeight),
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    val isSelected = listState.firstVisibleItemIndex == index
                    
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Center indicator
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(width - 8.dp)
                .height(itemHeight)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        )
    }
}

@Composable
private fun PasswordListItem(
    password: Password,
    onDelete: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    // Decrypt the password for display
    val decryptedPassword = remember(password.passwordValue) {
        SecurityUtils.decrypt(password.passwordValue)
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { 
                Text(
                    "Delete Password?",
                    style = MaterialTheme.typography.titleMedium
                ) 
            },
            text = { 
                Text(
                    "Are you sure you want to delete the password for \"${password.siteName}\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel")
                }
            },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = password.siteName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (password.type == PasswordType.PIN) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "PIN",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Text(
                        text = password.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                // Use decryptedPassword for display
                Text(
                    text = if (isVisible) decryptedPassword else "•".repeat(12),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = { isVisible = !isVisible }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = {
                        // Copy decrypted password
                        clipboardManager.setText(AnnotatedString(decryptedPassword))
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}


