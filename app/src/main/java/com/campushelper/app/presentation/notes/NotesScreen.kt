package com.campushelper.app.presentation.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campushelper.app.presentation.components.CampusCard
import com.campushelper.app.presentation.components.CampusEmptyState
import com.campushelper.app.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Notes screen (Phase 3.3) — the first repository-backed screen in Hapsay.
 *
 * It is a pure view: it collects [NotesUiState] from [NotesViewModel] (which
 * owns all data access via the repository) and reports user intents back up.
 * No Room/DAO/entity types cross this boundary. The single Scaffold and bottom
 * bar still live in [com.campushelper.app.presentation.CampusHelperApp]; this
 * composable is only content, plus a floating action button aligned inside its
 * own bounds so it never appears on other tabs.
 */
@Composable
fun NotesScreen(
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel? = null,
    onAddNote: () -> Unit = {},
    onNoteClick: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val resolvedViewModel: NotesViewModel = viewModel ?: viewModel { NotesViewModel.create(context) }
    val uiState by resolvedViewModel.uiState.collectAsStateWithLifecycle()
    val errorResId = uiState.errorResId

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Header -------------------------------------------------------
            Text(
                text = "Notes",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.extraSmall))
            Text(
                text = "Keep your thoughts organized.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.smallMedium))

            // --- Search -------------------------------------------------------
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = resolvedViewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search notes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { resolvedViewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                            )
                        }
                    }
                },
                placeholder = {
                    Text(
                        text = "Search notes",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                label = {
                    Text(text = "Search")
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                ),
            )

            Spacer(Modifier.height(Spacing.smallMedium))

            Text(
                text = "All Notes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(Spacing.small))

            // --- Content area (fills remaining space, scrolls independently) ---
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    uiState.isLoading -> LoadingState()
                    errorResId != null -> ErrorState(message = stringResource(errorResId))
                    uiState.isEmpty -> CampusEmptyState(
                        icon = Icons.Filled.Description,
                        title = "No notes yet",
                        subtitle = "Create your first note to keep important information in one place.",
                    )
                    uiState.hasSearchNoResults -> CampusEmptyState(
                        icon = Icons.Filled.SearchOff,
                        title = "No matching notes",
                        subtitle = "Try a different title or keyword.",
                        actionLabel = "Clear search",
                        onActionClick = { resolvedViewModel.setSearchQuery("") },
                    )
                    else -> NotesList(notes = uiState.notes, onNoteClick = onNoteClick)
                }
            }
        }

        // --- Floating action button (entry point for the future editor) -------
        FloatingActionButton(
            onClick = onAddNote,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.medium),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add note",
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
        )
    }
}

@Composable
private fun ErrorState(message: String) {
    CampusEmptyState(
        icon = Icons.Filled.ErrorOutline,
        title = "Couldn't load notes",
        subtitle = "$message We'll retry automatically.",
    )
}

@Composable
private fun NotesList(notes: List<com.campushelper.app.domain.model.Note>, onNoteClick: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.smallMedium),
    ) {
        items(
            items = notes,
            key = { note -> note.id },
        ) { note ->
            NoteCard(note = note, onClick = { onNoteClick(note.id) })
        }
    }
}

@Composable
private fun NoteCard(note: com.campushelper.app.domain.model.Note, onClick: () -> Unit) {
    CampusCard(
        modifier = Modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(Spacing.smallMedium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null, // decorative beside the title
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp),
            )
            Spacer(Modifier.width(Spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Untitled note" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.content.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.extraSmall))
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(Spacing.small))
                Text(
                    text = formatUpdatedAt(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatUpdatedAt(epochMillis: Long): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val zoned = instant.atZone(ZoneId.systemDefault())
    return zoned.format(
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()),
    )
}
