package com.campushelper.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database row for one saved note (Notes feature, schema version 2).
 *
 * Storage contract for the "notes" table:
 *  - `id`: auto-generated row id - the stable key across restarts,
 *  - `title` / `content`: the user's text, stored verbatim (no trimming),
 *  - `created_at` / `updated_at`: epoch milliseconds, stamped by the
 *    repository at write time; `createdAt` never changes after insert.
 *
 * All "data.*" classes describe how data is STORED. Presentation code must
 * not depend on them - it uses domain/model/Note.kt instead.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    /** The note body. Column renamed from the seed's `body` (migration 1 -> 2). */
    @ColumnInfo(name = "content")
    val content: String,

    /** Epoch milliseconds; set once when the note is created. */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    /** Epoch milliseconds; re-stamped on every update. */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
