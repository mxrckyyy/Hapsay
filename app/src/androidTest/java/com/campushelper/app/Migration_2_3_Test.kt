package com.campushelper.app

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.campushelper.app.core.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration_2_3_Test {

    private val dbName = "migration-test-2-3.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate2To3_preservesNotesAndCreatesExpensesTable() {
        // 1. Create a v2 database and seed it with a Notes record.
        val v2Db = helper.createDatabase(dbName, 2)
        v2Db.execSQL(
            "INSERT INTO notes (id, title, content, created_at, updated_at) " +
                "VALUES (1, 'Keep me', 'body text', 111, 222)",
        )
        v2Db.close()

        // 2. Run the real v2 -> v3 migration and validate against the v3 schema.
        val migrated = helper.runMigrationsAndValidate(
            dbName,
            3,
            true,
            AppDatabase.MIGRATION_2_3,
        )

        // 3. Existing Notes record preserved with original values.
        migrated.query("SELECT title, content, created_at, updated_at FROM notes WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Keep me", c.getString(0))
            assertEquals("body text", c.getString(1))
            assertEquals(111L, c.getLong(2))
            assertEquals(222L, c.getLong(3))
        }

        // 4. The new expenses table exists and accepts/returns a row.
        migrated.execSQL(
            "INSERT INTO expenses (title, amount_minor, category, expense_date_epoch_day, note, created_at, updated_at) " +
                "VALUES ('Lunch', 15050, 'food', 100, 'with friends', 1, 2)",
        )
        migrated.query("SELECT title, amount_minor, category, expense_date_epoch_day, note FROM expenses").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Lunch", c.getString(0))
            assertEquals(15050L, c.getLong(1))
            assertEquals("food", c.getString(2))
            assertEquals(100L, c.getLong(3))
            assertEquals("with friends", c.getString(4))
        }

        // 5. Indexes exist.
        migrated.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='expenses'",
        ).use { c ->
            val names = mutableSetOf<String>()
            while (c.moveToNext()) names += c.getString(0)
            assertTrue(names.contains("index_expenses_expense_date_epoch_day"))
            assertTrue(names.contains("index_expenses_category"))
        }

        migrated.close()
    }
}
