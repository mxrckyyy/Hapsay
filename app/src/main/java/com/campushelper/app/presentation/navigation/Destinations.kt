package com.campushelper.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/*
 * Type-safe navigation routes.
 *
 * Each top-level destination is a Kotlin object annotated with @Serializable
 * (the kotlinx.serialization plugin generates the code Navigation uses to
 * match the current entry to the route). To add a screen in a future feature:
 * 1. Add a route object here.
 * 2. Add a `composable<Route> { ... }` line in CampusHelperNavHost.
 * 3. (When it needs a bottom-bar entry) add a TopLevelRoute below.
 */

@Serializable
data object HomeRoute

@Serializable
data object NotesRoute

@Serializable
data object ExpensesRoute

@Serializable
data object CalendarRoute

@Serializable
data object ScheduleRoute

/** One entry in the bottom navigation bar: a label, an icon and its route. */
data class TopLevelRoute<T : Any>(
    val name: String,
    val icon: ImageVector,
    val route: T,
)

/** The five primary destinations of Campus Helper, in display order. */
val topLevelRoutes: List<TopLevelRoute<Any>> = listOf(
    TopLevelRoute("Home", Icons.Filled.Home, HomeRoute),
    TopLevelRoute("Notes", Icons.AutoMirrored.Filled.List, NotesRoute),
    TopLevelRoute("Expenses", Icons.Filled.ShoppingCart, ExpensesRoute),
    TopLevelRoute("Calendar", Icons.Filled.DateRange, CalendarRoute),
    TopLevelRoute("Schedule", Icons.Filled.Schedule, ScheduleRoute),
)
