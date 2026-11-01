package com.campushelper.app.presentation.notes

import com.campushelper.app.R
import com.campushelper.app.data.repository.NotesRepository
import com.campushelper.app.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ViewModel unit tests (Phase 3.2). The Notes feature is exercised through a
 * [FakeNotesRepository] - no Room, no device - per phase plan. The fake
 * mirrors the REAL repository contract:
 *  - `searchNotes` trims; blank/whitespace-only queries mean "all notes",
 *  - observation flows are live: each write mutates the backing list and the
 *    stream re-emits (as Room's table-invalidation Flow does),
 *  - a one-shot failure flag reproduces "transient error, then recovery".
 *
 * Main dispatcher is a [StandardTestDispatcher] so `viewModelScope` work is
 * deterministic and the 300 ms search debounce is driven by virtual time.
 */
class NotesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Test fixtures with distinct ids/timestamps so ordering checks stay meaningful. */
    private fun note(id: Long, title: String, content: String) =
        Note(id = id, title = title, content = content, createdAt = 1_000L + id, updatedAt = 1_000L + id)

    /**
     * Fake of the NotesRepository contract (not of Room): state-mirroring
     * writes, live re-emitting observation flows, one-shot failure injection.
     */
    private class FakeNotesRepository(initialNotes: List<Note> = emptyList()) : NotesRepository {

        /** The live "table": writes replace this list, streams re-emit it. */
        private val notesState = MutableStateFlow(initialNotes)

        val searchInputsRecord = mutableListOf<String>()
        val createRequestsRecord = mutableListOf<Pair<String, String>>()
        val updateRequestsRecord = mutableListOf<Note>()
        val deleteRequestsRecord = mutableListOf<Long>()

        /** One-shot failure injections (auto-consumed so retry paths unlock). */
        var failNextObservation: Exception? = null
        var failNextWrite: Exception? = null

        private var nextCreatedId = 42L

        override val notes: MutableStateFlow<List<Note>> = notesState

        override fun observeNote(id: Long) =
            notes.map { list -> list.firstOrNull { it.id == id } }

        override fun searchNotes(query: String): Flow<List<Note>> = flow {
            searchInputsRecord += query
            failNextObservation?.let { val failure = it; failNextObservation = null; throw failure }
            // Live view of the table, re-filtered on every table change
            // (emitAll of a never-completing upstream mirrors Room Flow).
            val needle = query.trim()
            emitAll(
                notesState.map { all ->
                    if (needle.isBlank()) {
                        all
                    } else {
                        // ASCII fixtures + contains(ignoreCase): mirrors the
                        // SQLite LIKE folding the DAO's tests verified.
                        all.filter {
                            it.title.contains(needle, ignoreCase = true) ||
                                it.content.contains(needle, ignoreCase = true)
                        }
                    }
                },
            )
        }

        override suspend fun createNote(title: String, content: String): Long {
            failNextWrite?.let { val failure = it; failNextWrite = null; throw failure }
            createRequestsRecord += title to content
            val id = nextCreatedId++
            notesState.value = notesState.value + Note(id, title, content, createdAt = 1L, updatedAt = 1L)
            return id
        }

        override suspend fun updateNote(note: Note) {
            failNextWrite?.let { val failure = it; failNextWrite = null; throw failure }
            updateRequestsRecord += note
            notesState.value = notesState.value.map { if (it.id == note.id) note else it }
        }

        override suspend fun deleteNote(id: Long) {
            failNextWrite?.let { val failure = it; failNextWrite = null; throw failure }
            deleteRequestsRecord += id
            notesState.value = notesState.value.filter { it.id != id }
        }
    }

    // --- Loading / empty / loaded -------------------------------------------

    @Test
    fun initialState_beforeAnyEmission_isHonestLoading() = runTest(testDispatcher) {
        val viewModel = NotesViewModel(FakeNotesRepository())
        // Virtual time has not advanced: no repository call processed yet.
        assertTrue(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isEmpty)
        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun loadedNotes_surfaceWithoutLoadingAndWithoutError() = runTest(testDispatcher) {
        val fake = FakeNotesRepository(
            listOf(note(1, "Biology", "cell structure"), note(2, "Grocery", "eggs, milk, bread")),
        )
        val viewModel = NotesViewModel(fake)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertFalse(state.hasSearchNoResults)
        assertNull(state.errorResId)
        assertEquals(2, state.notes.size)
        assertEquals("Biology", state.notes[0].title)
    }

    @Test
    fun noNotesAtAll_showEmptyState_notSearchNoResults() = runTest(testDispatcher) {
        val viewModel = NotesViewModel(FakeNotesRepository())

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertFalse(state.hasSearchNoResults)
    }

    // --- Search --------------------------------------------------------------

    @Test
    fun searchQuery_updatesEchoInState_andFiltersResults() = runTest(testDispatcher) {
        val fake = FakeNotesRepository(
            listOf(note(1, "Biology", "cell structure"), note(2, "Grocery", "eggs, milk, bread")),
        )
        val viewModel = NotesViewModel(fake)
        advanceUntilIdle()

        viewModel.setSearchQuery("  ")
        advanceUntilIdle()

        viewModel.setSearchQuery("BIO") // case-foldingLike the real DAO search
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("BIO", state.searchQuery)
        assertEquals(listOf("Biology"), state.notes.map { it.title })
        assertFalse(state.hasSearchNoResults)
    }

    @Test
    fun whitespaceOnlyQuery_meansAllNotes_notNoResults() = runTest(testDispatcher) {
        val fake = FakeNotesRepository(
            listOf(note(1, "Alpha", "alpha text"), note(2, "Beta", "beta text")),
        )
        val viewModel = NotesViewModel(fake)
        advanceUntilIdle()

        viewModel.setSearchQuery("   ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.notes.size)
        assertFalse(state.hasSearchNoResults)
        assertFalse(state.isEmpty)
    }

    @Test
    fun searchThatMatchesNothing_surfacesNoResultsFlag() = runTest(testDispatcher) {
        val viewModel = NotesViewModel(FakeNotesRepository(listOf(note(1, "Alpha", "alpha text"))))
        advanceUntilIdle()

        viewModel.setSearchQuery("zebra")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.notes.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty) // not the "no notes at all" state
        assertTrue(state.hasSearchNoResults)
    }

    @Test
    fun debounce_burstTyping_issuesOneRepositoryQuery_perWinningInput() = runTest(testDispatcher) {
        val fake = FakeNotesRepository(listOf(note(1, "Biology", "cell structure")))
        val viewModel = NotesViewModel(fake)

        // Let the initial blank-query load settle (its query is recorded).
        advanceUntilIdle()
        assertEquals(listOf(""), fake.searchInputsRecord)

        // Burst inside the 300 ms window: only the last query should survive.
        viewModel.setSearchQuery("b")
        advanceTimeBy(100)
        viewModel.setSearchQuery("bi")
        advanceTimeBy(100)
        viewModel.setSearchQuery("bio")
        advanceTimeBy(100)
        repeat(3) { viewModel.setSearchQuery("bio") } // equal repeats: conflated, no timer resets
        advanceUntilIdle()

        assertEquals(listOf("", "bio"), fake.searchInputsRecord) // exactly ONE burst query
        assertEquals("bio", viewModel.uiState.value.searchQuery)
        assertEquals(1, viewModel.uiState.value.notes.size)
    }

    // --- Writes ----------------------------------------------------------------

    @Test
    fun createNote_savesVerbatim_returnsId_andRefreshesThroughFlow() = runTest(testDispatcher) {
        val fake = FakeNotesRepository(listOf(note(1, "Alpha", "alpha text")))
        val viewModel = NotesViewModel(fake)
        advanceUntilIdle()

        var reportedId = -1L
        viewModel.createNote("  Draft   ", " body text ", onCreated = { reportedId = it })
        advanceUntilIdle()

        // The generated id is exposed to the caller ...
        assertEquals(42L, reportedId)
        // ... the text reached the repository verbatim (no silent trims) ...
        assertEquals("  Draft   " to " body text ", fake.createRequestsRecord.single())
        // ... and the observable Flow refreshed the state (like Room invalidation).
        val state = viewModel.uiState.value
        assertEquals(2, state.notes.size)
        assertTrue(state.notes.any { it.id == 42L && it.title == "  Draft   " })
        assertNull(state.errorResId)
    }

    @Test
    fun updateNote_forwardsTheModel_andRefreshesThroughFlow() = runTest(testDispatcher) {
        val fake = FakeNotesRepository(listOf(note(1, "Alpha", "alpha text")))
        val viewModel = NotesViewModel(fake)
        advanceUntilIdle()

        viewModel.updateNote(Note(1, "Alpha final", "edited body", createdAt = 1_001L, updatedAt = 9_999L))
        advanceUntilIdle()

        val forwarded = fake.updateRequestsRecord.single()
        assertEquals(1L, forwarded.id)
        assertEquals(1_001L, forwarded.createdAt) // ViewModel does not rewrite history
        val state = viewModel.uiState.value
        assertEquals("edited body", state.notes.single().content)
        assertNull(state.errorResId)
    }

    @Test
    fun deleteNote_removesById_andRefreshesThroughFlow() = runTest(testDispatcher) {
        val fake = FakeNotesRepository(
            listOf(note(1, "Alpha", "alpha text"), note(2, "Beta", "beta text")),
        )
        val viewModel = NotesViewModel(fake)
        advanceUntilIdle()

        viewModel.deleteNote(1L)
        advanceUntilIdle()

        assertEquals(1L, fake.deleteRequestsRecord.single())
        val state = viewModel.uiState.value
        assertEquals(listOf("Beta"), state.notes.map { it.title })
        assertNull(state.errorResId)
    }

    // --- Error handling ----------------------------------------------------------

    @Test
    fun observationFailure_showsLoadError_thenSelfHealsOnRetry() = runTest(testDispatcher) {
        val fake = FakeNotesRepository(listOf(note(1, "Alpha", "alpha text")))
        fake.failNextObservation = IllegalStateException("storage hiccup")
        val viewModel = NotesViewModel(fake)

        // Past the debounce (first search fires) but not yet past the retry delay.
        advanceTimeBy(301)
        val failing = viewModel.uiState.value
        assertFalse(failing.isLoading)
        assertEquals(R.string.notes_error_load_failed, failing.errorResId)

        advanceUntilIdle() // retry (3 s later, virtual) now succeeds
        val recovered = viewModel.uiState.value
        assertNull(recovered.errorResId)
        assertEquals(1, recovered.notes.size)
        assertEquals("Alpha", recovered.notes[0].title)
    }

    @Test
    fun failedWrite_showsSaveError_keepsListWithoutCrash_thenClearsOnNextEmission() =
        runTest(testDispatcher) {
            val fake = FakeNotesRepository(listOf(note(1, "Alpha", "alpha text")))
            fake.failNextWrite = IllegalStateException("disk full")
            val viewModel = NotesViewModel(fake)
            advanceUntilIdle()

            viewModel.createNote("New", "content")
            advanceUntilIdle()

            val failing = viewModel.uiState.value
            assertEquals(R.string.notes_error_save_failed, failing.errorResId)
            assertEquals(1, failing.notes.size) // honest: last known list, no fake success

            // Next successful write refreshes the data: the observation flow clears the error.
            viewModel.createNote("New", "content")
            advanceUntilIdle()

            val recovered = viewModel.uiState.value
            assertNull(recovered.errorResId)
            assertEquals(2, recovered.notes.size)
        }
}
