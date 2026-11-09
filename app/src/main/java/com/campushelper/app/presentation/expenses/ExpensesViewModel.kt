package com.campushelper.app.presentation.expenses

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campushelper.app.R
import com.campushelper.app.core.di.ServiceLocator
import com.campushelper.app.data.repository.ExpensesRepository
import com.campushelper.app.domain.model.Expense
import com.campushelper.app.domain.model.ExpenseCategory
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Period filters for the Expenses overview.
 *
 * Boundaries are computed from the current local date (injected via [Clock]):
 *  - [ALL]: no date restriction.
 *  - [TODAY]: the current calendar day only.
 *  - [THIS_WEEK]: Monday..Sunday containing today.
 *  - [THIS_MONTH]: first..last day of the current month.
 */
enum class ExpensePeriodFilter {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
}

/**
 * One row of the category breakdown: a category, its summed amount and count.
 * Sorted by amount descending, then category id for determinism.
 */
data class CategoryTotal(
    val category: ExpenseCategory,
    val amountMinor: Long,
    val count: Int,
)

/**
 * Immutable UI snapshot of the Expenses overview.
 *
 * Deliberately small state: [totalCount] and [isEmpty] are DERIVED (stored
 * values would drift from [expenses]), so they are exposed as computed
 * properties instead of fields.
 *
 * Daily and monthly totals are computed over ALL stored expenses for their
 * respective calendar periods, regardless of the selected [periodFilter] or
 * [categoryFilter], so the summary stays honest about real spending.
 */
data class ExpensesUiState(
    /** True until the first list emission arrives (honest loading state). */
    val isLoading: Boolean = true,

    /** Expenses after applying [periodFilter] and [categoryFilter]. */
    val expenses: List<Expense> = emptyList(),

    val periodFilter: ExpensePeriodFilter = ExpensePeriodFilter.ALL,
    val categoryFilter: ExpenseCategory? = null,

    /** Sum of all expenses dated today (calendar day, all categories). */
    val todayTotalMinor: Long = 0L,

    /** Sum of all expenses dated in the current month (all categories). */
    val monthTotalMinor: Long = 0L,

    /** Per-category sums/counts over the *filtered* list, sorted by amount desc. */
    val categoryBreakdown: List<CategoryTotal> = emptyList(),

    @StringRes val errorResId: Int? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && expenses.isEmpty() && errorResId == null

    val totalCount: Int
        get() = expenses.size
}

/**
 * ViewModel of the Expenses overview (Phase 4.2).
 *
 * Follows the NotesViewModel conventions: a single reactive pipeline in
 * `viewModelScope`, repository-only dependencies, honest loading/error states,
 * and a testable date source (injected [Clock]).
 *
 * Date-dependent values (today/month totals, period boundaries) are evaluated
 * when the pipeline runs: on creation and whenever [refreshDate] is called.
 * They do NOT auto-refresh at midnight — call [refreshDate] from a lifecycle
 * event (e.g. ON_RESUME) if a long-lived screen needs it. No background
 * scheduling is used in this phase.
 */
class ExpensesViewModel(
    private val repository: ExpensesRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    private val periodFilter = MutableStateFlow(ExpensePeriodFilter.ALL)
    private val categoryFilter = MutableStateFlow<ExpenseCategory?>(null)
    private val dateRefresh = MutableStateFlow(0)

    init {
        viewModelScope.launch { observeUntilCleared() }
    }

    private suspend fun observeUntilCleared() {
        while (currentCoroutineContext().isActive) {
            try {
                combine(
                    repository.expenses,
                    periodFilter,
                    categoryFilter,
                    dateRefresh,
                ) { all, period, category, _ ->
                    val today = today()
                    val filtered = all.filter { expense ->
                        matchesPeriod(expense, period, today) &&
                            (category == null || expense.category == category)
                    }
                    val todayTotal = safeSum(all.filter { it.expenseDateEpochDay == today.toEpochDay() })
                    val monthTotal = safeSum(
                        all.filter { isInMonth(it.expenseDateEpochDay, today) },
                    )
                    val breakdown = filtered
                        .groupBy { it.category }
                        .map { (cat, exps) ->
                            CategoryTotal(
                                category = cat,
                                amountMinor = safeSum(exps) ?: 0L,
                                count = exps.size,
                            )
                        }
                        .sortedWith(compareByDescending<CategoryTotal> { it.amountMinor }
                            .thenBy { it.category.id })
                    ComputeResult(filtered, todayTotal, monthTotal, breakdown)
                }.collect { result ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            expenses = result.filtered,
                            periodFilter = periodFilter.value,
                            categoryFilter = categoryFilter.value,
                            todayTotalMinor = result.todayTotal ?: current.todayTotalMinor,
                            monthTotalMinor = result.monthTotal ?: current.monthTotalMinor,
                            categoryBreakdown = result.categoryBreakdown,
                            errorResId = if (result.overflow) R.string.expenses_error_overflow else null,
                        )
                    }
                }
                break
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update { current ->
                    current.copy(isLoading = false, errorResId = R.string.expenses_error_load_failed)
                }
                delay(RETRY_DELAY_MILLIS)
            }
        }
    }

    fun setPeriodFilter(filter: ExpensePeriodFilter) {
        periodFilter.value = filter
    }

    fun setCategoryFilter(category: ExpenseCategory?) {
        categoryFilter.value = category
    }

    fun clearCategoryFilter() {
        categoryFilter.value = null
    }

    /** Re-evaluates date-dependent boundaries (today/month totals, period ranges). */
    fun refreshDate() {
        dateRefresh.value = dateRefresh.value + 1
    }

    private fun today(): LocalDate = LocalDate.now(clock)

    private fun matchesPeriod(expense: Expense, period: ExpensePeriodFilter, today: LocalDate): Boolean {
        val day = expense.expenseDateEpochDay
        return when (period) {
            ExpensePeriodFilter.ALL -> true
            ExpensePeriodFilter.TODAY -> day == today.toEpochDay()
            ExpensePeriodFilter.THIS_WEEK -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
                val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).toEpochDay()
                day in start..end
            }
            ExpensePeriodFilter.THIS_MONTH -> {
                val start = today.withDayOfMonth(1).toEpochDay()
                val end = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()
                day in start..end
            }
        }
    }

    private fun isInMonth(epochDay: Long, today: LocalDate): Boolean {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.year == today.year && date.month == today.month
    }

    private fun safeSum(expenses: List<Expense>): Long? {
        var total = 0L
        for (expense in expenses) {
            total = try {
                Math.addExact(total, expense.amountMinor)
            } catch (e: ArithmeticException) {
                return null
            }
        }
        return total
    }

    private data class ComputeResult(
        val filtered: List<Expense>,
        val todayTotal: Long?,
        val monthTotal: Long?,
        val categoryBreakdown: List<CategoryTotal>,
    ) {
        val overflow: Boolean
            get() = todayTotal == null || monthTotal == null
    }

    companion object {
        private const val RETRY_DELAY_MILLIS = 3_000L

        fun create(context: Context): ExpensesViewModel =
            ExpensesViewModel(ServiceLocator.provideExpensesRepository(context))
    }
}
