package com.baverika.r_journal.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.UUID

enum class BlockType {
    @SerializedName("PARAGRAPH")
    PARAGRAPH,
    @SerializedName("BULLET")
    BULLET,
    @SerializedName("NUMBERED")
    NUMBERED,
    @SerializedName("CHECKLIST")
    CHECKLIST
}

enum class SpanType {
    @SerializedName("BOLD")
    BOLD,
    @SerializedName("ITALIC")
    ITALIC,
    @SerializedName("UNDERLINE")
    UNDERLINE,
    @SerializedName("STRIKETHROUGH")
    STRIKETHROUGH,
    @SerializedName("COLOR")
    COLOR
}

data class RichSpan(
    @SerializedName("start") val start: Int,
    @SerializedName("end") val end: Int,
    @SerializedName("type") val type: SpanType,
    @SerializedName("colorHex") val colorHex: String? = null
) {
    init {
        require(start >= 0) { "start must be >= 0, was $start" }
        require(end >= start) { "end must be >= start, was start=$start end=$end" }
    }
}

data class RichBlock(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("type") val type: BlockType = BlockType.PARAGRAPH,
    @SerializedName("text") val text: String = "",
    @SerializedName("isChecked") val isChecked: Boolean = false,
    @SerializedName("spans") val spans: List<RichSpan> = emptyList()
) {
    fun toggleChecked(): RichBlock {
        return if (type == BlockType.CHECKLIST) {
            copy(isChecked = !isChecked)
        } else {
            this
        }
    }
}

data class RichContent(
    @SerializedName("blocks") val blocks: List<RichBlock> = listOf(RichBlock()),
    @SerializedName("version") val version: Int = 1
) {
    fun toPlainText(): String {
        return blocks.mapIndexed { index, block ->
            when (block.type) {
                BlockType.CHECKLIST -> if (block.isChecked) "[x] ${block.text}" else "[ ] ${block.text}"
                BlockType.BULLET -> "\u2022 ${block.text}"
                BlockType.NUMBERED -> "${index + 1}. ${block.text}"
                BlockType.PARAGRAPH -> block.text
            }
        }.joinToString("\n")
    }

    fun hasChecklists(): Boolean {
        return blocks.any { it.type == BlockType.CHECKLIST }
    }

    fun checklistStats(): Pair<Int, Int> {
        val checklists = blocks.filter { it.type == BlockType.CHECKLIST }
        val checked = checklists.count { it.isChecked }
        return Pair(checked, checklists.size)
    }

    fun toggleChecklist(blockId: String): RichContent {
        return copy(
            blocks = blocks.map {
                if (it.id == blockId) it.toggleChecked() else it
            }
        )
    }

    fun toJson(): String {
        return gson.toJson(this)
    }

    companion object {
        private val gson = Gson()

        fun fromJson(json: String): RichContent {
            return gson.fromJson(json, RichContent::class.java)
        }

        fun fromContentString(content: String): RichContent {
            if (content.isBlank()) return RichContent(listOf(RichBlock()))
            val trimmed = content.trim()
            if (trimmed.startsWith("{") && trimmed.contains("\"blocks\"")) {
                try {
                    val parsed = fromJson(trimmed)
                    if (parsed.blocks.isNotEmpty()) {
                        return parsed
                    }
                } catch (_: Exception) {
                    // Fall back to plain text parsing
                }
            }
            return fromPlainText(content)
        }

        fun fromPlainText(text: String): RichContent {
            if (text.isEmpty()) return RichContent(listOf(RichBlock()))
            val lines = text.split("\n")
            val blocks = lines.map { rawLine ->
                val line = rawLine.trim()
                when {
                    line.startsWith("[x] ") || line.startsWith("[X] ") -> RichBlock(
                        type = BlockType.CHECKLIST,
                        text = rawLine.replaceFirst(Regex("^\\s*\\[[xX]\\]\\s*"), ""),
                        isChecked = true
                    )
                    line.startsWith("- [x] ") || line.startsWith("- [X] ") -> RichBlock(
                        type = BlockType.CHECKLIST,
                        text = rawLine.replaceFirst(Regex("^\\s*-\\s*\\[[xX]\\]\\s*"), ""),
                        isChecked = true
                    )
                    line.startsWith("[ ] ") -> RichBlock(
                        type = BlockType.CHECKLIST,
                        text = rawLine.replaceFirst(Regex("^\\s*\\[ \\]\\s*"), ""),
                        isChecked = false
                    )
                    line.startsWith("- [ ] ") -> RichBlock(
                        type = BlockType.CHECKLIST,
                        text = rawLine.replaceFirst(Regex("^\\s*-\\s*\\[ \\]\\s*"), ""),
                        isChecked = false
                    )
                    line.startsWith("\u2022 ") || line.startsWith("• ") || line.startsWith("* ") || line.startsWith("- ") -> RichBlock(
                        type = BlockType.BULLET,
                        text = rawLine.replaceFirst(Regex("^\\s*[\u2022•*-]\\s*"), "")
                    )
                    line.matches(Regex("^\\d+\\.\\s.*")) -> {
                        val dotIndex = rawLine.indexOf(". ")
                        RichBlock(
                            type = BlockType.NUMBERED,
                            text = if (dotIndex >= 0) rawLine.substring(dotIndex + 2) else rawLine
                        )
                    }
                    else -> RichBlock(
                        type = BlockType.PARAGRAPH,
                        text = rawLine
                    )
                }
            }
            return RichContent(blocks)
        }

        /**
         * Clamps existing span ranges when block text changes so formatting remains aligned and valid.
         * Modeled after the proven RNotes implementation.
         */
        @Suppress("UNUSED_PARAMETER")
        fun adjustSpans(
            oldText: String,
            newText: String,
            spans: List<RichSpan>
        ): List<RichSpan> {
            if (spans.isEmpty() || newText.isEmpty()) return emptyList()
            val newLength = newText.length
            return spans.filter { it.start < newLength }.map {
                it.copy(end = minOf(it.end, newLength))
            }
        }
    }
}
