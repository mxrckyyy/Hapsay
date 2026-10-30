package com.campushelper.app.presentation.home

import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state of the Home dashboard.
 *
 * Phase 1 status of every data-backed section (verified by code inspection):
 *  - Schedule data:  no ScheduleDao/Course storage exists -> hasScheduleToday = false.
 *  - Calendar data:  no EventDao exists                    -> hasSavedEvents = false.
 *  - Weather:        no API integrated yet                 -> weather section renders
 *      an honest "not available yet" card (no fabricated temperatures).
 *  - Productivity:   notes/expenses/classes have no repositories yet -> honest
 *      setup state only. Numbers will be computed from real data later.
 *
 * The state object follows the app-wide convention: an immutable snapshot
 * exposed as a read-only [StateFlow]; the UI renders it and reports user
 * events back up (unidirectional data flow).
 */
data class HomeUiState(
    /**
     * The current device date, formatted for display ("Friday, October 30").
     * Produced from the real system clock - never hardcoded.
     */
    val dateLabel: String,

    /** True when the student has at least one class stored for today. */
    val hasScheduleToday: Boolean = false,

    /** True when at least one event exists in the calendar. */
    val hasSavedEvents: Boolean = false,
)

/**
 * ViewModel of the Home screen.
 *
 * Derived-state logic belongs here - never in composables. When future
 * phases connect repositories (courses, events, notes, weather), they will be
 * collected in `viewModelScope` and folded into `_uiState`; user interactions
 * continue to flow through the screen action callbacks, exactly as in
 * `HomeScreen` today.
 */
class HomeViewModel : ViewModel() {

    // Private, mutable state that only this ViewModel can change.
    private val _uiState = MutableStateFlow(HomeUiState(dateLabel = buildDateLabel()))

    // Public, read-only view of the state that the UI collects.
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Formats the actual current date using the device locale, e.g.
     * "Friday, October 30" (language follows the user's phone settings).
     */
    private fun buildDateLabel(): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
        return LocalDate.now().format(formatter)
    }
}
