package com.campushelper.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database row for one recorded expense (Expenses feature, schema version 3).
 *
 * Storage contract for the "expenses" table:
 *  - `id`: auto-generated row id - the stable key across restarts,
 *  - `title`: the user's description, stored verbatim (no trimming),
 *  - `amount_minor`: money in centavos (Long) - never Float/Double,
 *  - `category`: stable id of an [com.campushelper.app.domain.model.ExpenseCategory],
 *  - `expense_date_epoch_day`: calendar date as days since the Unix epoch,
 *  - `note`: optional free text, stored verbatim,
 *  - `created_at` / `updated_at`: epoch milliseconds, stamped by the repository.
 *
 * All "data.*" classes describe how data is STORED. Presentation code must not
 * depend on them - it uses domain/model/Expense.kt instead.
 */
@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["expense_date_epoch_day"]),
        Index(value = ["category"]),
    ],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,

    @ColumnInfo(name = "category")
    val categoryId: String,

    @ColumnInfo(name = "expense_date_epoch_day")
    val expenseDateEpochDay: Long,

    @ColumnInfo(name = "note")
    val note: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
