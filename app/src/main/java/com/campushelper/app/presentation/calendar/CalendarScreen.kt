package com.campushelper.app.presentation.calendar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.campushelper.app.presentation.components.PlaceholderScreen

/** Calendar screen placeholder. Quizzes, deadlines and activities are implemented in the Calendar phase. */
@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Calendar",
        description = "Monthly calendar with quizzes, deadlines and academic activities.",
        modifier = modifier,
    )
}
