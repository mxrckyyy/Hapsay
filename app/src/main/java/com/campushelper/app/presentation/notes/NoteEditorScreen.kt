package com.campushelper.app.presentation.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campushelper.app.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Note editor (Phase 3.4): create and edit a single note.
 *
 * The same composable serves both modes, chosen by [noteId]:
 *  - null  -> create mode (blank draft, title "New Note")
 *  - set   -> edit mode (draft loaded from the repository, title "Edit Note")
 *
 * All persistence goes through [NoteEditorViewModel] -> [com.campushelper.app.data.repository.NotesRepository];
 * no Room/DAO/entity types cross this boundary. The screen is pure content:
 * the single Scaffold and bottom bar still live in CampusHelperApp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteEditorViewModel? = null,
) {
    val context = LocalContext.current
    val resolvedViewModel: NoteEditorViewModel =
        viewModel ?: viewModel { NoteEditorViewModel.create(context, noteId) }
    val uiState by resolvedViewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Request focus on the title in create mode for a writing-first feel.
    LaunchedEffect(noteId) {
        if (noteId == null) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    // Dismiss the delete dialog automatically if deletion fails (error shown in body).
    LaunchedEffect(showDeleteDialog, uiState.isDeleting, uiState.errorResId) {
        if (showDeleteDialog && !uiState.isDeleting && uiState.errorResId != null) {
            showDeleteDialog = false
        }
    }

    val handleBack: () -> Unit = {
        if (uiState.hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    // System back button: intercept only when there is something to lose.
    BackHandler(enabled = uiState.hasUnsavedChanges && !uiState.isDeleting) {
        showDiscardDialog = true
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = if (noteId == null) "New Note" else "Edit Note",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            navigationIcon = {
                IconButton(onClick = handleBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            actions = {
                if (uiState.isEditing) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = uiState.canDelete,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete note",
                            tint = if (uiState.canDelete) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                        )
                    }
                }
                if (uiState.isSaving || uiState.isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(horizontal = Spacing.small),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    IconButton(
                        onClick = { resolvedViewModel.save(onSuccess = onBack) },
                        enabled = uiState.canSave,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Save note",
                            tint = if (uiState.canSave) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            uiState.noteNotFound -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "This note could not be found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.medium),
                ) {
                    // --- Title ----------------------------------------------------
                    BasicTextField(
                        value = uiState.title,
                        onValueChange = resolvedViewModel::onTitleChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next,
                        ),
                        decorationBox = { inner ->
                            if (uiState.title.isEmpty()) {
                                Text(
                                    text = "Note title",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            inner()
                        },
                    )

                    // --- Subtle metadata (edit mode only) -------------------------
                    uiState.updatedAt?.let { updated ->
                        Spacer(Modifier.height(Spacing.extraSmall))
                        Text(
                            text = "Last updated ${formatUpdatedAt(updated)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }

                    // Error message (honest, never a stack trace)
                    uiState.errorResId?.let { resId ->
                        Spacer(Modifier.height(Spacing.small))
                        Text(
                            text = androidx.compose.ui.res.stringResource(resId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(Modifier.height(Spacing.smallMedium))

                    // --- Content (writing space) ---------------------------------
                    BasicTextField(
                        value = uiState.content,
                        onValueChange = resolvedViewModel::onContentChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default,
                        ),
                        decorationBox = { inner ->
                            if (uiState.content.isEmpty()) {
                                Text(
                                    text = "Start writing your note...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            inner()
                        },
                    )

                    Spacer(Modifier.height(Spacing.small))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isDeleting) showDeleteDialog = false },
            title = { Text("Delete note?") },
            text = {
                Text("This note will be permanently deleted. This action cannot be undone.")
            },
            confirmButton = {
                if (uiState.isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    TextButton(
                        onClick = {
                            resolvedViewModel.deleteNote(onSuccess = {
                                showDeleteDialog = false
                                onBack()
                            })
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !uiState.isDeleting,
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = {
                Text("You have unsaved changes. If you leave now, they will be lost.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) {
                    Text("Discard Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing")
                }
            },
        )
    }
}

private fun formatUpdatedAt(epochMillis: Long): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val zoned = instant.atZone(ZoneId.systemDefault())
    return zoned.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}
