package com.campushelper.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.campushelper.app.data.dao.NoteDao
import com.campushelper.app.data.entity.NoteEntity

/**
 * Central Room database of Campus Helper.
 *
 * The application's persistent entities will be:
 *  - Note      (Notes feature)
 *  - Expense   (Expenses feature)
 *  - Event     (Calendar feature: quizzes, deadlines, academic activities)
 *  - Course    (Class Schedule feature)
 *
 * During project preparation the database intentionally has no entities yet -
 * it exists to prove the Room + KSP toolchain compiles and is wired
 * correctly. In the database phase, entity classes get added to [entities],
 * DAOs are attached with `abstract fun noteDao(): NoteDao`, and `version`
 * is bumped for every schema change.
 */
@Database(
    entities = [NoteEntity::class],
    version = 1,
    // Schema export will be enabled (with a room.schemaLocation KSP argument)
    // once the data model is finalized, so migrations can be tested.
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    /** Seed DAO wired so the generation pipeline is verified end-to-end. */
    abstract fun noteDao(): NoteDao
}
