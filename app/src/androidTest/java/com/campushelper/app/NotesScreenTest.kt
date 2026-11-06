package com.campushelper.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.campushelper.app.data.repository.NotesRepository
import com.campushelper.app.domain.model.Note
import com.campushelper.app.presentation.notes.NotesScreen
import com.campushelper.app.presentation.notes.NotesViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose-level smoke tests for the Notes screen (Phase 3.3). They exercise
 * the real screen against a fake repository (no Room, no network) so loading,
 * empty, populated, search, no-results, error and FAB-snackbar states can all
 * be driven deterministically.
 */
@RunWith(AndroidJUnit4::class)
class NotesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeNotesRepository(initialNotes: List<Note> = emptyList()) : NotesRepository {
        private val notesState = MutableStateFlow(initialNotes)
        var failNextObservation: Exception? = null

        override val notes: MutableStateFlow<List<Note>> = notesState
        override fun observeNote(id: Long) = notes.map { list -> list.firstOrNull { it.id == id } }
        override fun searchNotes(query: String): Flow<List<Note>> = flow {
            failNextObservation?.let { val failure = it; failNextObservation = null; throw failure }
            val needle = query.trim()
            emitAll(
                notesState.map { all ->
                    if (needle.isBlank()) all
                    else all.filter {
                        it.title.contains(needle, ignoreCase = true) ||
                            it.content.contains(needle, ignoreCase = true)
                    }
                },
            )
        }
        override suspend fun createNote(title: String, content: String): Long = 0L
        override suspend fun updateNote(note: Note) = Unit
        override suspend fun deleteNote(id: Long) = Unit
    }

    private fun note(id: Long, title: String, content: String) =
        Note(id = id, title = title, content = content, createdAt = 1_000L + id, updatedAt = 1_000L + id)

    @Test
    fun emptyDatabase_showsEmptyState() {
        composeTestRule.setContent { NotesScreen(viewModel = NotesViewModel(FakeNotesRepository())) }
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("No notes yet").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("No notes yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create your first note to keep important information in one place.").assertIsDisplayed()
    }

    @Test
    fun populatedDatabase_showsNotes() {
        val fake = FakeNotesRepository(listOf(note(1, "Biology", "cell structure"), note(2, "Grocery", "eggs, milk")))
        composeTestRule.setContent { NotesScreen(viewModel = NotesViewModel(fake)) }
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Biology").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Biology").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grocery").assertIsDisplayed()
    }

    @Test
    fun search_filtersNotes_andNoResultsState() {
        val fake = FakeNotesRepository(listOf(note(1, "Biology", "cell structure"), note(2, "Grocery", "eggs, milk")))
        composeTestRule.setContent { NotesScreen(viewModel = NotesViewModel(fake)) }
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Biology").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Search").performTextInput("bio")
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Biology").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText("Grocery").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithText("Biology").assertIsDisplayed()

        composeTestRule.onNodeWithText("Search").performTextInput("zzz")
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("No matching notes").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("No matching notes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try a different title or keyword.").assertIsDisplayed()
    }

    @Test
    fun fabTap_invokesAddNoteCallback() {
        var addNoteCalls = 0
        composeTestRule.setContent {
            NotesScreen(viewModel = NotesViewModel(FakeNotesRepository()), onAddNote = { addNoteCalls++ })
        }
        composeTestRule.onNodeWithContentDescription("Add note").performClick()
        composeTestRule.runOnIdle {
            org.junit.Assert.assertEquals(1, addNoteCalls)
        }
    }

    @Test
    fun observationFailure_showsErrorState() {
        val fake = FakeNotesRepository(listOf(note(1, "Alpha", "alpha text")))
        fake.failNextObservation = IllegalStateException("storage hiccup")
        composeTestRule.setContent { NotesScreen(viewModel = NotesViewModel(fake)) }
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText("Couldn't load notes").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Couldn't load notes").assertIsDisplayed()
    }
}
