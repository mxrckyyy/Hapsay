package com.campushelper.app.presentation.expenses

import com.campushelper.app.R
import com.campushelper.app.data.repository.ExpensesRepository
import com.campushelper.app.domain.model.Expense
import com.campushelper.app.domain.model.ExpenseCategory
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    // Fixed "today" = Monday 2026-11-09 (UTC). Month: Nov 1..30; week: Nov 9..15.
    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-11-09T12:00:00Z"),
        ZoneId.of("UTC"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun expense(id: Long, title: String, amount: Long, category: ExpenseCategory, date: Long) =
        Expense(id = id, title = title, amountMinor = amount, category = category,
            expenseDateEpochDay = date, note = "", createdAt = 1L, updatedAt = 1L)

    private open class FakeExpensesRepository(initial: List<Expense> = emptyList()) : ExpensesRepository {
        val expensesFlow = MutableStateFlow(initial)
        var failNextObservation: Exception? = null
        open override val expenses: Flow<List<Expense>> = flow {
            failNextObservation?.let { val f = it; failNextObservation = null; throw f }
            emitAll(expensesFlow)
        }
        override fun observeExpense(id: Long) = expensesFlow.map { it.firstOrNull { e -> e.id == id } }
        override fun observeExpenses(category: ExpenseCategory?, start: Long?, end: Long?) =
            expensesFlow.map { list ->
                list.filter { e ->
                    (category == null || e.category == category) &&
                        (start == null || e.expenseDateEpochDay >= start) &&
                        (end == null || e.expenseDateEpochDay <= end)
                }
            }
        override suspend fun createExpense(title: String, amountMinor: Long, category: ExpenseCategory,
            expenseDateEpochDay: Long, note: String): Long = 0L
        override suspend fun updateExpense(expense: Expense) = Unit
        override suspend fun deleteExpense(id: Long) = Unit
    }

    private val todayEpoch = java.time.LocalDate.parse("2026-11-09").toEpochDay()
    private val mondayEpoch = todayEpoch // Nov 9 2026 is a Monday
    private val sundayEpoch = todayEpoch + 6
    private val monthStart = java.time.LocalDate.parse("2026-11-01").toEpochDay()
    private val monthEnd = java.time.LocalDate.parse("2026-11-30").toEpochDay()
    private val lastMonth = java.time.LocalDate.parse("2026-10-15").toEpochDay()

    // --- Initial / observation ---------------------------------------------

    @Test
    fun initialState_beforeEmission_isLoading() = runTest(testDispatcher) {
        val vm = ExpensesViewModel(FakeExpensesRepository(), fixedClock)
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun emptyRepository_showsEmptyState() = runTest(testDispatcher) {
        val vm = ExpensesViewModel(FakeExpensesRepository(), fixedClock)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertTrue(state.expenses.isEmpty())
        assertEquals(0, state.totalCount)
        assertEquals(0L, state.todayTotalMinor)
        assertEquals(0L, state.monthTotalMinor)
        assertTrue(state.categoryBreakdown.isEmpty())
        assertNull(state.errorResId)
    }

    @Test
    fun populatedRepository_loadsExpenses() = runTest(testDispatcher) {
        val data = listOf(
            expense(1, "Lunch", 15050, ExpenseCategory.FOOD, todayEpoch),
            expense(2, "Bus", 2500, ExpenseCategory.TRANSPORT, todayEpoch),
        )
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.expenses.size)
        assertEquals(2, state.totalCount)
        assertNull(state.errorResId)
    }

    @Test
    fun repositoryError_showsLoadError_thenRecovers() = runTest(testDispatcher) {
        val fake = FakeExpensesRepository()
        fake.failNextObservation = IllegalStateException("storage hiccup")
        val vm = ExpensesViewModel(fake, fixedClock)

        advanceTimeBy(301)
        val failing = vm.uiState.value
        assertFalse(failing.isLoading)
        assertEquals(R.string.expenses_error_load_failed, failing.errorResId)

        advanceUntilIdle()
        val recovered = vm.uiState.value
        assertNull(recovered.errorResId)
        assertTrue(recovered.expenses.isEmpty())
    }

    // --- Period filtering ---------------------------------------------------

    @Test
    fun periodFilter_all_showsEverything() = runTest(testDispatcher) {
        val data = listOf(
            expense(1, "Today", 100, ExpenseCategory.FOOD, todayEpoch),
            expense(2, "LastMonth", 200, ExpenseCategory.FOOD, lastMonth),
        )
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        vm.setPeriodFilter(ExpensePeriodFilter.ALL)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.expenses.size)
    }

    @Test
    fun periodFilter_today_includesOnlyToday() = runTest(testDispatcher) {
        val data = listOf(
            expense(1, "Today", 100, ExpenseCategory.FOOD, todayEpoch),
            expense(2, "Yesterday", 200, ExpenseCategory.FOOD, todayEpoch - 1),
        )
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        vm.setPeriodFilter(ExpensePeriodFilter.TODAY)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.expenses.size)
        assertEquals("Today", vm.uiState.value.expenses[0].title)
    }

    @Test
    fun periodFilter_thisWeek_mondayToSundayInclusive() = runTest(testDispatcher) {
        val data = listOf(
            expense(1, "Mon", 100, ExpenseCategory.FOOD, mondayEpoch),       // Monday (inclusive start)
            expense(2, "Sun", 200, ExpenseCategory.FOOD, sundayEpoch),       // Sunday (inclusive end)
            expense(3, "PrevSun", 300, ExpenseCategory.FOOD, mondayEpoch - 1), // before
            expense(4, "NextMon", 400, ExpenseCategory.FOOD, sundayEpoch + 1), // after
        )
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        vm.setPeriodFilter(ExpensePeriodFilter.THIS_WEEK)
        advanceUntilIdle()
        val titles = vm.uiState.value.expenses.map { it.title }.toSet()
        assertEquals(setOf("Mon", "Sun"), titles)
    }

    @Test
    fun periodFilter_thisMonth_firstToLastInclusive() = runTest(testDispatcher) {
        val data = listOf(
            expense(1, "First", 100, ExpenseCategory.FOOD, monthStart),
            expense(2, "Last", 200, ExpenseCategory.FOOD, monthEnd),
            expense(3, "PrevMonth", 300, ExpenseCategory.FOOD, monthStart - 1),
        )
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        vm.setPeriodFilter(ExpensePeriodFilter.THIS_MONTH)
        advanceUntilIdle()
        val titles = vm.uiState.value.expenses.map { it.title }.toSet()
        assertEquals(setOf("First", "Last"), titles)
    }

    // --- Category filtering -----------------------------------------------

    @Test
    fun categoryFilter_selectAndClear() = runTest(testDispatcher) {
        val data = listOf(
            expense(1, "Lunch", 100, ExpenseCategory.FOOD, todayEpoch),
            expense(2, "Bus", 200, ExpenseCategory.TRANSPORT, todayEpoch),
        )
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        vm.setCategoryFilter(ExpenseCategory.FOOD)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.expenses.size)
        assertEquals(ExpenseCategory.FOOD, vm.uiState.value.categoryFilter)
        vm.clearCategoryFilter()
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.expenses.size)
        assertNull(vm.uiState.value.categoryFilter)
    }

    @Test
    fun categoryFilter_combinesWithPeriodFilter() = runTest(testDispatcher) {
        val data = listOf(
            expense(1, "FoodToday", 100, ExpenseCategory.FOOD, todayEpoch),
            expense(2, "TransportToday", 200, ExpenseCategory.TRANSPORT, todayEpoch),
            expense(3, "FoodLastMonth", 300, ExpenseCategory.FOOD, lastMonth),
        )
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        vm.setPeriodFilter(ExpensePeriodFilter.TODAY)
        vm.setCategoryFilter(ExpenseCategory.FOOD)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.expenses.size)
        assertEquals("FoodToday", vm.uiState.value.expenses[0].title)
    }

    @Test
    fun changingPeriodFilter_preservesCategoryFilter() = runTest(testDispatcher) {
        val data = listOf(expense(1, "Lunch", 100, ExpenseCategory.FOOD, todayEpoch))
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        vm.setCategoryFilter(ExpenseCategory.FOOD)
        vm.setPeriodFilter(ExpensePeriodFilter.TODAY)
        advanceUntilIdle()
        assertEquals(ExpenseCategory.FOOD, vm.uiState.value.categoryFilter)
    }

    @Test
    fun filterWithNoMatches_showsEmptyButNotError() = runTest(testDispatcher) {
        val data = listOf(expense(1, "Lunch", 100, ExpenseCategory.FOOD, todayEpoch))
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        vm.setCategoryFilter(ExpenseCategory.HEALTH)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.expenses.isEmpty())
        assertTrue(state.isEmpty)
        assertNull(state.errorResId)
    }

    // --- Summary calculations ----------------------------------------------

    @Test
    fun todayTotal_sumsOnlyTodayExpenses() = runTest(testDispatcher) {
        val data = listOf(
            expense(1, "Today1", 100, ExpenseCategory.FOOD, todayEpoch),
            expense(2, "Today2", 200, ExpenseCategory.FOOD, todayEpoch),
            expense(3, "OtherDay", 999, ExpenseCategory.FOOD, todayEpoch - 1),
        )
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        assertEquals(300L, vm.uiState.value.todayTotalMinor)
    }

    @Test
    fun monthTotal_sumsOnlyCurrentMonthExpenses() = runTest(testDispatcher) {
        val data = listOf(
            expense(1, "Nov", 100, ExpenseCategory.FOOD, monthStart),
            expense(2, "Nov2", 200, ExpenseCategory.FOOD, monthEnd),
            expense(3, "Oct", 999, ExpenseCategory.FOOD, lastMonth),
        )
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        assertEquals(300L, vm.uiState.value.monthTotalMinor)
    }

    @Test
    fun categoryBreakdown_groupsSumsAndCounts_overFilteredList() = runTest(testDispatcher) {
        val data = listOf(
            expense(1, "Lunch", 100, ExpenseCategory.FOOD, todayEpoch),
            expense(2, "Snack", 50, ExpenseCategory.FOOD, todayEpoch),
            expense(3, "Bus", 200, ExpenseCategory.TRANSPORT, todayEpoch),
        )
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        val breakdown = vm.uiState.value.categoryBreakdown
        assertEquals(2, breakdown.size)
        assertEquals(ExpenseCategory.TRANSPORT, breakdown[0].category)
        assertEquals(200L, breakdown[0].amountMinor)
        assertEquals(1, breakdown[0].count)
        assertEquals(ExpenseCategory.FOOD, breakdown[1].category)
        assertEquals(150L, breakdown[1].amountMinor)
        assertEquals(2, breakdown[1].count)
    }

    @Test
    fun categoryBreakdown_excludesZeroCategories() = runTest(testDispatcher) {
        val data = listOf(expense(1, "Lunch", 100, ExpenseCategory.FOOD, todayEpoch))
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        val breakdown = vm.uiState.value.categoryBreakdown
        assertEquals(1, breakdown.size)
        assertTrue(breakdown.none { it.category == ExpenseCategory.TRANSPORT })
    }

    @Test
    fun categoryBreakdown_emptyWhenNoExpenses() = runTest(testDispatcher) {
        val vm = ExpensesViewModel(FakeExpensesRepository(), fixedClock)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.categoryBreakdown.isEmpty())
    }

    @Test
    fun categoryBreakdown_deterministicOrdering() = runTest(testDispatcher) {
        // Two categories with equal amounts: tie-break by id.
        val data = listOf(
            expense(1, "A", 100, ExpenseCategory.FOOD, todayEpoch),
            expense(2, "B", 100, ExpenseCategory.HEALTH, todayEpoch),
        )
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        val ids = vm.uiState.value.categoryBreakdown.map { it.category.id }
        // "food" < "health" alphabetically
        assertEquals(listOf("food", "health"), ids)
    }

    @Test
    fun largeMonetaryValues_doNotOverflow_whenWithinRange() = runTest(testDispatcher) {
        val big = 1_000_000_000_000L // 10 billion pesos in centavos
        val data = listOf(
            expense(1, "A", big, ExpenseCategory.FOOD, todayEpoch),
            expense(2, "B", big, ExpenseCategory.FOOD, todayEpoch),
        )
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceUntilIdle()
        assertEquals(2_000_000_000_000L, vm.uiState.value.todayTotalMinor)
    }

    @Test
    fun monetaryOverflow_setsErrorState() = runTest(testDispatcher) {
        val data = listOf(
            expense(1, "A", Long.MAX_VALUE, ExpenseCategory.FOOD, todayEpoch),
            expense(2, "B", 1L, ExpenseCategory.FOOD, todayEpoch),
        )
        val vm = ExpensesViewModel(FakeExpensesRepository(data), fixedClock)
        advanceTimeBy(301)
        assertEquals(R.string.expenses_error_overflow, vm.uiState.value.errorResId)
    }

    // --- Date behavior -----------------------------------------------------

    @Test
    fun refreshDate_recomputesDateDependentTotals() = runTest(testDispatcher) {
        val vm = ExpensesViewModel(FakeExpensesRepository(), fixedClock)
        advanceUntilIdle()
        vm.refreshDate()
        advanceUntilIdle()
        // No crash; state remains consistent.
        assertNull(vm.uiState.value.errorResId)
    }
}
