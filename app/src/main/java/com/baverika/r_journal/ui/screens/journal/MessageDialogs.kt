package com.baverika.r_journal.ui.screens.journal

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * ModalBottomSheet for message long-press options (Copy, Edit, Delete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageOptionsSheet(
    isCurrentEntryToday: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Message Options",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Copy
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                label = { Text("Copy Message") },
                selected = false,
                onClick = onCopy,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            if (isCurrentEntryToday) {
                // Edit
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    label = { Text("Edit Message") },
                    selected = false,
                    onClick = onEdit,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Delete
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    label = { Text("Delete Message", color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = onDelete,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

/**
 * Edit message dialog.
 */
@Composable
fun EditMessageDialog(
    editTextValue: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Message") },
        text = {
            OutlinedTextField(
                value = editTextValue,
                onValueChange = onTextChange,
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                maxLines = 5
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Delete message confirmation dialog.
 */
@Composable
fun DeleteMessageDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Message") },
        text = { Text("Are you sure you want to delete this message?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * ModalBottomSheet for media selection (Camera / Gallery).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerSheet(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Add Media",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Camera, contentDescription = null) },
                label = { Text("Take Photo") },
                selected = false,
                onClick = onTakePhoto,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Image, contentDescription = null) },
                label = { Text("Choose from Gallery") },
                selected = false,
                onClick = onChooseFromGallery,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

/**
 * Exit confirmation dialog for unsaved changes.
 */
@Composable
fun ExitConfirmationDialog(
    onDiscard: () -> Unit,
    onKeepWriting: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepWriting,
        title = { Text("Discard Unsaved Changes?") },
        text = { Text("You have unsaved text. Are you sure you want to leave?") },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepWriting) {
                Text("Keep Writing")
            }
        }
    )
}
