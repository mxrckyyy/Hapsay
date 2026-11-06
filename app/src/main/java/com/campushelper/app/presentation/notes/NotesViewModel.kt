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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Immutable UI snapshot of the Notes feature. Only collected by the (future)
 * Notes screen, never by anything below the presentation layer.
 *
 * Deliberately small state: `isEmpty` and `hasSearchNoResults` are DERIVED
 * (stored state would drift from [notes]/[searchQuery]), so they are exposed
 * as computed properties instead of fields.
 *
 * Error policy (UI_UX_GUIDELINES §8.5 - loading/error/empty/success must all
 * be representable): an observation failure (list/search) surfaces
 * [com.campushelper.app.R.string.notes_error_load_failed] and the ViewModel
 * retries on its own; a write failure (create/update/delete) surfaces
 * [com.campushelper.app.R.string.notes_error_save_failed] while the list keeps
 * showing its last known state - never a fake success. Any subsequent
 * successful list emission clears the error again.
 */
data class NotesUiState(
    /** True until the first list emission arrives (honest loading state). */
    val isLoading: Boolean = true,

    /** Notes currently shown: all notes for a blank query, filtered otherwise. */
    val notes: List<Note> = emptyList(),

    /** Raw content of the search field - echoed verbatim, never trimmed. */
    val searchQuery: String = "",

    /** Set to one of the `notes_error_*` resources after a failed operation. */
    @field:StringRes
    val errorResId: Int? = null,
) {
    /** True when there is genuinely no data to show (no search running). */
    val isEmpty: Boolean
        get() = !isLoading && notes.isEmpty() && searchQuery.isBlank()

    /** True when a real search query matched zero notes. */
    val hasSearchNoResults: Boolean
        get() = !isLoading && notes.isEmpty() && searchQuery.isNotBlank()
}

/**
 * ViewModel of the Notes feature (first repository-backed ViewModel of the
 * app - the consumer that Phase 3.1's data foundation was built for).
 *
 * Data flow (dependency direction per ARCHITECTURE.md §4):
 *
 * ```
 * NotesScreen (implemented) -- collects StateFlow, reports intents
 *      |            ^
 *      v            |
 * supports this: NotesViewModel -- NotesRepository (interface only) -- ...
 * ```
 *
 * It never touches NoteDao/NoteEntity directly, and the repository is
 * injected here rather than fetched inside composables, so unit tests
 * substitute a fake (see NotesViewModelTest).
 *
 * Observation strategy: one while-loop collector in `viewModelScope` over
 * `searchQuery --debounce--> flatMapLatest --> repository.searchNotes`.
 *  - No `stateIn` duplication: the state already lives in `_uiState`
 *    (the app-wide convention from HomeViewModel); a second holder would
 *    risk divergence.
 *  - `flatMapLatest` cancels the previous repository flow the moment a newer
 *    query wins, so stale results can never overwrite fresh ones.
 *  - `debounce` (~300 ms) collapses fast typing into one repository call;
 *    MutableStateFlow itself already drops repeated equal queries.
 *  - Failures of the hot stream are caught and retried (repository flows are
 *    long-lived Room streams - they do not complete normally), so a transient
 *    storage hiccup degrades to the error state instead of crashing.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class) // flatMapLatest (cancel-stale) + debounce (FlowPreview API)
class NotesViewModel(
    private val repository: NotesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())

    /** Read-only state for the screen. Only this class writes to it. */
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    /** Reactive input behind the search field; conflated + debounced. */
    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch { observeNotesUntilCleared() }
    }

    /**
     * The single observation loop for the notes list. Runs for the whole
     * lifetime of the ViewModel: lives through query changes, repo updates
     * and error/retry cycles, and exits only on ViewModel clear.
     */
    private suspend fun observeNotesUntilCleared() {
        while (currentCoroutineContext().isActive) {
            try {
                notesForCurrentQuery()
                    .collect { recent ->
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                notes = recent,
                                // A worked operation is the best proof a
                                // previous failure is fixed; show no stale
                                // error next to fresh data.
                                errorResId = null,
                            )
                        }
                    }
                // Repository flows do not complete normally while the app
                // runs. Reaching here means the stream closed - stop cleanly
                // with the last known state instead of hot-looping.
                break
            } catch (cancellation: CancellationException) {
                throw cancellation // ViewModel cleared: real cancellation, not an error.
            } catch (failure: Exception) {
                // Not silent: the state records it, the future screen shows
                // it, and this loop self-heals after a short pause.
                _uiState.update { current ->
                    current.copy(isLoading = false, errorResId = R.string.notes_error_load_failed)
                }
                delay(RETRY_DELAY_MILLIS)
            }
        }
    }

    /**
     * Live notes view for whatever the query currently is. A blank query is
     * NOT special-cased here: `NotesRepository.searchNotes` already defines
     * blank/whitespace-only input as "all notes" (contract existed since the
     * data layer was implemented), so there is exactly one collection path.
     */
    private fun notesForCurrentQuery() =
        searchQuery
            .debounce(SEARCH_QUERY_DEBOUNCE_MILLIS)
            .flatMapLatest { query -> repository.searchNotes(query) }
            .distinctUntilChanged()

    /**
     * Called by the screen on every keystroke. The raw text goes into the
     * state (the field shows what the user typed - trimming is the
     * repository's job) and into the debounced observation chain.
     */
    fun setSearchQuery(query: String) {
        searchQuery.value = query
        _uiState.update { current -> current.copy(searchQuery = query) }
    }

    /**
     * Creates a note with verbatim text. The generated id is handed to
     * [onCreated] on the main thread after the repository call succeeds
     * (the future editor navigates with it; default empty keeps the call
     * site simple today). The list refreshes on its own via the observation
     * Flow - that is the whole refresh story, no manual reload.
     */
    fun createNote(title: String, content: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val createdId = repository.createNote(title = title, content = content)
                onCreated(createdId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update { current ->
                    current.copy(isLoading = false, errorResId = R.string.notes_error_save_failed)
                }
            }
        }
    }

    /**
     * Updates a note (text/contents arrive as a domain Note with id and
     * createdAt intact). Refresh happens through the repository Flow.
     */
    fun updateNote(note: Note) {
        viewModelScope.launch {
            try {
                repository.updateNote(note)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update { current ->
                    current.copy(isLoading = false, errorResId = R.string.notes_error_save_failed)
                }
            }
        }
    }

    /**
     * Deletes a note by id. Refresh happens through the repository Flow
     * (and `observeById` will emit null afterwards for an open editor).
     */
    fun deleteNote(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteNote(id)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update { current ->
                    current.copy(isLoading = false, errorResId = R.string.notes_error_save_failed)
                }
            }
        }
    }

    companion object {
        /** Search input debounce; short enough to feel live, long enough to batch typing. */
        private const val SEARCH_QUERY_DEBOUNCE_MILLIS = 300L

        /** Pause between observation-failure retries. */
        private const val RETRY_DELAY_MILLIS = 3_000L

        /**
         * Creation seam for the Phase 3.3 screen wiring (NOT wired into
         * navigation yet - Phase 3.2 delivers the ViewModel only):
         *
         * ```
         * // in the Notes composable, the composition root:
         * val viewModel: NotesViewModel =
         *     viewModel { NotesViewModel.create(LocalContext.current) }
         * ```
         *
         * `ServiceLocator` is intentionally resolved here (composition root
         * or later in the `viewModel { }` factory lambda - never inside
         * composables; see ARCHITECTURE.md §3-§4).
         */
        fun create(context: Context): NotesViewModel =
            NotesViewModel(ServiceLocator.provideNotesRepository(context))
    }
}
