package com.campushelper.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.campushelper.app.data.dao.NoteDao
import com.campushelper.app.data.entity.NoteEntity

/**
 * Central Room database of Campus Helper (schema version 2).
 *
 * Persistent entities:
 *  - Note      (Notes feature - IMPLEMENTED, Phase 3.1)
 *  - Expense   (Expenses feature - planned)
 *  - Event     (Calendar feature: quizzes, deadlines, activities - planned)
 *  - Course    (Class Schedule feature - planned)
 *
 * Schema changes follow the expansion protocol in ARCHITECTURE.md §7: bump
 * [version], add an explicit migration, and never use destructive fallback.
 * Version 2 (`MIGRATION_1_2`) finalized the Notes table: `body` was renamed
 * to `content` and `created_at` was added.
 */
@Database(
    entities = [NoteEntity::class],
    version = 2,
    // The Notes model is final: each schema version is exported to
    // app/schemas/ (ksp room.schemaLocation in app/build.gradle.kts) so
    // migrations stay reviewable and testable going forward.
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    /** Notes query surface. Only NotesRepositoryImpl may call this. */
    abstract fun noteDao(): NoteDao

    companion object {

        /**
         * Schema v1 -> v2: the notes table gained `content` (renamed from the
         * seed's `body`) and `created_at`.
         *
         * SQLite's RENAME COLUMN requires 3.25 (≈Android 9); the project
         * supports API 26, so the portable recreate-table pattern is used:
         * build the final table under a temp name, copy every row across,
         * then swap tables. Non-destructive by construction.
         *
         * The temp-table DDL is byte-for-byte the v2 createSql Room generated
         * (app/schemas/.../2.json) with the table renamed to `notes_new`, so
         * the migration result always matches what Room expects to find.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes_new` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`content` TEXT NOT NULL, " +
                        "`created_at` INTEGER NOT NULL, " +
                        "`updated_at` INTEGER NOT NULL)",
                )
                // Copy legacy rows: body -> content; created_at is unknown in
                // v1 so it is backfilled with the row's own updated_at (the
                // best derivable value - no timestamps are invented).
                db.execSQL(
                    "INSERT INTO `notes_new` (id, title, content, created_at, updated_at) " +
                        "SELECT id, title, body, updated_at, updated_at FROM notes",
                )
                db.execSQL("DROP TABLE notes")
                db.execSQL("ALTER TABLE `notes_new` RENAME TO notes")
            }
        }
    }
}
