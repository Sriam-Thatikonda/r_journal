package com.baverika.r_journal

import com.baverika.r_journal.data.model.BlockType
import com.baverika.r_journal.data.model.RichBlock
import com.baverika.r_journal.data.model.RichContent
import com.baverika.r_journal.data.model.RichSpan
import com.baverika.r_journal.data.model.SpanType
import org.junit.Assert.*
import org.junit.Test

class RichContentTest {

    @Test
    fun serializeAndDeserialize_preservesAllFormattingAndSpans() {
        val originalContent = RichContent(
            blocks = listOf(
                RichBlock(
                    id = "block-1",
                    type = BlockType.PARAGRAPH,
                    text = "This is normal and RED TEXT and bold text.",
                    isChecked = false,
                    spans = listOf(
                        RichSpan(start = 19, end = 27, type = SpanType.COLOR, colorHex = "#EF4444"),
                        RichSpan(start = 32, end = 41, type = SpanType.BOLD)
                    )
                ),
                RichBlock(
                    id = "block-2",
                    type = BlockType.BULLET,
                    text = "Bullet item with italic",
                    isChecked = false,
                    spans = listOf(
                        RichSpan(start = 17, end = 23, type = SpanType.ITALIC)
                    )
                ),
                RichBlock(
                    id = "block-3",
                    type = BlockType.CHECKLIST,
                    text = "Complete project",
                    isChecked = true,
                    spans = listOf(
                        RichSpan(start = 0, end = 8, type = SpanType.UNDERLINE)
                    )
                ),
                RichBlock(
                    id = "block-4",
                    type = BlockType.CHECKLIST,
                    text = "Buy groceries",
                    isChecked = false,
                    spans = emptyList()
                )
            )
        )

        val json = originalContent.toJson()
        assertNotNull(json)
        assertTrue(json.contains("\"blocks\""))
        assertTrue(json.contains("#EF4444"))

        val restored = RichContent.fromContentString(json)
        assertEquals(4, restored.blocks.size)

        // Block 1: Paragraph with Color and Bold
        val b1 = restored.blocks[0]
        assertEquals("block-1", b1.id)
        assertEquals(BlockType.PARAGRAPH, b1.type)
        assertEquals("This is normal and RED TEXT and bold text.", b1.text)
        assertEquals(2, b1.spans.size)
        assertEquals(SpanType.COLOR, b1.spans[0].type)
        assertEquals("#EF4444", b1.spans[0].colorHex)
        assertEquals(19, b1.spans[0].start)
        assertEquals(27, b1.spans[0].end)
        assertEquals(SpanType.BOLD, b1.spans[1].type)

        // Block 2: Bullet
        val b2 = restored.blocks[1]
        assertEquals(BlockType.BULLET, b2.type)
        assertEquals("Bullet item with italic", b2.text)
        assertEquals(1, b2.spans.size)
        assertEquals(SpanType.ITALIC, b2.spans[0].type)

        // Block 3: Checklist (checked)
        val b3 = restored.blocks[2]
        assertEquals(BlockType.CHECKLIST, b3.type)
        assertTrue(b3.isChecked)
        assertEquals("Complete project", b3.text)
        assertEquals(SpanType.UNDERLINE, b3.spans[0].type)

        // Block 4: Checklist (unchecked)
        val b4 = restored.blocks[3]
        assertEquals(BlockType.CHECKLIST, b4.type)
        assertFalse(b4.isChecked)
        assertEquals("Buy groceries", b4.text)
    }

    @Test
    fun fromPlainText_legacyNotesAreCorrectlyConverted() {
        val legacyText = """
            My Shopping List
            - [ ] Milk
            - [x] Eggs
            • Bread
            * Butter
            1. Clean kitchen
            Just a regular note
        """.trimIndent()

        val parsed = RichContent.fromPlainText(legacyText)
        assertEquals(7, parsed.blocks.size)

        assertEquals(BlockType.PARAGRAPH, parsed.blocks[0].type)
        assertEquals("My Shopping List", parsed.blocks[0].text)

        assertEquals(BlockType.CHECKLIST, parsed.blocks[1].type)
        assertEquals("Milk", parsed.blocks[1].text)
        assertFalse(parsed.blocks[1].isChecked)

        assertEquals(BlockType.CHECKLIST, parsed.blocks[2].type)
        assertEquals("Eggs", parsed.blocks[2].text)
        assertTrue(parsed.blocks[2].isChecked)

        assertEquals(BlockType.BULLET, parsed.blocks[3].type)
        assertEquals("Bread", parsed.blocks[3].text)

        assertEquals(BlockType.BULLET, parsed.blocks[4].type)
        assertEquals("Butter", parsed.blocks[4].text)

        assertEquals(BlockType.NUMBERED, parsed.blocks[5].type)
        assertEquals("Clean kitchen", parsed.blocks[5].text)

        assertEquals(BlockType.PARAGRAPH, parsed.blocks[6].type)
        assertEquals("Just a regular note", parsed.blocks[6].text)
    }

    @Test
    fun checklist_toggleAndStats() {
        val content = RichContent(
            blocks = listOf(
                RichBlock(id = "item-1", type = BlockType.CHECKLIST, text = "Task 1", isChecked = false),
                RichBlock(id = "item-2", type = BlockType.CHECKLIST, text = "Task 2", isChecked = true)
            )
        )

        assertTrue(content.hasChecklists())
        val (checked1, total1) = content.checklistStats()
        assertEquals(1, checked1)
        assertEquals(2, total1)

        val toggled = content.toggleChecklist("item-1")
        assertTrue(toggled.blocks.first { it.id == "item-1" }.isChecked)

        val (checked2, total2) = toggled.checklistStats()
        assertEquals(2, checked2)
        assertEquals(2, total2)
    }

    @Test
    fun spanAdjustment_insertionShiftsSpans() {
        val oldText = "Hello world"
        val spans = listOf(
            RichSpan(start = 6, end = 11, type = SpanType.BOLD) // "world"
        )

        // Insert "beautiful " before "world" at index 6
        val newText = "Hello beautiful world"
        val adjusted = RichContent.adjustSpans(oldText, newText, spans)

        assertEquals(1, adjusted.size)
        assertEquals(16, adjusted[0].start)
        assertEquals(21, adjusted[0].end)
        assertEquals(SpanType.BOLD, adjusted[0].type)
        assertEquals("world", newText.substring(adjusted[0].start, adjusted[0].end))
    }

    @Test
    fun spanAdjustment_typingInsideExpandsSpan() {
        val oldText = "Hello world"
        val spans = listOf(
            RichSpan(start = 6, end = 11, type = SpanType.COLOR, colorHex = "#EF4444") // "world"
        )

        // Insert "l" inside "world" -> "worlld"
        val newText = "Hello worlld"
        val adjusted = RichContent.adjustSpans(oldText, newText, spans)

        assertEquals(1, adjusted.size)
        assertEquals(6, adjusted[0].start)
        assertEquals(12, adjusted[0].end)
        assertEquals("worlld", newText.substring(adjusted[0].start, adjusted[0].end))
    }

    @Test
    fun toPlainText_producesReadableText() {
        val content = RichContent(
            blocks = listOf(
                RichBlock(type = BlockType.PARAGRAPH, text = "Header"),
                RichBlock(type = BlockType.BULLET, text = "Item A"),
                RichBlock(type = BlockType.CHECKLIST, text = "Todo 1", isChecked = false),
                RichBlock(type = BlockType.CHECKLIST, text = "Todo 2", isChecked = true)
            )
        )

        val plain = content.toPlainText()
        assertTrue(plain.contains("Header"))
        assertTrue(plain.contains("• Item A"))
        assertTrue(plain.contains("[ ] Todo 1"))
        assertTrue(plain.contains("[x] Todo 2"))
    }
}
