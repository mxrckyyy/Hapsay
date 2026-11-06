package com.campushelper.app.core.di

import android.content.Context
import androidx.room.Room
import com.campushelper.app.core.database.AppDatabase
import com.campushelper.app.data.repository.ExpensesRepository
import com.campushelper.app.data.repository.ExpensesRepositoryImpl
import com.campushelper.app.data.repository.NotesRepository
import com.campushelper.app.data.repository.NotesRepositoryImpl

/**
 * Manual dependency injection for Campus Helper.
 *
 * The project intentionally avoids Hilt to stay beginner-friendly. This
 * object is the single place where shared singletons (the Room database, the
 * repositories built on it; later: Retrofit services) are created lazily and
 * reused.
 *
 * Usage pattern from feature ViewModels (via constructor parameters provided
 * by the screen's `viewModel { }` factory - never from inside a composable):
 *
 *     val repository = ServiceLocator.provideNotesRepository(context)
 */
object ServiceLocator {

    @Volatile
    private var appDatabase: AppDatabase? = null

    @Volatile
    private var notesRepository: NotesRepository? = null

    @Volatile
    private var expensesRepository: ExpensesRepository? = null

    /**
     * Returns the shared [AppDatabase], creating it on first use.
     *
     * Double-checked locking keeps it safe even if called from multiple
     * threads on startup.
     */
    fun provideAppDatabase(context: Context): AppDatabase {
        val existing = appDatabase
        if (existing != null) return existing
        return synchronized(this) {
            val again = appDatabase
            if (again != null) {
                again
            } else {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    // Stored in the app-private data directory. Schema upgrades
                    // run ONLY through the explicit migrations registered below -
                    // destructive fallbacks are never allowed (see AGENTS.md
                    // and ARCHITECTURE.md §7).
                    "campus-helper.db",
                )
                    .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
                    .build()
                    .also { appDatabase = it }
            }
        }
    }

    /**
     * Returns the shared [NotesRepository], backed by the shared database.
     * Same double-checked locking as the database provider above.
     */
    fun provideNotesRepository(context: Context): NotesRepository {
        val existing = notesRepository
        if (existing != null) return existing
        return synchronized(this) {
            val again = notesRepository
            if (again != null) {
                again
            } else {
                NotesRepositoryImpl(provideAppDatabase(context).noteDao())
                    .also { notesRepository = it }
            }
        }
    }

    /**
     * Returns the shared [ExpensesRepository], backed by the shared database.
     * Same double-checked locking as the notes repository provider above.
     */
    fun provideExpensesRepository(context: Context): ExpensesRepository {
        val existing = expensesRepository
        if (existing != null) return existing
        return synchronized(this) {
            val again = expensesRepository
            if (again != null) {
                again
            } else {
                ExpensesRepositoryImpl(provideAppDatabase(context).expenseDao())
                    .also { expensesRepository = it }
            }
        }
    }
}
