package com.campushelper.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.campushelper.app.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for notes (schema version 2).
 *
 * The repository (data/repository/NotesRepositoryImpl) is the ONLY class that
 * should call this: ViewModels and composables never touch DAOs directly.
 *
 * Query conventions:
 *  - observation queries return [Flow] so collectors re-emit on every change;
 *  - write operations are suspend (Room runs them on its IO executor);
 *  - every query takes parameters - user input is never concatenated into SQL.
 */
@Dao
interface NoteDao {

    /**
     * All notes, newest-updated first. The `id DESC` tie-break keeps the
     * ordering deterministic when two notes share the same `updated_at`.
     */
    @Query("SELECT * FROM notes ORDER BY updated_at DESC, id DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    /**
     * One note by row id, as a live stream - emits null once the note is
     * deleted, so an editor can react instead of editing a ghost row.
     */
    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<NoteEntity?>

    /**
     * Notes whose title OR content contains [query] (SQLite LIKE, ASCII
     * case-insensitive). An empty query ('%%') intentionally matches every
     * note - the repository trims input before calling this.
     */
    @Query(
        "SELECT * FROM notes " +
            "WHERE title LIKE '%' || :query || '%' " +
            "OR content LIKE '%' || :query || '%' " +
            "ORDER BY updated_at DESC, id DESC",
    )
    fun search(query: String): Flow<List<NoteEntity>>

    /** Insert a new note and return its generated row id. */
    @Insert
    suspend fun insert(note: NoteEntity): Long

    /** Update an existing note (matched by its primary key). */
    @Update
    suspend fun update(note: NoteEntity)

    /** Delete a note by its row id. No-op when the id is unknown. */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
