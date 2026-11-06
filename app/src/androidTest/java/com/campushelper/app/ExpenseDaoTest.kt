package com.campushelper.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.campushelper.app.core.database.AppDatabase
import com.campushelper.app.data.dao.ExpenseDao
import com.campushelper.app.data.entity.ExpenseEntity
import com.campushelper.app.domain.model.ExpenseCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase
    private lateinit var dao: ExpenseDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.expenseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun expense(
        title: String,
        amountMinor: Long,
        category: String = ExpenseCategory.FOOD.id,
        date: Long,
        note: String = "",
    ) = ExpenseEntity(
        title = title,
        amountMinor = amountMinor,
        categoryId = category,
        expenseDateEpochDay = date,
        note = note,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    @Test
    fun insert_returnsGeneratedId() = runBlocking {
        val id = dao.insert(expense("Lunch", 15050, date = 100))
        assertTrue(id > 0)
    }

    @Test
    fun observeAll_ordersByDateDescThenIdDesc() = runBlocking {
        dao.insert(expense("A", 1000, date = 10)) // id 1
        dao.insert(expense("B", 2000, date = 20)) // id 2
        dao.insert(expense("C", 3000, date = 30)) // id 3
        dao.insert(expense("D", 4000, date = 30)) // id 4 (same date, higher id first)
        val titles = dao.observeAll().first().map { it.title }
        assertEquals(listOf("D", "C", "B", "A"), titles)
    }

    @Test
    fun observeById_returnsInsertedExpense() = runBlocking {
        val id = dao.insert(expense("Lunch", 15050, date = 100))
        val loaded = dao.observeById(id).first()
        assertEquals("Lunch", loaded?.title)
        assertEquals(15050L, loaded?.amountMinor)
    }

    @Test
    fun update_changesStoredFields() = runBlocking {
        val id = dao.insert(expense("Lunch", 15050, date = 100))
        dao.update(
            ExpenseEntity(
                id = id,
                title = "Dinner",
                amountMinor = 20000,
                categoryId = ExpenseCategory.FOOD.id,
                expenseDateEpochDay = 101,
                note = "updated",
                createdAt = 1_000L,
                updatedAt = 9_999L,
            ),
        )
        val updated = dao.observeById(id).first()
        assertEquals("Dinner", updated?.title)
        assertEquals(20000L, updated?.amountMinor)
        assertEquals(101L, updated?.expenseDateEpochDay)
        assertEquals(9_999L, updated?.updatedAt)
    }

    @Test
    fun deleteById_removesOnlyTarget() = runBlocking {
        val target = dao.insert(expense("Delete me", 1000, date = 10))
        dao.insert(expense("Keep me", 2000, date = 20))
        dao.deleteById(target)
        val remaining = dao.observeAll().first()
        assertEquals(1, remaining.size)
        assertEquals("Keep me", remaining[0].title)
    }

    @Test
    fun observeByCategory_filtersCorrectly() = runBlocking {
        dao.insert(expense("Lunch", 1000, category = ExpenseCategory.FOOD.id, date = 10))
        dao.insert(expense("Bus", 2000, category = ExpenseCategory.TRANSPORT.id, date = 20))
        dao.insert(expense("Snack", 500, category = ExpenseCategory.FOOD.id, date = 30))
        val food = dao.observeByCategory(ExpenseCategory.FOOD.id).first()
        assertEquals(2, food.size)
        assertTrue(food.all { it.categoryId == ExpenseCategory.FOOD.id })
    }

    @Test
    fun observeByDateRange_isInclusive() = runBlocking {
        dao.insert(expense("A", 1000, date = 5))
        dao.insert(expense("B", 2000, date = 10))
        dao.insert(expense("C", 3000, date = 15))
        val range = dao.observeByDateRange(10, 15).first()
        assertEquals(2, range.size)
        assertEquals(setOf("B", "C"), range.map { it.title }.toSet())
    }

    @Test
    fun observeFiltered_combinesCategoryAndDateRange() = runBlocking {
        dao.insert(expense("Lunch", 1000, category = ExpenseCategory.FOOD.id, date = 10))
        dao.insert(expense("Bus", 2000, category = ExpenseCategory.TRANSPORT.id, date = 10))
        dao.insert(expense("Snack", 500, category = ExpenseCategory.FOOD.id, date = 20))
        val filtered = dao.observeFiltered(
            categoryId = ExpenseCategory.FOOD.id,
            startEpochDay = 5,
            endEpochDay = 15,
        ).first()
        assertEquals(1, filtered.size)
        assertEquals("Lunch", filtered[0].title)
    }

    @Test
    fun observeFiltered_withNulls_returnsAll() = runBlocking {
        dao.insert(expense("A", 1000, date = 10))
        dao.insert(expense("B", 2000, date = 20))
        val all = dao.observeFiltered(null, null, null).first()
        assertEquals(2, all.size)
    }

    @Test
    fun emptyDatabase_returnsEmptyList() = runBlocking {
        assertTrue(dao.observeAll().first().isEmpty())
    }
}
