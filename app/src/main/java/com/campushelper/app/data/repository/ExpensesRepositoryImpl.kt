package com.campushelper.app.data.repository

import com.campushelper.app.data.dao.ExpenseDao
import com.campushelper.app.data.entity.ExpenseEntity
import com.campushelper.app.domain.model.Expense
import com.campushelper.app.domain.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed implementation of [ExpensesRepository]; built by ServiceLocator.
 *
 * Responsibilities (only these):
 *  - call the ExpenseDao (nothing else does),
 *  - stamp every write with ONE clock read per operation,
 *  - map ExpenseEntity <-> Expense so data.* types never leave this file.
 */
internal class ExpensesRepositoryImpl(
    private val expenseDao: ExpenseDao,
) : ExpensesRepository {

    override val expenses: Flow<List<Expense>> =
        expenseDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeExpense(id: Long): Flow<Expense?> =
        expenseDao.observeById(id).map { entity -> entity?.toDomain() }

    override fun observeExpenses(
        category: ExpenseCategory?,
        startEpochDay: Long?,
        endEpochDay: Long?,
    ): Flow<List<Expense>> =
        expenseDao.observeFiltered(
            categoryId = category?.id,
            startEpochDay = startEpochDay,
            endEpochDay = endEpochDay,
        ).map { entities -> entities.map { it.toDomain() } }

    override suspend fun createExpense(
        title: String,
        amountMinor: Long,
        category: ExpenseCategory,
        expenseDateEpochDay: Long,
        note: String,
    ): Long {
        // Enforce the domain invariant at the write boundary: a non-positive
        // amount can never be persisted, even if a caller bypasses the
        // amount parser (parseExpenseAmount). Throwing (not clamping) keeps
        // the failure visible instead of silently storing bad data.
        require(amountMinor > 0) {
            "amountMinor must be a positive number of centavos, was $amountMinor"
        }
        val timestamp = System.currentTimeMillis()
        return expenseDao.insert(
            ExpenseEntity(
                title = title,
                amountMinor = amountMinor,
                categoryId = category.id,
                expenseDateEpochDay = expenseDateEpochDay,
                note = note,
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    override suspend fun updateExpense(expense: Expense) {
        require(expense.amountMinor > 0) {
            "amountMinor must be a positive number of centavos, was ${expense.amountMinor}"
        }
        expenseDao.update(expense.toEntity(stamp = System.currentTimeMillis()))
    }

    override suspend fun deleteExpense(id: Long) = expenseDao.deleteById(id)

    // --- Entity <-> domain mapping ------------------------------------------

    private fun ExpenseEntity.toDomain(): Expense = Expense(
        id = id,
        title = title,
        amountMinor = amountMinor,
        category = ExpenseCategory.fromId(categoryId),
        expenseDateEpochDay = expenseDateEpochDay,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun Expense.toEntity(stamp: Long): ExpenseEntity = ExpenseEntity(
        id = id,
        title = title,
        amountMinor = amountMinor,
        categoryId = category.id,
        expenseDateEpochDay = expenseDateEpochDay,
        note = note,
        createdAt = createdAt,
        updatedAt = stamp,
    )
}
