// app/src/main/java/com/baverika/r_journal/ui/screens/NewQuickNoteScreen.kt

package com.baverika.r_journal.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baverika.r_journal.ui.screens.editor.QuickNoteEditorScreen
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.LocalAppTheme
import com.baverika.r_journal.ui.viewmodel.QuickNoteEditorViewModel
import com.baverika.r_journal.ui.viewmodel.QuickNoteEditorViewModelFactory
import com.baverika.r_journal.ui.viewmodel.QuickNoteViewModel

@Composable
fun NewQuickNoteScreen(
    viewModel: QuickNoteViewModel,
    navController: NavController
) {
    val currentTheme = LocalAppTheme.current

    val defaultNoteColor = when (currentTheme) {
        AppTheme.LIGHT         -> 0xFFE8EAED // Light gray
        AppTheme.CLOUD_DANCER  -> 0xFFF2F0E9 // Cloud Dancer warm cream
        else                   -> 0xFF000000 // Pure black for dark themes
    }

    val editorViewModel: QuickNoteEditorViewModel = viewModel(
        factory = QuickNoteEditorViewModelFactory(
            repository = viewModel.repository,
            initialNoteId = null,
            defaultNoteColor = defaultNoteColor
        )
    )

    QuickNoteEditorScreen(
        viewModel = editorViewModel,
        navController = navController
    )
}