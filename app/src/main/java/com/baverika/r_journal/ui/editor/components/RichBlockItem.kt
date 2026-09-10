package com.baverika.r_journal.ui.editor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baverika.r_journal.data.model.BlockType
import com.baverika.r_journal.data.model.RichBlock
import com.baverika.r_journal.data.model.RichSpan
import com.baverika.r_journal.data.model.SpanType

@Composable
fun RichBlockItem(
    block: RichBlock,
    index: Int,
    isFocused: Boolean,
    focusRequester: FocusRequester,
    textColor: Color,
    secondaryTextColor: Color,
    onTextChanged: (String, TextRange) -> Unit,
    onFocusGained: (TextRange) -> Unit,
    onToggleChecked: (String) -> Unit,
    onEnterPressed: (Int) -> Unit,
    onBackspaceOnEmpty: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(block.id) {
        mutableStateOf(TextFieldValue(text = block.text, selection = TextRange(block.text.length)))
    }

    // Keep internal text in sync if external changes occur (e.g. undo/redo)
    if (textFieldValue.text != block.text) {
        val clampedSelection = TextRange(
            textFieldValue.selection.start.coerceIn(0, block.text.length),
            textFieldValue.selection.end.coerceIn(0, block.text.length)
        )
        textFieldValue = textFieldValue.copy(text = block.text, selection = clampedSelection)
    }

    val visualTransformation = remember(block.spans, block.isChecked, block.type) {
        RichTextVisualTransformation(block.spans, block.type == BlockType.CHECKLIST && block.isChecked)
    }

    val itemTextColor by animateColorAsState(
        targetValue = if (block.type == BlockType.CHECKLIST && block.isChecked) {
            secondaryTextColor.copy(alpha = 0.5f)
        } else {
            textColor
        },
        animationSpec = tween(150),
        label = "itemTextColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Fixed-width prefix column for proper indentation
        when (block.type) {
            BlockType.CHECKLIST -> {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(top = 2.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onToggleChecked(block.id) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (block.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = if (block.isChecked) "Checked" else "Unchecked",
                        tint = if (block.isChecked) MaterialTheme.colorScheme.primary else secondaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            BlockType.BULLET -> {
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 28.dp)
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.titleLarge,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            BlockType.NUMBERED -> {
                Box(
                    modifier = Modifier
                        .widthIn(min = 28.dp)
                        .height(28.dp)
                        .padding(top = 4.dp, end = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
            BlockType.PARAGRAPH -> {
                // No prefix
            }
        }

        // Editable text column - wrapped lines stay within this column
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Handle enter press if newline character is inserted
                    val newlineIdx = newValue.text.indexOf('\n')
                    if (newlineIdx >= 0) {
                        // Enter was pressed at newlineIdx
                        val before = newValue.text.substring(0, newlineIdx)
                        val after = if (newlineIdx + 1 <= newValue.text.length) newValue.text.substring(newlineIdx + 1) else ""
                        val cleaned = before + after
                        textFieldValue = TextFieldValue(cleaned, TextRange(newlineIdx))
                        onTextChanged(cleaned, TextRange(newlineIdx))
                        onEnterPressed(newlineIdx)
                    } else {
                        textFieldValue = newValue
                        onTextChanged(newValue.text, newValue.selection)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onFocusGained(textFieldValue.selection)
                        }
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Backspace && textFieldValue.text.isEmpty()) {
                            onBackspaceOnEmpty()
                            true
                        } else {
                            false
                        }
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = itemTextColor,
                    lineHeight = 24.sp
                ),
                cursorBrush = SolidColor(textColor),
                visualTransformation = visualTransformation,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Default
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onEnterPressed(textFieldValue.selection.end) }
                )
            )

            if (textFieldValue.text.isEmpty() && isFocused) {
                Text(
                    text = when (block.type) {
                        BlockType.CHECKLIST -> "Checklist item..."
                        BlockType.BULLET -> "List item..."
                        BlockType.NUMBERED -> "Numbered item..."
                        BlockType.PARAGRAPH -> "Note..."
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = secondaryTextColor.copy(alpha = 0.4f),
                        lineHeight = 24.sp
                    )
                )
            }
        }
    }
}

class RichTextVisualTransformation(
    private val spans: List<RichSpan>,
    private val forceStrikeThrough: Boolean
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val builder = AnnotatedString.Builder(raw)

        if (forceStrikeThrough) {
            builder.addStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough),
                0,
                raw.length
            )
        }

        for (span in spans) {
            val start = span.start.coerceIn(0, raw.length)
            val end = span.end.coerceIn(start, raw.length)
            if (start < end) {
                val style = when (span.type) {
                    SpanType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                    SpanType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                    SpanType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                    SpanType.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    SpanType.COLOR -> {
                        val parsedColor = try {
                            if (span.colorHex != null) {
                                Color(android.graphics.Color.parseColor(span.colorHex))
                            } else {
                                Color.Unspecified
                            }
                        } catch (_: Exception) {
                            Color.Unspecified
                        }
                        SpanStyle(color = parsedColor)
                    }
                }
                builder.addStyle(style, start, end)
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
