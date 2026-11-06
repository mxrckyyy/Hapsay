package com.campushelper.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Business model of one recorded expense, as the presentation layer sees it.
 *
 * Pure Kotlin: no Android or Room types. The data layer owns the mapping
 * between [Expense] and its storage representation (data/entity/ExpenseEntity).
 *
 * Money is stored as integer minor units (centavos) in [amountMinor] to
 * avoid floating-point error. Use [parseExpenseAmount] to convert user input
 * and [Long.toPesoString] to format for display.
 *
 * [expenseDateEpochDay] is a calendar date (days since the Unix epoch), not a
 * timestamp, so it never shifts with the device time zone.
 */
data class Expense(
    val id: Long,
    val title: String,
    val amountMinor: Long,
    val category: ExpenseCategory,
    val expenseDateEpochDay: Long,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Parses a user-entered peso amount into centavos.
 *
 * Returns `null` for any invalid input:
 *  - blank/whitespace-only,
 *  - non-numeric or malformed,
 *  - zero or negative,
 *  - more than two decimal places,
 *  - a value that does not fit in a `Long` (overflow).
 *
 * Uses [BigDecimal] (no floating point) and `HALF_UP` rounding only as a
 * safety net; inputs with more than two decimals are rejected outright rather
 * than silently rounded.
 */
fun parseExpenseAmount(input: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    val decimal = try {
        BigDecimal(trimmed)
    } catch (e: NumberFormatException) {
        return null
    }
    if (decimal.signum() <= 0) return null
    // Reject more than two decimal places.
    if (decimal.scale() > 2) return null
    // Normalize to exactly two decimals, then convert to centavos.
    val scaled = try {
        decimal.movePointRight(2).setScale(0, RoundingMode.HALF_UP)
    } catch (e: ArithmeticException) {
        return null
    }
    return try {
        scaled.longValueExact()
    } catch (e: ArithmeticException) {
        null
    }
}

/**
 * Formats a centavo amount as a peso string, e.g. `15050L -> "₱150.50"`.
 * Manual formatting guarantees the peso symbol and two decimals regardless of
 * device locale.
 */
fun Long.toPesoString(): String {
    val negative = this < 0
    val abs = if (negative) -this else this
    val pesos = abs / 100
    val cents = abs % 100
    return (if (negative) "-" else "") + "₱" + pesos.toString() + "." +
        cents.toString().padStart(2, '0')
}
