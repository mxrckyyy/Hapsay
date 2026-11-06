package com.campushelper.app.presentation.notes

import com.campushelper.app.R
import com.campushelper.app.data.repository.NotesRepository
import com.campushelper.app.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
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

class NoteEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun note(id: Long, title: String, content: String) =
        Note(id = id, title = title, content = content, createdAt = 1_000L + id, updatedAt = 2_000L + id)

    private class FakeNotesRepository(initialNotes: List<Note> = emptyList()) : NotesRepository {
        private val notesState = MutableStateFlow(initialNotes)
        val createRequests = mutableListOf<Pair<String, String>>()
        val updateRequests = mutableListOf<Note>()
        val deleteRequests = mutableListOf<Long>()
        var failNextWrite: Exception? = null
        var failNextDelete: Exception? = null

        override val notes: MutableStateFlow<List<Note>> = notesState
        override fun observeNote(id: Long): Flow<Note?> = flow {
            emit(notesState.value.firstOrNull { it.id == id })
        }
        override fun searchNotes(query: String): Flow<List<Note>> = notesState
        override suspend fun createNote(title: String, content: String): Long {
            failNextWrite?.let { val failure = it; failNextWrite = null; throw failure }
            createRequests += title to content
            val id = 42L + createRequests.size
            notesState.value = notesState.value + Note(id, title, content, createdAt = 1L, updatedAt = 1L)
            return id
        }
        override suspend fun updateNote(note: Note) {
            failNextWrite?.let { val failure = it; failNextWrite = null; throw failure }
            updateRequests += note
            notesState.value = notesState.value.map { if (it.id == note.id) note.copy(updatedAt = 9_999L) else it }
        }
        override suspend fun deleteNote(id: Long) {
            failNextDelete?.let { val failure = it; failNextDelete = null; throw failure }
            deleteRequests += id
            notesState.value = notesState.value.filter { it.id != id }
        }
    }

    // --- Create mode ---------------------------------------------------------

    @Test
    fun createMode_initialState_isBlankAndNotLoading() = runTest(testDispatcher) {
        val viewModel = NoteEditorViewModel(FakeNotesRepository(), noteId = null)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("", state.title)
        assertEquals("", state.content)
        assertFalse(state.isLoading)
        assertFalse(state.isSaving)
        assertNull(state.errorResId)
        assertFalse(state.noteNotFound)
        assertFalse(state.hasUnsavedChanges)
        assertFalse(state.canSave)
    }

    @Test
    fun createMode_typing_setsUnsavedChanges() = runTest(testDispatcher) {
        val viewModel = NoteEditorViewModel(FakeNotesRepository(), noteId = null)
        advanceUntilIdle()
        viewModel.onTitleChange("Draft title")
        viewModel.onContentChange("Draft body")
        val state = viewModel.uiState.value
        assertEquals("Draft title", state.title)
        assertEquals("Draft body", state.content)
        assertTrue(state.hasUnsavedChanges)
        assertTrue(state.canSave)
    }

    @Test
    fun createMode_blankValidation_preventsSave() = runTest(testDispatcher) {
        val fake = FakeNotesRepository()
        val viewModel = NoteEditorViewModel(fake, noteId = null)
        advanceUntilIdle()
        viewModel.save(onSuccess = {})
        advanceUntilIdle()
        assertTrue(fake.createRequests.isEmpty())
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun createMode_successfulSave_callsRepository_andClearsUnsaved() = runTest(testDispatcher) {
        val fake = FakeNotesRepository()
        val viewModel = NoteEditorViewModel(fake, noteId = null)
        advanceUntilIdle()
        var successCalled = false
        viewModel.onTitleChange("  Draft   ")
        viewModel.onContentChange(" body text ")
        viewModel.save(onSuccess = { successCalled = true })
        advanceUntilIdle()
        assertTrue(successCalled)
        assertEquals("  Draft   " to " body text ", fake.createRequests.single())
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertFalse(state.hasUnsavedChanges)
        assertNull(state.errorResId)
    }

    @Test
    fun createMode_duplicateSave_isIgnored() = runTest(testDispatcher) {
        val fake = FakeNotesRepository()
        val viewModel = NoteEditorViewModel(fake, noteId = null)
        advanceUntilIdle()
        viewModel.onTitleChange("T")
        viewModel.save(onSuccess = {})
        viewModel.save(onSuccess = {}) // duplicate while saving
        advanceUntilIdle()
        assertEquals(1, fake.createRequests.size)
    }

    @Test
    fun createMode_saveFailure_preservesInputAndShowsError() = runTest(testDispatcher) {
        val fake = FakeNotesRepository()
        fake.failNextWrite = IllegalStateException("disk full")
        val viewModel = NoteEditorViewModel(fake, noteId = null)
        advanceUntilIdle()
        viewModel.onTitleChange("T")
        viewModel.onContentChange("C")
        viewModel.save(onSuccess = {})
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("T", state.title)
        assertEquals("C", state.content)
        assertFalse(state.isSaving)
        assertEquals(R.string.note_error_save_failed, state.errorResId)
        assertTrue(state.hasUnsavedChanges)
    }

    // --- Edit mode ----------------------------------------------------------

    @Test
    fun editMode_loadsExistingNoteIntoFields() = runTest(testDispatcher) {
        val existing = note(7, "Saved title", "Saved body")
        val viewModel = NoteEditorViewModel(FakeNotesRepository(listOf(existing)), noteId = 7L)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.noteNotFound)
        assertEquals("Saved title", state.title)
        assertEquals("Saved body", state.content)
        assertEquals("Saved title", state.initialTitle)
        assertEquals("Saved body", state.initialContent)
        assertEquals(existing.createdAt, state.createdAt)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun editMode_missingNote_setsNotFound() = runTest(testDispatcher) {
        val viewModel = NoteEditorViewModel(FakeNotesRepository(), noteId = 99L)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.noteNotFound)
        assertFalse(state.canSave)
    }

    @Test
    fun editMode_successfulUpdate_preservesIdAndCreatedAt() = runTest(testDispatcher) {
        val existing = note(7, "Old", "old body")
        val fake = FakeNotesRepository(listOf(existing))
        val viewModel = NoteEditorViewModel(fake, noteId = 7L)
        advanceUntilIdle()
        viewModel.onTitleChange("New")
        viewModel.onContentChange("new body")
        var successCalled = false
        viewModel.save(onSuccess = { successCalled = true })
        advanceUntilIdle()
        assertTrue(successCalled)
        val forwarded = fake.updateRequests.single()
        assertEquals(7L, forwarded.id)
        assertEquals(existing.createdAt, forwarded.createdAt)
        val state = viewModel.uiState.value
        assertFalse(state.hasUnsavedChanges)
        assertNull(state.errorResId)
    }

    @Test
    fun editMode_updateFailure_preservesInputAndShowsError() = runTest(testDispatcher) {
        val existing = note(7, "Old", "old body")
        val fake = FakeNotesRepository(listOf(existing))
        fake.failNextWrite = IllegalStateException("disk full")
        val viewModel = NoteEditorViewModel(fake, noteId = 7L)
        advanceUntilIdle()
        viewModel.onTitleChange("New")
        viewModel.save(onSuccess = {})
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("New", state.title)
        assertFalse(state.isSaving)
        assertEquals(R.string.note_error_save_failed, state.errorResId)
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun delete_unavailableInCreateMode() = runTest(testDispatcher) {
        val fake = FakeNotesRepository()
        val viewModel = NoteEditorViewModel(fake, noteId = null)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.canDelete)
        viewModel.deleteNote(onSuccess = {})
        advanceUntilIdle()
        assertTrue(fake.deleteRequests.isEmpty())
    }

    @Test
    fun delete_availableForValidExistingNote() = runTest(testDispatcher) {
        val existing = note(7, "Old", "old body")
        val viewModel = NoteEditorViewModel(FakeNotesRepository(listOf(existing)), noteId = 7L)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canDelete)
    }

    @Test
    fun delete_ignoredWhileLoading() = runTest(testDispatcher) {
        val existing = note(7, "Old", "old body")
        val fake = FakeNotesRepository(listOf(existing))
        val viewModel = NoteEditorViewModel(fake, noteId = 7L)
        // Do not advance: still loading.
        assertFalse(viewModel.uiState.value.canDelete)
        viewModel.deleteNote(onSuccess = {})
        advanceUntilIdle()
        assertTrue(fake.deleteRequests.isEmpty())
    }

    @Test
    fun delete_ignoredWhileSaving() = runTest(testDispatcher) {
        val existing = note(7, "Old", "old body")
        val fake = FakeNotesRepository(listOf(existing))
        val viewModel = NoteEditorViewModel(fake, noteId = 7L)
        advanceUntilIdle()
        viewModel.onTitleChange("New")
        viewModel.save(onSuccess = {})
        // isSaving true now (before advance): delete must be ignored.
        viewModel.deleteNote(onSuccess = {})
        advanceUntilIdle()
        assertTrue(fake.deleteRequests.isEmpty())
    }

    @Test
    fun delete_successful_callsRepositoryWithCorrectId_andNavigates() = runTest(testDispatcher) {
        val existing = note(7, "Old", "old body")
        val fake = FakeNotesRepository(listOf(existing))
        val viewModel = NoteEditorViewModel(fake, noteId = 7L)
        advanceUntilIdle()
        var successCalls = 0
        viewModel.deleteNote(onSuccess = { successCalls++ })
        advanceUntilIdle()
        assertEquals(listOf(7L), fake.deleteRequests)
        assertEquals(1, successCalls)
        assertFalse(viewModel.uiState.value.isDeleting)
        assertNull(viewModel.uiState.value.errorResId)
    }

    @Test
    fun delete_duplicateRequest_prevented() = runTest(testDispatcher) {
        val existing = note(7, "Old", "old body")
        val fake = FakeNotesRepository(listOf(existing))
        val viewModel = NoteEditorViewModel(fake, noteId = 7L)
        advanceUntilIdle()
        viewModel.deleteNote(onSuccess = {})
        viewModel.deleteNote(onSuccess = {}) // duplicate while deleting
        advanceUntilIdle()
        assertEquals(1, fake.deleteRequests.size)
    }

    @Test
    fun delete_failure_preservesStateAndShowsError() = runTest(testDispatcher) {
        val existing = note(7, "Old", "old body")
        val fake = FakeNotesRepository(listOf(existing))
        fake.failNextDelete = IllegalStateException("disk full")
        val viewModel = NoteEditorViewModel(fake, noteId = 7L)
        advanceUntilIdle()
        viewModel.onTitleChange("Unsaved edit")
        viewModel.deleteNote(onSuccess = {})
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("Unsaved edit", state.title)
        assertEquals("old body", state.content) // content unchanged by the edit above? title only changed
        assertFalse(state.isDeleting)
        assertEquals(R.string.note_error_delete_failed, state.errorResId)
        assertTrue(state.isEditing)
    }

    @Test
    fun delete_failure_doesNotNavigate() = runTest(testDispatcher) {
        val existing = note(7, "Old", "old body")
        val fake = FakeNotesRepository(listOf(existing))
        fake.failNextDelete = IllegalStateException("disk full")
        val viewModel = NoteEditorViewModel(fake, noteId = 7L)
        advanceUntilIdle()
        var successCalls = 0
        viewModel.deleteNote(onSuccess = { successCalls++ })
        advanceUntilIdle()
        assertEquals(0, successCalls)
    }
}

