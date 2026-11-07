package com.campushelper.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.campushelper.app.core.database.AppDatabase
import com.campushelper.app.data.repository.ExpensesRepositoryImpl
import com.campushelper.app.domain.model.ExpenseCategory
import com.campushelper.app.domain.model.parseExpenseAmount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpensesRepositoryImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase
    private lateinit var repository: ExpensesRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExpensesRepositoryImpl(database.expenseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createExpense_stampsIdenticalTimestamps_andRoundTripsAmount() = runBlocking {
        val before = System.currentTimeMillis()
        val id = repository.createExpense(
            title = "  Lunch  ",
            amountMinor = parseExpenseAmount("150.50")!!,
            category = ExpenseCategory.FOOD,
            expenseDateEpochDay = 100L,
            note = "  with friends  ",
        )
        val after = System.currentTimeMillis()

        val expense = repository.expenses.first().single { it.id == id }
        assertEquals("  Lunch  ", expense.title) // verbatim
        assertEquals("  with friends  ", expense.note) // verbatim
        assertEquals(15050L, expense.amountMinor)
        assertEquals(ExpenseCategory.FOOD, expense.category)
        assertEquals(100L, expense.expenseDateEpochDay)
        assertTrue(expense.createdAt == expense.updatedAt)
        assertTrue(expense.createdAt in before..after)
    }

    @Test
    fun updateExpense_preservesCreatedAt_andRestampsUpdatedAt() = runBlocking {
        val id = repository.createExpense("Lunch", 15050, ExpenseCategory.FOOD, 100L, "")
        val created = repository.expenses.first().single { it.id == id }

        repository.updateExpense(created.copy(title = "Dinner", amountMinor = 20000))
        val updated = repository.expenses.first().single { it.id == id }
        assertEquals("Dinner", updated.title)
        assertEquals(20000L, updated.amountMinor)
        assertEquals(created.createdAt, updated.createdAt)
        assertTrue(updated.updatedAt >= created.updatedAt)
    }

    @Test
    fun deleteExpense_removesTheExpense() = runBlocking {
        val target = repository.createExpense("Temp", 1000, ExpenseCategory.OTHER, 10L, "")
        repository.createExpense("Keeper", 2000, ExpenseCategory.OTHER, 20L, "")
        repository.deleteExpense(target)
        val all = repository.expenses.first()
        assertEquals(1, all.size)
        assertEquals("Keeper", all[0].title)
    }

    @Test
    fun observeExpense_emitsNullOnceDeleted() = runBlocking {
        val id = repository.createExpense("Disposable", 1000, ExpenseCategory.OTHER, 10L, "")
        assertTrue(repository.observeExpense(id).first() != null)
        repository.deleteExpense(id)
        assertTrue(repository.observeExpense(id).first() == null)
    }

    @Test
    fun observeExpenses_filtersByCategoryAndDateRange() = runBlocking {
        repository.createExpense("Lunch", 1000, ExpenseCategory.FOOD, 10L, "")
        repository.createExpense("Bus", 2000, ExpenseCategory.TRANSPORT, 10L, "")
        repository.createExpense("Snack", 500, ExpenseCategory.FOOD, 20L, "")

        val foodInRange = repository.observeExpenses(
            category = ExpenseCategory.FOOD,
            startEpochDay = 5L,
            endEpochDay = 15L,
        ).first()
        assertEquals(1, foodInRange.size)
        assertEquals("Lunch", foodInRange[0].title)
    }

    @Test
    fun createExpense_rejectsZeroAmount_andPersistsNothing() = runBlocking {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.createExpense("Lunch", 0L, ExpenseCategory.FOOD, 100L, "")
            }
        }
        assertTrue(repository.expenses.first().isEmpty())
    }

    @Test
    fun createExpense_rejectsNegativeAmount_andPersistsNothing() = runBlocking {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.createExpense("Lunch", -50L, ExpenseCategory.FOOD, 100L, "")
            }
        }
        assertTrue(repository.expenses.first().isEmpty())
    }

    @Test
    fun updateExpense_rejectsZeroAmount_andPreservesStoredValue() = runBlocking {
        val id = repository.createExpense("Lunch", 15050, ExpenseCategory.FOOD, 100L, "")
        val before = repository.expenses.first().single { it.id == id }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.updateExpense(before.copy(amountMinor = 0L))
            }
        }
        val after = repository.expenses.first().single { it.id == id }
        assertEquals(15050L, after.amountMinor)
        assertEquals(before.updatedAt, after.updatedAt)
    }

    @Test
    fun updateExpense_rejectsNegativeAmount_andPreservesStoredValue() = runBlocking {
        val id = repository.createExpense("Lunch", 15050, ExpenseCategory.FOOD, 100L, "")
        val before = repository.expenses.first().single { it.id == id }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.updateExpense(before.copy(amountMinor = -1L))
            }
        }
        val after = repository.expenses.first().single { it.id == id }
        assertEquals(15050L, after.amountMinor)
        assertEquals(before.updatedAt, after.updatedAt)
    }

    @Test
    fun unknownCategoryInStorage_mapsToOther() = runBlocking {
        // Insert a row with an unknown category id directly via the DAO.
        database.expenseDao().insert(
            com.campushelper.app.data.entity.ExpenseEntity(
                title = "Mystery",
                amountMinor = 100L,
                categoryId = "nope",
                expenseDateEpochDay = 1L,
                note = "",
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
        val loaded = repository.expenses.first().single()
        assertEquals(ExpenseCategory.OTHER, loaded.category)
    }
}

