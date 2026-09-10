package com.baverika.r_journal.ui.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.local.entity.QuickNote
import com.baverika.r_journal.data.model.BlockType
import com.baverika.r_journal.data.model.RichBlock
import com.baverika.r_journal.data.model.RichContent
import com.baverika.r_journal.data.model.RichSpan
import com.baverika.r_journal.data.model.SpanType
import com.baverika.r_journal.repository.QuickNoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class QuickNoteEditorState(
    val noteId: String = UUID.randomUUID().toString(),
    val title: String = "",
    val blocks: List<RichBlock> = listOf(RichBlock()),
    val color: Long = 0xFF000000,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val activeBlockIndex: Int = 0,
    val activeSelection: TextRange = TextRange.Zero,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isSaving: Boolean = false,
    val isNewNote: Boolean = true,
    val isLoaded: Boolean = false
)

private data class EditorSnapshot(
    val title: String,
    val blocks: List<RichBlock>,
    val color: Long,
    val isPinned: Boolean
)

class QuickNoteEditorViewModel(
    private val repository: QuickNoteRepository,
    private val initialNoteId: String?,
    defaultNoteColor: Long = 0xFF000000
) : ViewModel() {

    private val _state = MutableStateFlow(
        QuickNoteEditorState(
            noteId = initialNoteId ?: UUID.randomUUID().toString(),
            color = defaultNoteColor,
            isNewNote = initialNoteId.isNullOrBlank()
        )
    )
    val state: StateFlow<QuickNoteEditorState> = _state.asStateFlow()

    private val undoStack = mutableListOf<EditorSnapshot>()
    private val redoStack = mutableListOf<EditorSnapshot>()

    private var autoSaveJob: Job? = null

    init {
        if (!initialNoteId.isNullOrBlank()) {
            loadNote(initialNoteId)
        } else {
            _state.value = _state.value.copy(isLoaded = true)
            recordSnapshot()
        }
    }

    private fun loadNote(id: String) {
        viewModelScope.launch {
            val note = repository.getNoteById(id)
            if (note != null) {
                val richContent = RichContent.fromContentString(note.content)
                val blocks = if (richContent.blocks.isEmpty()) listOf(RichBlock()) else richContent.blocks
                _state.value = _state.value.copy(
                    noteId = note.id,
                    title = note.title,
                    blocks = blocks,
                    color = note.color,
                    isPinned = note.isPinned,
                    createdAt = note.timestamp,
                    updatedAt = note.timestamp,
                    activeBlockIndex = 0,
                    isNewNote = false,
                    isLoaded = true
                )
                undoStack.clear()
                redoStack.clear()
                recordSnapshot()
            } else {
                _state.value = _state.value.copy(isLoaded = true)
                recordSnapshot()
            }
        }
    }

    private fun recordSnapshot() {
        val current = _state.value
        val snapshot = EditorSnapshot(
            title = current.title,
            blocks = current.blocks,
            color = current.color,
            isPinned = current.isPinned
        )
        if (undoStack.lastOrNull() != snapshot) {
            undoStack.add(snapshot)
            redoStack.clear()
            updateUndoRedoAvailability()
        }
    }

    private fun updateUndoRedoAvailability() {
        _state.value = _state.value.copy(
            canUndo = undoStack.size > 1,
            canRedo = redoStack.isNotEmpty()
        )
    }

    fun onTitleChanged(newTitle: String) {
        _state.value = _state.value.copy(
            title = newTitle,
            updatedAt = System.currentTimeMillis()
        )
        scheduleAutoSave()
    }

    fun onBlockTextChanged(index: Int, newText: String, selection: TextRange) {
        val currentBlocks = _state.value.blocks.toMutableList()
        if (index in currentBlocks.indices) {
            val oldBlock = currentBlocks[index]
            // Clamping spans predictably matching RNotes
            val adjustedSpans = oldBlock.spans.filter { it.start < newText.length }.map {
                it.copy(end = minOf(it.end, newText.length))
            }
            currentBlocks[index] = oldBlock.copy(text = newText, spans = adjustedSpans)
            _state.value = _state.value.copy(
                blocks = currentBlocks,
                activeBlockIndex = index,
                activeSelection = selection,
                updatedAt = System.currentTimeMillis()
            )
            scheduleAutoSave()
        }
    }

    fun onBlockFocusChanged(index: Int, selection: TextRange) {
        _state.value = _state.value.copy(
            activeBlockIndex = index,
            activeSelection = selection
        )
    }

    fun toggleChecklist(blockId: String) {
        recordSnapshot()
        val currentBlocks = _state.value.blocks.map {
            if (it.id == blockId) it.toggleChecked() else it
        }
        _state.value = _state.value.copy(
            blocks = currentBlocks,
            updatedAt = System.currentTimeMillis()
        )
        saveImmediately()
    }

    fun setBlockType(type: BlockType) {
        val index = _state.value.activeBlockIndex
        val currentBlocks = _state.value.blocks.toMutableList()
        if (index in currentBlocks.indices) {
            recordSnapshot()
            val oldBlock = currentBlocks[index]
            currentBlocks[index] = oldBlock.copy(
                type = if (oldBlock.type == type) BlockType.PARAGRAPH else type,
                isChecked = if (type == BlockType.CHECKLIST) oldBlock.isChecked else false
            )
            _state.value = _state.value.copy(
                blocks = currentBlocks,
                updatedAt = System.currentTimeMillis()
            )
            saveImmediately()
        }
    }

    fun applyFormatting(spanType: SpanType, colorHex: String? = null) {
        val index = _state.value.activeBlockIndex
        val selection = _state.value.activeSelection
        val currentBlocks = _state.value.blocks.toMutableList()

        if (index in currentBlocks.indices) {
            val block = currentBlocks[index]
            val text = block.text

            val (start, end) = if (selection.min != selection.max) {
                Pair(selection.min, selection.max)
            } else {
                findWordBoundaries(text, selection.start)
            }

            if (start < end && end <= text.length) {
                recordSnapshot()
                val currentSpans = block.spans.toMutableList()

                if (spanType == SpanType.COLOR) {
                    // Remove existing color spans overlapping this range
                    currentSpans.removeAll {
                        it.type == SpanType.COLOR && !(it.end <= start || it.start >= end)
                    }
                    if (colorHex != null) {
                        currentSpans.add(RichSpan(start = start, end = end, type = SpanType.COLOR, colorHex = colorHex))
                    }
                } else {
                    // Toggle style: if already covering this exact range, remove it; else add it
                    val existing = currentSpans.firstOrNull {
                        it.type == spanType && it.start <= start && it.end >= end
                    }
                    if (existing != null) {
                        currentSpans.remove(existing)
                    } else {
                        currentSpans.add(RichSpan(start = start, end = end, type = spanType))
                    }
                }

                currentBlocks[index] = block.copy(spans = currentSpans)
                _state.value = _state.value.copy(
                    blocks = currentBlocks,
                    updatedAt = System.currentTimeMillis()
                )
                saveImmediately()
            }
        }
    }

    private fun findWordBoundaries(text: String, cursor: Int): Pair<Int, Int> {
        if (text.isEmpty() || cursor < 0 || cursor > text.length) return Pair(0, 0)
        var start = cursor.coerceIn(0, text.length)
        while (start > 0 && !text[start - 1].isWhitespace()) {
            start--
        }
        var end = cursor.coerceIn(0, text.length)
        while (end < text.length && !text[end].isWhitespace()) {
            end++
        }
        return Pair(start, end)
    }

    fun onEnterPressed(index: Int) {
        val currentBlocks = _state.value.blocks.toMutableList()
        if (index !in currentBlocks.indices) return
        val block = currentBlocks[index]

        recordSnapshot()

        // If pressing enter on empty list/checklist/numbered item -> exit to normal paragraph
        if (block.text.isEmpty() && block.type != BlockType.PARAGRAPH) {
            currentBlocks[index] = block.copy(type = BlockType.PARAGRAPH)
            _state.value = _state.value.copy(
                blocks = currentBlocks,
                activeBlockIndex = index,
                activeSelection = TextRange.Zero,
                updatedAt = System.currentTimeMillis()
            )
            saveImmediately()
            return
        }

        // Continue list/checklist or create normal block
        val nextType = if (block.type == BlockType.CHECKLIST || block.type == BlockType.BULLET || block.type == BlockType.NUMBERED) {
            block.type
        } else {
            BlockType.PARAGRAPH
        }

        val insertIndex = (index + 1).coerceAtMost(currentBlocks.size)
        val newBlock = RichBlock(type = nextType)
        currentBlocks.add(insertIndex, newBlock)
        _state.value = _state.value.copy(
            blocks = currentBlocks,
            activeBlockIndex = insertIndex,
            activeSelection = TextRange.Zero,
            updatedAt = System.currentTimeMillis()
        )
        saveImmediately()
    }

    fun onBackspaceOnEmpty(index: Int) {
        val currentBlocks = _state.value.blocks.toMutableList()
        if (index !in currentBlocks.indices) return
        val block = currentBlocks[index]
        if (block.text.isNotEmpty()) return

        if (block.type != BlockType.PARAGRAPH) {
            recordSnapshot()
            // Revert list item to standard paragraph
            currentBlocks[index] = block.copy(type = BlockType.PARAGRAPH)
            _state.value = _state.value.copy(
                blocks = currentBlocks,
                activeBlockIndex = index,
                updatedAt = System.currentTimeMillis()
            )
            saveImmediately()
        } else if (currentBlocks.size > 1) {
            recordSnapshot()
            // Remove empty paragraph block and focus previous block
            currentBlocks.removeAt(index)
            val newActive = (index - 1).coerceAtLeast(0)
            val prevBlock = currentBlocks[newActive]
            _state.value = _state.value.copy(
                blocks = currentBlocks,
                activeBlockIndex = newActive,
                activeSelection = TextRange(prevBlock.text.length),
                updatedAt = System.currentTimeMillis()
            )
            saveImmediately()
        }
    }

    fun addNewBlockAfter(index: Int, type: BlockType = BlockType.PARAGRAPH) {
        recordSnapshot()
        val currentBlocks = _state.value.blocks.toMutableList()
        val insertIndex = (index + 1).coerceAtMost(currentBlocks.size)
        val newBlock = RichBlock(type = type)
        currentBlocks.add(insertIndex, newBlock)
        _state.value = _state.value.copy(
            blocks = currentBlocks,
            activeBlockIndex = insertIndex,
            activeSelection = TextRange.Zero,
            updatedAt = System.currentTimeMillis()
        )
        saveImmediately()
    }

    fun removeBlockAt(index: Int) {
        val currentBlocks = _state.value.blocks.toMutableList()
        if (currentBlocks.size > 1 && index in currentBlocks.indices) {
            recordSnapshot()
            currentBlocks.removeAt(index)
            val newActive = (index - 1).coerceAtLeast(0)
            _state.value = _state.value.copy(
                blocks = currentBlocks,
                activeBlockIndex = newActive,
                updatedAt = System.currentTimeMillis()
            )
            saveImmediately()
        }
    }

    fun setNoteColor(color: Long) {
        recordSnapshot()
        _state.value = _state.value.copy(
            color = color,
            updatedAt = System.currentTimeMillis()
        )
        saveImmediately()
    }

    fun togglePin() {
        recordSnapshot()
        _state.value = _state.value.copy(
            isPinned = !_state.value.isPinned,
            updatedAt = System.currentTimeMillis()
        )
        saveImmediately()
    }

    fun undo() {
        if (undoStack.size > 1) {
            val current = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(current)
            val previous = undoStack.last()

            _state.value = _state.value.copy(
                title = previous.title,
                blocks = previous.blocks,
                color = previous.color,
                isPinned = previous.isPinned,
                updatedAt = System.currentTimeMillis()
            )
            updateUndoRedoAvailability()
            saveImmediately()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(next)

            _state.value = _state.value.copy(
                title = next.title,
                blocks = next.blocks,
                color = next.color,
                isPinned = next.isPinned,
                updatedAt = System.currentTimeMillis()
            )
            updateUndoRedoAvailability()
            saveImmediately()
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            recordSnapshot()
            saveImmediately()
        }
    }

    fun saveImmediately() {
        viewModelScope.launch {
            val current = _state.value
            val hasContent = current.title.isNotBlank() || current.blocks.any { it.text.isNotBlank() }
            if (!hasContent && current.isNewNote) {
                return@launch
            }

            _state.value = _state.value.copy(isSaving = true)
            val contentJson = RichContent(blocks = current.blocks).toJson()
            val note = QuickNote(
                id = current.noteId,
                title = current.title, // Preserves empty title as "", not forcing "Untitled"
                content = contentJson,
                timestamp = current.updatedAt,
                color = current.color,
                isPinned = current.isPinned
            )
            repository.upsertNote(note)
            _state.value = _state.value.copy(
                isSaving = false,
                isNewNote = false
            )
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val current = _state.value
            val note = QuickNote(
                id = current.noteId,
                title = current.title,
                content = RichContent(blocks = current.blocks).toJson(),
                color = current.color,
                isPinned = current.isPinned
            )
            repository.deleteNote(note)
            onDeleted()
        }
    }

    class Factory(
        private val repository: QuickNoteRepository,
        private val initialNoteId: String?,
        private val defaultNoteColor: Long = 0xFF000000
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QuickNoteEditorViewModel(repository, initialNoteId, defaultNoteColor) as T
        }
    }
}
