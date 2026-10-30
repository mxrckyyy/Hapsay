package com.campushelper.app.domain.model

/**
 * Business model of one note, as the presentation layer sees it.
 *
 * Pure Kotlin: no Android or Room types. The data layer owns the mapping
 * between [Note] and its storage representation (data/entity/NoteEntity),
 * so presentation code never imports data.* classes.
 *
 * Timestamps are epoch milliseconds (same format as storage) so the UI can
 * format createdAt/updatedAt directly.
 */
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)
