package com.campushelper.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Stable, persisted identifier for an expense category.
 *
 * The [id] is what Room stores (a String column), so it must never change
 * once shipped. The enum's `name` is an implementation detail and is NOT the
 * persisted value. Unknown ids loaded from storage degrade to [OTHER].
 *
 * Icon resolution is deliberately NOT done here: this enum stays free of
 * Compose types. The presentation layer maps [ExpenseCategory] to an
 * `ImageVector` (see the Expenses screens, Phase 4.3+).
 */
enum class ExpenseCategory(
    val id: String,
    val displayName: String,
) {
    FOOD("food", "Food"),
    TRANSPORT("transport", "Transportation"),
    SCHOOL("school", "School"),
    PERSONAL("personal", "Personal"),
    SHOPPING("shopping", "Shopping"),
    BILLS("bills", "Bills"),
    ENTERTAINMENT("entertainment", "Entertainment"),
    HEALTH("health", "Health"),
    OTHER("other", "Other");

    companion object {
        /** Maps a persisted id to its category; unknown/blank ids -> [OTHER]. */
        fun fromId(id: String?): ExpenseCategory =
            entries.firstOrNull { it.id == id } ?: OTHER

        /** All stable ids, for validation and tests. */
        val stableIds: Set<String> = entries.map { it.id }.toSet()
    }
}
