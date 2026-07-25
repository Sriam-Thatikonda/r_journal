// app/src/main/java/com/baverika/r_journal/ui/screens/Screen.kt

package com.baverika.r_journal.ui.screens

sealed class Screen {
    object Journal : Screen() // Represents the Archive Screen
    object ChatInput : Screen() // Represents the Chat Input Screen for today
    object QuickNotes : Screen()
    object Search : Screen()
    object Dashboard : Screen()
    object Mine : Screen()
    object Export : Screen()
    object Import : Screen()
}