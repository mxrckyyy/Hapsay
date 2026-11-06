package com.campushelper.app.data.repository

import com.campushelper.app.domain.model.Expense
import com.campushelper.app.domain.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

/**
 * Data boundary of the Expenses feature.
 *
 * Presentations depend on this interface only - never on ExpenseDao or the
 * entity classes (dependency rules in ARCHITECTURE.md §4). Implementations are
 * registered in core/di/ServiceLocator.
 */
interface ExpensesRepository {

    /** All expenses, newest dates first, as a live stream. */
    val expenses: Flow<List<Expense>>

    /** One expense by id, as a live stream; emits null once it is deleted. */
    fun observeExpense(id: Long): Flow<Expense?>

    /** Expenses filtered by optional category and/or inclusive date range. */
    fun observeExpenses(
        category: ExpenseCategory? = null,
        startEpochDay: Long? = null,
        endEpochDay: Long? = null,
    ): Flow<List<Expense>>

    /** Creates an expense; stamps createdAt = updatedAt = now. Returns the new id. */
    suspend fun createExpense(
        title: String,
        amountMinor: Long,
        category: ExpenseCategory,
        expenseDateEpochDay: Long,
        note: String,
    ): Long

    /** Updates an expense: preserves id and createdAt, re-stamps updatedAt. */
    suspend fun updateExpense(expense: Expense)

    /** Deletes an expense by id. No-op when the id is unknown. */
    suspend fun deleteExpense(id: Long)
}
