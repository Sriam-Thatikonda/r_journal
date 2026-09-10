package com.baverika.r_journal.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baverika.r_journal.ui.screens.editor.QuickNoteEditorScreen
import com.baverika.r_journal.ui.viewmodel.QuickNoteEditorViewModel
import com.baverika.r_journal.ui.viewmodel.QuickNoteEditorViewModelFactory
import com.baverika.r_journal.ui.viewmodel.QuickNoteViewModel

@Composable
fun EditNoteScreen(
    noteId: String,
    viewModel: QuickNoteViewModel,
    navController: NavController
) {
    val editorViewModel: QuickNoteEditorViewModel = viewModel(
        key = "editor_$noteId",
        factory = QuickNoteEditorViewModelFactory(
            repository = viewModel.repository,
            initialNoteId = noteId
        )
    )

    QuickNoteEditorScreen(
        viewModel = editorViewModel,
        navController = navController
    )
}
