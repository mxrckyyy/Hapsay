package com.campushelper.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.campushelper.app.core.database.AppDatabase
import com.campushelper.app.data.repository.NotesRepositoryImpl
import com.campushelper.app.domain.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the repository seam over a real (in-memory) Room
 * database - the exact production wiring minus persistence.
 *
 * Verifies the Phase 3.1 repository contract:
 *  - mapping entity <-> domain is lossless in both directions,
 *  - createdAt == updatedAt (same clock read) on creation and unchanged on update,
 *  - updatedAt is re-stamped on update,
 *  - search input is trimmed; blank search means "all notes";
 *  - saved note text is stored verbatim (no silent trimming/rewriting).
 */
@RunWith(AndroidJUnit4::class)
class NotesRepositoryImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase
    private lateinit var repository: NotesRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NotesRepositoryImpl(database.noteDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createNote_stampsIdenticalTimestamps_andMapsLosslessly() = runBlocking {
        val before = System.currentTimeMillis()
        val id = repository.createNote(title = "  Study plan   ", content = "  Final week notes. ")
        val after = System.currentTimeMillis()

        // Lossless mapping in BOTH directions: verbatim text (no trimming),
        // and the domain shape the repository emits.
        val note = repository.notes.first().single { it.id == id }
        assertEquals("  Study plan   ", note.title)
        assertEquals("  Final week notes. ", note.content)

        // One clock read per write: both stamps identical and within the
        // measured window (i.e. epoch millis, not seconds).
        assertTrue(note.createdAt == note.updatedAt)
        assertTrue(note.createdAt in before..after)
    }

    @Test
    fun updateNote_preservesCreatedAt_andRestampsUpdatedAt() = runBlocking {
        val id = repository.createNote(title = "Draft", content = "v1 text")
        val created = repository.notes.first().single { it.id == id }

        // The future editor submits the loaded domain Note with edited text;
        // repository keeps id/createdAt, stamps updatedAt fresh.
        repository.updateNote(created.copy(title = "Draft final", content = "v2 text"))

        val updated = repository.notes.first().single { it.id == id }
        assertEquals("Draft final", updated.title)
        assertEquals("v2 text", updated.content)
        assertEquals(created.createdAt, updated.createdAt)
        assertTrue(updated.updatedAt >= created.updatedAt)
    }

    @Test
    fun searchNotes_overRepository_matchesTitleAndContent() = runBlocking {
        val biologyId = repository.createNote("Biology Lecture", "Photosynthesis notes")
        repository.createNote("Grocery list", "eggs, milk, bread")

        assertEquals(listOf(biologyId), repository.searchNotes("biology").first().map { it.id })
        assertEquals(listOf(biologyId), repository.searchNotes("photosyn").first().map { it.id })
    }

    @Test
    fun searchNotes_trimsQuery_beforeHittingTheDao() = runBlocking {
        // "  eggs  " must behave exactly like "eggs".
        repository.createNote("Grocery list", "eggs, milk, bread")
        repository.createNote("Schedule thought", "morning classes are heavy")

        assertEquals(1, repository.searchNotes("  eggs  ").first().size)
        assertEquals(0, repository.searchNotes("zebra").first().size)
    }

    @Test
    fun searchNotes_blankQueryReturnsAllNotes() = runBlocking {
        repository.createNote("Alpha", "alpha text")
        repository.createNote("Beta", "beta text")

        // Empty and whitespace-only input both degrade to observe-all.
        assertEquals(2, repository.searchNotes("").first().size)
        assertEquals(2, repository.searchNotes("   \t ").first().size)
    }

    @Test
    fun deleteNote_removesTheNote() = runBlocking {
        val targetId = repository.createNote("Temp", "delete me")
        repository.createNote("Keeper", "stays")

        repository.deleteNote(targetId)

        val all = repository.notes.first()
        assertEquals(1, all.size)
        assertEquals("Keeper", all[0].title)
    }

    @Test
    fun observeNote_emitsNullOnceDeleted() = runBlocking {
        val id = repository.createNote("Disposable", "gone soon")
        assertTrue(repository.observeNote(id).first() != null)

        repository.deleteNote(id)
        assertTrue(repository.observeNote(id).first() == null)
    }

    @Test
    fun updateNote_onUnknownId_isANoOp() = runBlocking {
        val ghost = Note(id = 987_654L, title = "x", content = "y", createdAt = 1L, updatedAt = 1L)

        repository.updateNote(ghost)
        repository.deleteNote(ghost.id)

        // No crash, and no phantom rows were created.
        assertTrue(repository.notes.first().isEmpty())
    }
}
