package com.baverika.r_journal.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * Extracts a compact text snippet around the matching search query or any of its terms.
 */
fun extractSearchSnippet(fullText: String, query: String, snippetLength: Int = 110): String {
    if (query.isBlank() || fullText.isBlank()) return fullText.take(snippetLength)
    val terms = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (terms.isEmpty()) return fullText.take(snippetLength)

    var idx = fullText.indexOf(query.trim(), ignoreCase = true)
    var matchLen = query.trim().length

    if (idx == -1) {
        for (term in terms) {
            val termIdx = fullText.indexOf(term, ignoreCase = true)
            if (termIdx != -1) {
                idx = termIdx
                matchLen = term.length
                break
            }
        }
    }

    if (idx == -1) {
        return if (fullText.length > snippetLength) fullText.take(snippetLength) + "…" else fullText
    }

    val contextBefore = 35
    val contextAfter = (snippetLength - contextBefore - matchLen).coerceAtLeast(20)
    val start = (idx - contextBefore).coerceAtLeast(0)
    val end = (idx + matchLen + contextAfter).coerceAtMost(fullText.length)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < fullText.length) "…" else ""
    return prefix + fullText.substring(start, end).trim() + suffix
}

/**
 * Displays text with search query terms highlighted using extra bold font,
 * vivid accent color, and an eye-catching translucent background tint.
 */
@Composable
fun HighlightedText(
    text: String,
    query: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    highlightBackground: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    if (query.isBlank() || text.isBlank()) {
        Text(
            text = text,
            style = style,
            color = color,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier
        )
        return
    }

    val terms = remember(query) {
        query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    if (terms.isEmpty() || !terms.any { text.contains(it, ignoreCase = true) }) {
        Text(
            text = text,
            style = style,
            color = color,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier
        )
        return
    }

    val annotated = remember(text, terms, highlightColor, highlightBackground) {
        buildAnnotatedString {
            val ranges = mutableListOf<IntRange>()
            val lowerText = text.lowercase()

            for (term in terms) {
                val lowerTerm = term.lowercase()
                var startIndex = 0
                while (startIndex < lowerText.length) {
                    val found = lowerText.indexOf(lowerTerm, startIndex)
                    if (found == -1) break
                    ranges.add(found until (found + lowerTerm.length))
                    startIndex = found + lowerTerm.length
                }
            }

            // Merge overlapping or adjacent ranges
            val mergedRanges = mutableListOf<IntRange>()
            ranges.sortBy { it.first }
            for (range in ranges) {
                if (mergedRanges.isEmpty()) {
                    mergedRanges.add(range)
                } else {
                    val last = mergedRanges.last()
                    if (range.first <= last.last + 1) {
                        mergedRanges[mergedRanges.size - 1] = last.first..maxOf(last.last, range.last)
                    } else {
                        mergedRanges.add(range)
                    }
                }
            }

            var curr = 0
            for (range in mergedRanges) {
                if (range.first > curr) {
                    append(text.substring(curr, range.first))
                }
                val matchEnd = (range.last + 1).coerceAtMost(text.length)
                pushStyle(
                    SpanStyle(
                        fontWeight = FontWeight.ExtraBold,
                        color = highlightColor,
                        background = highlightBackground
                    )
                )
                append(text.substring(range.first, matchEnd))
                pop()
                curr = matchEnd
            }
            if (curr < text.length) {
                append(text.substring(curr))
            }
        }
    }

    Text(
        text = annotated,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier
    )
}
