package com.campushelper.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.campushelper.app.data.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for expenses (schema version 3).
 *
 * The repository (data/repository/ExpensesRepositoryImpl) is the ONLY class
 * that should call this. ViewModels and composables never touch DAOs directly.
 *
 * Query conventions:
 *  - observation queries return [Flow] so collectors re-emit on every change;
 *  - write operations are suspend (Room runs them on its IO executor);
 *  - every query takes parameters - user input is never concatenated into SQL.
 *
 * Ordering: newest expense date first, with `id DESC` as the deterministic
 * tie-break for expenses recorded on the same date.
 */
@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY expense_date_epoch_day DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    fun observeById(id: Long): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expenses WHERE category = :categoryId ORDER BY expense_date_epoch_day DESC, id DESC")
    fun observeByCategory(categoryId: String): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT * FROM expenses WHERE expense_date_epoch_day >= :startEpochDay " +
            "AND expense_date_epoch_day <= :endEpochDay " +
            "ORDER BY expense_date_epoch_day DESC, id DESC",
    )
    fun observeByDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT * FROM expenses " +
            "WHERE (:categoryId IS NULL OR category = :categoryId) " +
            "AND (:startEpochDay IS NULL OR expense_date_epoch_day >= :startEpochDay) " +
            "AND (:endEpochDay IS NULL OR expense_date_epoch_day <= :endEpochDay) " +
            "ORDER BY expense_date_epoch_day DESC, id DESC",
    )
    fun observeFiltered(
        categoryId: String?,
        startEpochDay: Long?,
        endEpochDay: Long?,
    ): Flow<List<ExpenseEntity>>

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}
