package com.campushelper.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseCategoryTest {

    @Test
    fun stableIdsAreUnique() {
        val ids = ExpenseCategory.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun fromIdMapsKnownIds() {
        assertEquals(ExpenseCategory.FOOD, ExpenseCategory.fromId("food"))
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromId("other"))
        assertEquals(ExpenseCategory.HEALTH, ExpenseCategory.fromId("health"))
    }

    @Test
    fun fromIdFallsBackToOtherForUnknownOrNull() {
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromId("nope"))
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromId(null))
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromId(""))
    }

    @Test
    fun stableIdsMatchApprovedSet() {
        val expected = setOf(
            "food", "transport", "school", "personal", "shopping",
            "bills", "entertainment", "health", "other",
        )
        assertEquals(expected, ExpenseCategory.stableIds)
    }
}
