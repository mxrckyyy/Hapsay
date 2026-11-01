package com.campushelper.app.data.repository

import com.campushelper.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Data boundary of the Notes feature.
 *
 * Presentations (the future NotesViewModel) depend on this interface only -
 * never on [com.campushelper.app.data.dao.NoteDao] or the entity classes
 * (dependency rules in ARCHITECTURE.md §4). Implementations are registered
 * in core/di/ServiceLocator.
 */
interface NotesRepository {

    /** All notes, newest-updated first, as a live stream. */
    val notes: Flow<List<Note>>

    /** One note by id, as a live stream; emits null once it is deleted. */
    fun observeNote(id: Long): Flow<Note?>

    /**
     * Notes matching [query] in title or content. Input is trimmed here so an
     * empty/whitespace-only query consistently means "all notes".
     */
    fun searchNotes(query: String): Flow<List<Note>>

    /**
     * Creates a note with the given verbatim title/content; the single write
     * stamps createdAt = updatedAt = now. Returns the inserted row id.
     */
    suspend fun createNote(title: String, content: String): Long

    /**
     * Updates a note: preserves id and createdAt from the submitted model,
     * stores title/content verbatim, re-stamps updatedAt = now.
     */
    suspend fun updateNote(note: Note)

    /** Deletes one note. No-op when the id is unknown. */
    suspend fun deleteNote(id: Long)
}
