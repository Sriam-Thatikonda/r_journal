package com.baverika.r_journal

import androidx.compose.ui.text.TextRange
import com.baverika.r_journal.data.local.dao.QuickNoteDao
import com.baverika.r_journal.data.local.entity.QuickNote
import com.baverika.r_journal.data.model.BlockType
import com.baverika.r_journal.data.model.RichContent
import com.baverika.r_journal.data.model.SpanType
import com.baverika.r_journal.repository.QuickNoteRepository
import com.baverika.r_journal.ui.viewmodel.QuickNoteEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickNoteEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeQuickNoteDao : QuickNoteDao {
        val notes = mutableMapOf<String, QuickNote>()

        override fun getAllNotes(): Flow<List<QuickNote>> = flowOf(notes.values.toList())
        override fun searchNotes(query: String): Flow<List<QuickNote>> = flowOf(notes.values.filter { it.title.contains(query) || it.content.contains(query) })
        override suspend fun getNoteById(id: String): QuickNote? = notes[id]
        override suspend fun insertNote(note: QuickNote) { notes[note.id] = note }
        override suspend fun updateNote(note: QuickNote) { notes[note.id] = note }
        override suspend fun deleteNote(note: QuickNote) { notes.remove(note.id) }
    }

    private lateinit var fakeDao: FakeQuickNoteDao
    private lateinit var repository: QuickNoteRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeQuickNoteDao()
        repository = QuickNoteRepository(fakeDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun autosave_persistsNoteToRepositoryWithoutManualSave() = runTest(testDispatcher) {
        val viewModel = QuickNoteEditorViewModel(repository, initialNoteId = null)

        viewModel.onTitleChanged("My AutoSaved Note")
        viewModel.onBlockTextChanged(0, "Content that saves automatically", TextRange(32))

        // Before debounce delay (e.g. 200ms), note is not yet in repository
        testDispatcher.scheduler.advanceTimeBy(200)
        assertTrue(fakeDao.notes.isEmpty())

        // After debounce delay (advance past 600ms)
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, fakeDao.notes.size)
        val saved = fakeDao.notes.values.first()
        assertEquals("My AutoSaved Note", saved.title)

        val richContent = RichContent.fromContentString(saved.content)
        assertEquals("Content that saves automatically", richContent.blocks[0].text)
    }

    @Test
    fun emptyGhostNote_isNotPersisted() = runTest(testDispatcher) {
        val viewModel = QuickNoteEditorViewModel(repository, initialNoteId = null)

        // Advance time - note has no title and empty block
        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()
        viewModel.saveImmediately()
        testDispatcher.scheduler.runCurrent()

        // Repository remains empty because ghost notes are suppressed
        assertTrue(fakeDao.notes.isEmpty())
    }

    @Test
    fun textColor_appliesOnlyToSelectedSubstring() = runTest(testDispatcher) {
        val viewModel = QuickNoteEditorViewModel(repository, initialNoteId = null)

        val fullText = "This is normal and red text and normal again."
        // "red text" is at index 19 until 27
        viewModel.onBlockTextChanged(0, fullText, TextRange(fullText.length))

        // User selects only "red text"
        viewModel.onBlockFocusChanged(0, TextRange(19, 27))
        viewModel.applyFormatting(SpanType.COLOR, "#EF4444")

        val state = viewModel.state.value
        val block = state.blocks[0]
        assertEquals(1, block.spans.size)

        val colorSpan = block.spans[0]
        assertEquals(SpanType.COLOR, colorSpan.type)
        assertEquals("#EF4444", colorSpan.colorHex)
        assertEquals(19, colorSpan.start)
        assertEquals(27, colorSpan.end)

        // Verify the formatted substring
        assertEquals("red text", fullText.substring(colorSpan.start, colorSpan.end))
    }

    @Test
    fun differentColors_acrossMultipleParagraphs() = runTest(testDispatcher) {
        val viewModel = QuickNoteEditorViewModel(repository, initialNoteId = null)

        // Block 0: Red text
        viewModel.onBlockTextChanged(0, "Red paragraph", TextRange(0, 13))
        viewModel.applyFormatting(SpanType.COLOR, "#EF4444")

        // Block 1: Blue text
        viewModel.addNewBlockAfter(0, BlockType.PARAGRAPH)
        viewModel.onBlockTextChanged(1, "Blue paragraph", TextRange(0, 14))
        viewModel.applyFormatting(SpanType.COLOR, "#3B82F6")

        val state = viewModel.state.value
        assertEquals(2, state.blocks.size)

        assertEquals("#EF4444", state.blocks[0].spans[0].colorHex)
        assertEquals("#3B82F6", state.blocks[1].spans[0].colorHex)
    }

    @Test
    fun bullets_enterCreatesNextBullet_emptyBulletExitsList() = runTest(testDispatcher) {
        val viewModel = QuickNoteEditorViewModel(repository, initialNoteId = null)

        // Turn block 0 into bullet
        viewModel.setBlockType(BlockType.BULLET)
        viewModel.onBlockTextChanged(0, "Item one", TextRange(8))
        assertEquals(BlockType.BULLET, viewModel.state.value.blocks[0].type)

        // Press Enter at end of "Item one" -> creates second bullet
        viewModel.onEnterPressed(0, splitPosition = 8)

        val stateAfterEnter1 = viewModel.state.value
        assertEquals(2, stateAfterEnter1.blocks.size)
        assertEquals(BlockType.BULLET, stateAfterEnter1.blocks[1].type)
        assertEquals("", stateAfterEnter1.blocks[1].text)

        // Press Enter on the empty bullet -> converts it to normal PARAGRAPH
        viewModel.onEnterPressed(1, splitPosition = 0)

        val stateAfterEnter2 = viewModel.state.value
        assertEquals(2, stateAfterEnter2.blocks.size)
        assertEquals(BlockType.BULLET, stateAfterEnter2.blocks[0].type)
        assertEquals(BlockType.PARAGRAPH, stateAfterEnter2.blocks[1].type)
    }

    @Test
    fun checklist_toggleUpdatesStateAndPersists() = runTest(testDispatcher) {
        val viewModel = QuickNoteEditorViewModel(repository, initialNoteId = null)

        viewModel.setBlockType(BlockType.CHECKLIST)
        viewModel.onBlockTextChanged(0, "Buy groceries", TextRange(13))
        assertFalse(viewModel.state.value.blocks[0].isChecked)

        // Toggle checkbox
        val blockId = viewModel.state.value.blocks[0].id
        viewModel.toggleChecklist(blockId)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.state.value.blocks[0].isChecked)

        // Check repository has persisted the checked state
        val savedNote = fakeDao.notes.values.first()
        val richContent = RichContent.fromContentString(savedNote.content)
        assertTrue(richContent.blocks[0].isChecked)
    }

    @Test
    fun undoAndRedo_restoresEditorState() = runTest(testDispatcher) {
        val viewModel = QuickNoteEditorViewModel(repository, initialNoteId = null)

        viewModel.onTitleChanged("Version 1")
        testDispatcher.scheduler.advanceTimeBy(700)
        testDispatcher.scheduler.runCurrent()

        viewModel.onBlockTextChanged(0, "Text 1", TextRange(6))
        testDispatcher.scheduler.advanceTimeBy(700)
        testDispatcher.scheduler.runCurrent()

        viewModel.onTitleChanged("Version 2")
        testDispatcher.scheduler.advanceTimeBy(700)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.state.value.canUndo)
        viewModel.undo()

        assertEquals("Text 1", viewModel.state.value.blocks[0].text)
        assertEquals("Version 1", viewModel.state.value.title)
        assertTrue(viewModel.state.value.canRedo)

        viewModel.redo()
        assertEquals("Version 2", viewModel.state.value.title)
    }

    @Test
    fun saveImmediately_guaranteesSaveOnExit() = runTest(testDispatcher) {
        val viewModel = QuickNoteEditorViewModel(repository, initialNoteId = null)

        viewModel.onTitleChanged("Important Note")
        viewModel.onBlockTextChanged(0, "Will be saved instantly before exit", TextRange(35))

        // Before any debounce time elapses, call saveImmediately() (simulating BackHandler / onPause)
        viewModel.saveImmediately()
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, fakeDao.notes.size)
        assertEquals("Important Note", fakeDao.notes.values.first().title)
    }
}
