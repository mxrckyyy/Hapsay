package com.campushelper.app.data.repository

import com.campushelper.app.data.dao.NoteDao
import com.campushelper.app.data.entity.NoteEntity
import com.campushelper.app.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed implementation of [NotesRepository]; built by ServiceLocator.
 *
 * Responsibilities (only these):
 *  - call the NoteDao (nothing else does),
 *  - stamp every write with ONE clock read per operation,
 *  - map NoteEntity <-> Note so data.* types never leave this file.
 *
 * No extra threading: Room's Flows and suspend DAO calls already run on
 * Room's IO executors - a withContext here would be redundant.
 */
internal class NotesRepositoryImpl(
    private val noteDao: NoteDao,
) : NotesRepository {

    override val notes: Flow<List<Note>> =
        noteDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeNote(id: Long): Flow<Note?> =
        noteDao.observeById(id).map { entity -> entity?.toDomain() }

    override fun searchNotes(query: String): Flow<List<Note>> =
        // Trim once here so empty/whitespace input consistently degrades to
        // the DAO's '%%' pattern ("all notes"), identical to `notes`.
        noteDao.search(query = query.trim()).map { entities -> entities.map { it.toDomain() } }

    override suspend fun createNote(title: String, content: String): Long {
        // One clock read per write. Title/content are stored verbatim - the
        // repository never trims or rewrites the user's saved text.
        val timestamp = System.currentTimeMillis()
        return noteDao.insert(
            NoteEntity(
                title = title,
                content = content,
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    override suspend fun updateNote(note: Note) {
        // One clock read; id and createdAt are preserved from the submitted
        // domain model (they belong to the row's identity/history).
        noteDao.update(note.toEntity(stamp = System.currentTimeMillis()))
    }

    override suspend fun deleteNote(id: Long) = noteDao.deleteById(id)

    // --- Entity <-> domain mapping ------------------------------------------
    // Private to this file: the boundary contract is domain/model/Note only.

    private fun NoteEntity.toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun Note.toEntity(stamp: Long): NoteEntity = NoteEntity(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = stamp,
    )
}
