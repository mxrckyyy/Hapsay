package com.campushelper.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.campushelper.app.presentation.calendar.CalendarScreen
import com.campushelper.app.presentation.expenses.ExpensesScreen
import com.campushelper.app.presentation.home.HomeScreen
import com.campushelper.app.presentation.notes.NotesScreen
import com.campushelper.app.presentation.schedule.ScheduleScreen

/**
 * The single navigation graph of the app.
 *
 * Feature screens are wired here (route -> screen); the screens themselves
 * live in their own feature packages. Feature implementations will be attached
 * to these destinations phase by phase.
 */
@Composable
fun CampusHelperNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> {
            HomeScreen(
                // Home reports the user's intent; the navigation layer owns
                // the actual navigation calls (unidirectional data flow).
                onOpenNotes = { navController.navigateToTopLevel(NotesRoute) },
                onOpenExpenses = { navController.navigateToTopLevel(ExpensesRoute) },
                onOpenCalendar = { navController.navigateToTopLevel(CalendarRoute) },
                onOpenSchedule = { navController.navigateToTopLevel(ScheduleRoute) },
            )
        }
        composable<NotesRoute> {
            NotesScreen()
        }
        composable<ExpensesRoute> {
            ExpensesScreen()
        }
        composable<CalendarRoute> {
            CalendarScreen()
        }
        composable<ScheduleRoute> {
            ScheduleScreen()
        }
    }
}
