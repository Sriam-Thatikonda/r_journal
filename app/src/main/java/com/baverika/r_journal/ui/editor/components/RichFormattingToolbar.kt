package com.baverika.r_journal.ui.editor.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.data.model.BlockType
import com.baverika.r_journal.data.model.SpanType

val CuratedTextColors = listOf(
    Pair("Default", null),
    Pair("Coral Red", "#EF4444"),
    Pair("Crimson", "#DC2626"),
    Pair("Sunset Amber", "#F59E0B"),
    Pair("Warm Orange", "#FB923C"),
    Pair("Sage Green", "#10B981"),
    Pair("Ocean Blue", "#3B82F6"),
    Pair("Sky Blue", "#0EA5E9"),
    Pair("Lavender Purple", "#8B5CF6"),
    Pair("Soft Pink", "#EC4899"),
    Pair("Nordic Teal", "#14B8A6")
)

@Composable
fun RichFormattingToolbar(
    activeBlockType: BlockType,
    canUndo: Boolean,
    canRedo: Boolean,
    onFormatClick: (SpanType, String?) -> Unit,
    onListTypeClick: (BlockType) -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showColorPalette by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column {
            // Expandable Text Color Palette Bar
            AnimatedVisibility(
                visible = showColorPalette,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Color:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    for ((_, hex) in CuratedTextColors) {
                        if (hex == null) {
                            // "Default" button
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                    .clickable {
                                        onFormatClick(SpanType.COLOR, null)
                                        showColorPalette = false
                                    }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Default",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
                                    .clickable {
                                        onFormatClick(SpanType.COLOR, hex)
                                        showColorPalette = false
                                    }
                            )
                        }
                    }
                }
            }

            // Main Formatting Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ToolbarIconButton(
                    icon = Icons.Default.FormatBold,
                    description = "Bold",
                    onClick = { onFormatClick(SpanType.BOLD, null) }
                )

                ToolbarIconButton(
                    icon = Icons.Default.FormatItalic,
                    description = "Italic",
                    onClick = { onFormatClick(SpanType.ITALIC, null) }
                )

                ToolbarIconButton(
                    icon = Icons.Default.FormatUnderlined,
                    description = "Underline",
                    onClick = { onFormatClick(SpanType.UNDERLINE, null) }
                )

                ToolbarIconButton(
                    icon = Icons.Default.FormatStrikethrough,
                    description = "Strikethrough",
                    onClick = { onFormatClick(SpanType.STRIKETHROUGH, null) }
                )

                ToolbarIconButton(
                    icon = Icons.Default.FormatColorText,
                    description = "Text Color",
                    isActive = showColorPalette,
                    onClick = { showColorPalette = !showColorPalette }
                )

                ToolbarDivider()

                ToolbarIconButton(
                    icon = Icons.Default.CheckBox,
                    description = "Checklist",
                    isActive = activeBlockType == BlockType.CHECKLIST,
                    onClick = { onListTypeClick(BlockType.CHECKLIST) }
                )

                ToolbarIconButton(
                    icon = Icons.Default.FormatListBulleted,
                    description = "Bullet List",
                    isActive = activeBlockType == BlockType.BULLET,
                    onClick = { onListTypeClick(BlockType.BULLET) }
                )

                ToolbarIconButton(
                    icon = Icons.Default.FormatListNumbered,
                    description = "Numbered List",
                    isActive = activeBlockType == BlockType.NUMBERED,
                    onClick = { onListTypeClick(BlockType.NUMBERED) }
                )

                ToolbarDivider()

                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    description = "Undo",
                    enabled = canUndo,
                    onClick = onUndoClick
                )

                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.Redo,
                    description = "Redo",
                    enabled = canRedo,
                    onClick = onRedoClick
                )
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(38.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
