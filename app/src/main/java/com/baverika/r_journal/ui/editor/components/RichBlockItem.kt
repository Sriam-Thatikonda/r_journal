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
    autoFocus: Boolean = false,
    focusRequester: FocusRequester,
    textColor: Color,
    secondaryTextColor: Color,
    onTextChanged: (String, TextRange) -> Unit,
    onFocusGained: (TextRange) -> Unit,
    onToggleChecked: (String) -> Unit,
    onEnterPressed: (splitPosition: Int) -> Unit,
    onBackspaceOnEmpty: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(block.id) {
        mutableStateOf(TextFieldValue(text = block.text, selection = TextRange(0)))
    }

    // Keep internal text in sync if external changes occur (e.g. undo/redo)
    if (textFieldValue.text != block.text) {
        val clampedSelection = TextRange(
            textFieldValue.selection.start.coerceIn(0, block.text.length),
            textFieldValue.selection.end.coerceIn(0, block.text.length)
        )
        textFieldValue = textFieldValue.copy(text = block.text, selection = clampedSelection)
    }

    // Auto-focus when requested (e.g. new line created by Enter)
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            kotlinx.coroutines.delay(40)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
            onFocusGained(textFieldValue.selection)
        }
    }

    val visualTransformation = remember(block.spans, block.isChecked, block.type, textColor) {
        RichTextVisualTransformation(
            spans = block.spans,
            forceStrikeThrough = block.type == BlockType.CHECKLIST && block.isChecked,
            defaultColor = textColor
        )
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading prefix based on block type
        when (block.type) {
            BlockType.CHECKLIST -> {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onToggleChecked(block.id) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (block.isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                        contentDescription = if (block.isChecked) "Checked" else "Unchecked",
                        tint = if (block.isChecked) MaterialTheme.colorScheme.primary else secondaryTextColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            BlockType.BULLET -> {
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 36.dp)
                        .padding(start = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u2022",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            BlockType.NUMBERED -> {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 36.dp)
                        .padding(start = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            BlockType.PARAGRAPH -> {
                // No prefix
            }
        }

        // Editable text column
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val newlineIdx = newValue.text.indexOf('\n')
                    if (newlineIdx >= 0) {
                        val textBefore = newValue.text.substring(0, newlineIdx)
                        textFieldValue = TextFieldValue(textBefore, TextRange(textBefore.length))
                        onTextChanged(textBefore, TextRange(textBefore.length))
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
                    lineHeight = 26.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = visualTransformation,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Default
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onEnterPressed(textFieldValue.selection.start) }
                )
            )

            if (textFieldValue.text.isEmpty() && isFocused) {
                Text(
                    text = when (block.type) {
                        BlockType.CHECKLIST -> "To-do item..."
                        BlockType.BULLET -> "List item..."
                        BlockType.NUMBERED -> "List item..."
                        BlockType.PARAGRAPH -> "Type something..."
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = secondaryTextColor.copy(alpha = 0.4f),
                        lineHeight = 26.sp
                    )
                )
            }
        }
    }
}

class RichTextVisualTransformation(
    private val spans: List<RichSpan>,
    private val forceStrikeThrough: Boolean,
    private val defaultColor: Color = Color.Unspecified
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
                                defaultColor
                            }
                        } catch (_: Exception) {
                            defaultColor
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
