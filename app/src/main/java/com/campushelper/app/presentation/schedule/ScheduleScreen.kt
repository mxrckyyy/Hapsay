package com.campushelper.app.presentation.schedule

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.campushelper.app.presentation.components.PlaceholderScreen

/** Class schedule screen placeholder. The weekly timetable is implemented in the Schedule phase. */
@Composable
fun ScheduleScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Schedule",
        description = "Weekly class timetable with course, room and time information.",
        modifier = modifier,
    )
}
