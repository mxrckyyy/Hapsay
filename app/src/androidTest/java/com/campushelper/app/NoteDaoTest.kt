package com.campushelper.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.campushelper.app.core.database.AppDatabase
import com.campushelper.app.data.dao.NoteDao
import com.campushelper.app.data.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Notes DAO tests over an in-memory Room database (not disk, no
 * migration involvement - the fresh database is created directly at schema
 * version 2).
 *
 * Covers the DAO contract: insert, observe-all, observe-single, search (by
 * title / content / blank query), deterministic ordering, update and delete.
 * Timestamp stamping semantics live one layer up in NotesRepositoryImplTest.
 */
@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase
    private lateinit var noteDao: NoteDao

    @Before
    fun setUp() {
        // In-memory database: never writes files to the emulator.
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        noteDao = database.noteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Fixture with explicit epoch-millis times so ordering never depends on wall-clock timing. */
    private fun note(title: String, content: String, updatedAt: Long): NoteEntity = NoteEntity(
        title = title,
        content = content,
        createdAt = updatedAt,
        updatedAt = updatedAt,
    )

    @Test
    fun insert_returnsGeneratedIdAndIsObservedInList() = runBlocking {
        val id = noteDao.insert(note("Biology lecture", "Cell structure basics", updatedAt = 1_000L))

        assertTrue(id > 0)
        val all = noteDao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(id, all[0].id)
        assertEquals("Biology lecture", all[0].title)
        assertEquals("Cell structure basics", all[0].content)
    }

    @Test
    fun observeAll_ordersByUpdatedAtDescThenIdDesc() = runBlocking {
        // Insertion order assigns ids 1..4. Notes C and D tie on updated_at;
        // the deterministic tie-break must put D (higher id) before C.
        noteDao.insert(note("A", "a", updatedAt = 1_000L)) // id 1
        noteDao.insert(note("B", "b", updatedAt = 2_000L)) // id 2
        noteDao.insert(note("C", "c", updatedAt = 3_000L)) // id 3
        noteDao.insert(note("D", "d", updatedAt = 3_000L)) // id 4

        val titles = noteDao.observeAll().first().map { it.title }
        assertEquals(listOf("D", "C", "B", "A"), titles)
    }

    @Test
    fun observeById_emitsTheNote_thenNullAfterDeletion() = runBlocking {
        val id = noteDao.insert(note("History", "Machu Picchu visit plan", updatedAt = 500L))

        val observed = noteDao.observeById(id).first()
        assertEquals("History", observed?.title)
        assertEquals(500L, observed?.updatedAt)

        noteDao.deleteById(id)
        assertNull(noteDao.observeById(id).first())
    }

    @Test
    fun search_matchesByTitle_caseInsensitively() = runBlocking {
        noteDao.insert(note("Biology Lecture", "Photosynthesis notes", updatedAt = 10L))
        noteDao.insert(note("Grocery list", "eggs, milk, bread", updatedAt = 20L))

        // ASCII LIKE folding: uppercase needle still matches title.
        val hits = noteDao.search("BIOLOGY").first()
        assertEquals(1, hits.size)
        assertEquals("Biology Lecture", hits[0].title)
    }

    @Test
    fun search_matchesByContent() = runBlocking {
        noteDao.insert(note("Random title", "photosynthesis explanation", updatedAt = 10L))
        noteDao.insert(note("Other title", "completely different text", updatedAt = 20L))

        val hits = noteDao.search("photosyn").first()
        assertEquals(1, hits.size)
        assertEquals("Random title", hits[0].title)
    }

    @Test
    fun search_emptyQueryIntentionallyReturnsEverything() = runBlocking {
        // The repository trims input before this call: blank == '%%' == all.
        noteDao.insert(note("Alpha", "alpha content", updatedAt = 10L))
        noteDao.insert(note("Beta", "beta content", updatedAt = 20L))

        assertEquals(2, noteDao.search("").first().size)
        assertEquals(0, noteDao.search("zebra").first().size)
    }

    @Test
    fun update_changesStoredFields() = runBlocking {
        val id = noteDao.insert(note("Draft", "old text", updatedAt = 100L))

        noteDao.update(
            NoteEntity(
                id = id,
                title = "Draft - edited",
                content = "new text",
                createdAt = 100L,     // caller keeps createdAt (repository contract)
                updatedAt = 999L,     // caller supplies the new stamp
            ),
        )

        val updated = noteDao.observeById(id).first()
        assertEquals("Draft - edited", updated?.title)
        assertEquals("new text", updated?.content)
        assertEquals(999L, updated?.updatedAt)
    }

    @Test
    fun deleteById_removesOnlyTargetNote() = runBlocking {
        val targetId = noteDao.insert(note("Delete me", "bye", updatedAt = 10L))
        noteDao.insert(note("Keep me", "stay", updatedAt = 20L))

        noteDao.deleteById(targetId)

        val remaining = noteDao.observeAll().first()
        assertEquals(1, remaining.size)
        assertEquals("Keep me", remaining[0].title)
        assertFalse(remaining.any { it.id == targetId })
    }
}
