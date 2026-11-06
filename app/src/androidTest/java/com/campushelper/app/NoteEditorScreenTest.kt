package com.campushelper.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.campushelper.app.data.repository.NotesRepository
import com.campushelper.app.domain.model.Note
import com.campushelper.app.presentation.notes.NoteEditorScreen
import com.campushelper.app.presentation.notes.NoteEditorViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteEditorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeNotesRepository(val initialNote: Note? = null) : NotesRepository {
        val created = mutableListOf<Pair<String, String>>()
        val updated = mutableListOf<Note>()
        val deleted = mutableListOf<Long>()
        var failNextDelete: Exception? = null

        override val notes: MutableStateFlow<List<Note>> = MutableStateFlow(if (initialNote != null) listOf(initialNote) else emptyList())
        override fun observeNote(id: Long): Flow<Note?> = kotlinx.coroutines.flow.flow { emit(initialNote) }
        override fun searchNotes(query: String): Flow<List<Note>> = notes
        override suspend fun createNote(title: String, content: String): Long {
            created += title to content
            return 7L
        }
        override suspend fun updateNote(note: Note) { updated += note }
        override suspend fun deleteNote(id: Long) {
            failNextDelete?.let { val failure = it; failNextDelete = null; throw failure }
            deleted += id
        }
    }

    @Test
    fun createMode_rendersTitleAndContentFields() {
        composeTestRule.setContent {
            NoteEditorScreen(noteId = null, onBack = {}, viewModel = NoteEditorViewModel(FakeNotesRepository(), null))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("New Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Note title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start writing your note...").assertIsDisplayed()
    }

    @Test
    fun saveButton_disabledWhenBlank_andEnabledAfterTyping() {
        composeTestRule.setContent {
            NoteEditorScreen(noteId = null, onBack = {}, viewModel = NoteEditorViewModel(FakeNotesRepository(), null))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Save note").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Note title").performTextInput("Hello")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Save note").assertIsEnabled()
    }

    @Test
    fun saveButton_invokesOnBack_onSuccess() {
        val fake = FakeNotesRepository()
        var backCalls = 0
        composeTestRule.setContent {
            NoteEditorScreen(noteId = null, onBack = { backCalls++ }, viewModel = NoteEditorViewModel(fake, null))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Note title").performTextInput("Hello")
        composeTestRule.onNodeWithText("Start writing your note...").performTextInput("World")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Save note").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            org.junit.Assert.assertEquals(1, backCalls)
            org.junit.Assert.assertEquals("Hello" to "World", fake.created.single())
        }
    }

    @Test
    fun unsavedChanges_backShowsDialog() {
        composeTestRule.setContent {
            NoteEditorScreen(noteId = null, onBack = {}, viewModel = NoteEditorViewModel(FakeNotesRepository(), null))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Note title").performTextInput("Draft")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Discard changes?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keep Editing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Discard Changes").assertIsDisplayed()
    }

    @Test
    fun discardChanges_dialogDiscard_callsOnBack() {
        var backCalls = 0
        composeTestRule.setContent {
            NoteEditorScreen(noteId = null, onBack = { backCalls++ }, viewModel = NoteEditorViewModel(FakeNotesRepository(), null))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Note title").performTextInput("Draft")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Discard Changes").performClick()
        composeTestRule.runOnIdle {
            org.junit.Assert.assertEquals(1, backCalls)
        }
    }

    @Test
    fun createMode_doesNotShowDeleteAction() {
        composeTestRule.setContent {
            NoteEditorScreen(noteId = null, onBack = {}, viewModel = NoteEditorViewModel(FakeNotesRepository(), null))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Delete note").assertDoesNotExist()
    }

    @Test
    fun editMode_showsDeleteAction() {
        val existing = Note(7, "Saved", "body", createdAt = 1_000L, updatedAt = 2_000L)
        composeTestRule.setContent {
            NoteEditorScreen(noteId = 7L, onBack = {}, viewModel = NoteEditorViewModel(FakeNotesRepository(existing), 7L))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Delete note").assertIsDisplayed()
    }

    @Test
    fun deleteTap_showsConfirmationDialog() {
        val existing = Note(7, "Saved", "body", createdAt = 1_000L, updatedAt = 2_000L)
        composeTestRule.setContent {
            NoteEditorScreen(noteId = 7L, onBack = {}, viewModel = NoteEditorViewModel(FakeNotesRepository(existing), 7L))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Delete note").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete note?").assertIsDisplayed()
        composeTestRule.onNodeWithText("This note will be permanently deleted. This action cannot be undone.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun deleteDialog_cancel_closesDialogWithoutDeleting() {
        val existing = Note(7, "Saved", "body", createdAt = 1_000L, updatedAt = 2_000L)
        val fake = FakeNotesRepository(existing)
        composeTestRule.setContent {
            NoteEditorScreen(noteId = 7L, onBack = {}, viewModel = NoteEditorViewModel(fake, 7L))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Delete note").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete note?").assertDoesNotExist()
        org.junit.Assert.assertTrue(fake.deleted.isEmpty())
    }

    @Test
    fun deleteDialog_confirm_invokesDeleteAndNavigatesBack() {
        val existing = Note(7, "Saved", "body", createdAt = 1_000L, updatedAt = 2_000L)
        val fake = FakeNotesRepository(existing)
        var backCalls = 0
        composeTestRule.setContent {
            NoteEditorScreen(noteId = 7L, onBack = { backCalls++ }, viewModel = NoteEditorViewModel(fake, 7L))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Delete note").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            org.junit.Assert.assertEquals(listOf(7L), fake.deleted)
            org.junit.Assert.assertEquals(1, backCalls)
        }
    }

    @Test
    fun deleteFailure_showsErrorFeedback() {
        val existing = Note(7, "Saved", "body", createdAt = 1_000L, updatedAt = 2_000L)
        val fake = FakeNotesRepository(existing)
        fake.failNextDelete = IllegalStateException("disk full")
        composeTestRule.setContent {
            NoteEditorScreen(noteId = 7L, onBack = {}, viewModel = NoteEditorViewModel(fake, 7L))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Delete note").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("This note couldn't be deleted. Please try again.").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("This note couldn't be deleted. Please try again.").assertIsDisplayed()
    }
}
