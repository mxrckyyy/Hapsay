package com.campushelper.app.core.di

import android.content.Context
import androidx.room.Room
import com.campushelper.app.core.database.AppDatabase

/**
 * Manual dependency injection for Campus Helper.
 *
 * The project intentionally avoids Hilt to stay beginner-friendly. This
 * object is the single place where shared singletons (currently: the Room
 * database; later: Retrofit services) are created lazily and reused.
 *
 * Usage pattern from repositories / ViewModels (via constructor parameters
 * provided by the screens `viewModel(...)` factory in the feature phase):
 *
 *     val noteDao = ServiceLocator.provideDatabase(context).noteDao()
 */
object ServiceLocator {

    @Volatile
    private var appDatabase: AppDatabase? = null

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
                    // Stored in the app-private data directory; add a
                    // migration builder here when the schema evolves.
                    "campus-helper.db",
                )
                    .build()
                    .also { appDatabase = it }
            }
        }
    }
}
