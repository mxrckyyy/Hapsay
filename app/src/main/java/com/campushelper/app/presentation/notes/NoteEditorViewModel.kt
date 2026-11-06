package com.campushelper.app.presentation.notes

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campushelper.app.R
import com.campushelper.app.core.di.ServiceLocator
import com.campushelper.app.data.repository.NotesRepository
import com.campushelper.app.domain.model.Note
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state of the note editor. The same screen serves two modes:
 *  - create mode: [noteId] is null, fields start blank;
 *  - edit mode:   [noteId] is set, fields are populated from the repository.
 *
 * `initialTitle`/`initialContent` are the last *saved* snapshot; comparing the
 * live fields against them drives `hasUnsavedChanges` without a second source.
 * `createdAt` is carried through edit mode so the repository can preserve it.
 */
data class NoteEditorUiState(
    val title: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    @field:StringRes val errorResId: Int? = null,
    val noteNotFound: Boolean = false,
    val isEditing: Boolean = false,
    val initialTitle: String = "",
    val initialContent: String = "",
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
) {
    val hasUnsavedChanges: Boolean
        get() = title != initialTitle || content != initialContent

    /** Saving is allowed only with non-blank content and while not busy. */
    val canSave: Boolean
        get() = (title.isNotBlank() || content.isNotBlank()) && !isSaving && !isLoading && !isDeleting && !noteNotFound

    /** Deletion is allowed only for an existing, loaded, idle note. */
    val canDelete: Boolean
        get() = isEditing && !isLoading && !isSaving && !isDeleting && !noteNotFound
}

/**
 * ViewModel for creating and editing a single note (Phase 3.4).
 *
 * It owns only editor-specific state (draft text, dirty flag, save status) and
 * delegates every byte of persistence to [NotesRepository]; no Room/DAO/entity
 * types cross this boundary. Create vs. edit is decided once from [noteId].
 */
class NoteEditorViewModel(
    private val repository: NotesRepository,
    private val noteId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NoteEditorUiState(isLoading = noteId != null, isEditing = noteId != null),
    )

    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        if (noteId != null) {
            viewModelScope.launch { loadExistingNote(noteId) }
        }
    }

    private suspend fun loadExistingNote(id: Long) {
        try {
            val note = repository.observeNote(id).first()
            if (note == null) {
                _uiState.update { current ->
                    current.copy(isLoading = false, noteNotFound = true)
                }
            } else {
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        title = note.title,
                        content = note.content,
                        initialTitle = note.title,
                        initialContent = note.content,
                        createdAt = note.createdAt,
                        updatedAt = note.updatedAt,
                    )
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            _uiState.update { current ->
                current.copy(isLoading = false, errorResId = R.string.note_error_load_failed)
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { current -> current.copy(title = value) }
    }

    fun onContentChange(value: String) {
        _uiState.update { current -> current.copy(content = value) }
    }

    /**
     * Persists the draft. [onSuccess] runs on the main thread after a successful
     * write; the screen uses it to navigate back. Duplicate calls while saving
     * are ignored. On failure the draft is preserved and an error is shown.
     */
    fun save(onSuccess: () -> Unit) {
        val snapshot = _uiState.value
        if (!snapshot.canSave || snapshot.isSaving) return

        _uiState.update { current -> current.copy(isSaving = true, errorResId = null) }

        viewModelScope.launch {
            try {
                if (noteId == null) {
                    repository.createNote(title = snapshot.title, content = snapshot.content)
                } else {
                    repository.updateNote(
                        Note(
                            id = noteId,
                            title = snapshot.title,
                            content = snapshot.content,
                            createdAt = snapshot.createdAt ?: 0L,
                            updatedAt = 0L, // stamped by the repository
                        ),
                    )
                }
                // Mark the draft clean so back-navigation no longer warns.
                _uiState.update { current ->
                    current.copy(
                        isSaving = false,
                        initialTitle = snapshot.title,
                        initialContent = snapshot.content,
                    )
                }
                onSuccess()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update { current ->
                    current.copy(isSaving = false, errorResId = R.string.note_error_save_failed)
                }
            }
        }
    }

    /**
     * Permanently deletes the note (edit mode only). [onSuccess] runs on the
     * main thread after a successful delete; the screen uses it to navigate
     * back. The confirmation dialog stays open with a spinner while deleting;
     * on failure the dialog is dismissed by the caller and an error is shown,
     * with the draft preserved.
     */
    fun deleteNote(onSuccess: () -> Unit) {
        val id = noteId ?: return // create mode: nothing to delete
        val snapshot = _uiState.value
        if (!snapshot.canDelete || snapshot.isDeleting) return

        _uiState.update { current -> current.copy(isDeleting = true, errorResId = null) }

        viewModelScope.launch {
            try {
                repository.deleteNote(id)
                _uiState.update { current -> current.copy(isDeleting = false) }
                onSuccess()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update { current ->
                    current.copy(isDeleting = false, errorResId = R.string.note_error_delete_failed)
                }
            }
        }
    }

    companion object {
        fun create(context: Context, noteId: Long?): NoteEditorViewModel =
            NoteEditorViewModel(ServiceLocator.provideNotesRepository(context), noteId)
    }
}
