package com.campushelper.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseAmountTest {

    @Test
    fun parsesTypicalValues() {
        assertEquals(15050L, parseExpenseAmount("150.50"))
        assertEquals(125000L, parseExpenseAmount("1250.00"))
        assertEquals(2500L, parseExpenseAmount("25"))
        assertEquals(2500L, parseExpenseAmount("25.00"))
        assertEquals(1L, parseExpenseAmount("0.01"))
        assertEquals(100L, parseExpenseAmount("1"))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(parseExpenseAmount(""))
        assertNull(parseExpenseAmount("   "))
        assertNull(parseExpenseAmount("abc"))
        assertNull(parseExpenseAmount("12.345"))
        assertNull(parseExpenseAmount("0"))
        assertNull(parseExpenseAmount("0.00"))
        assertNull(parseExpenseAmount("-5"))
        assertNull(parseExpenseAmount("-5.00"))
        assertNull(parseExpenseAmount("1.2.3"))
        assertNull(parseExpenseAmount("1,250.00")) // thousands separators not accepted
    }

    @Test
    fun formatsPesoString() {
        assertEquals("₱150.50", 15050L.toPesoString())
        assertEquals("₱1250.00", 125000L.toPesoString())
        assertEquals("₱0.00", 0L.toPesoString())
        assertEquals("₱25.00", 2500L.toPesoString())
        assertEquals("-₱5.00", (-500L).toPesoString())
    }
}
