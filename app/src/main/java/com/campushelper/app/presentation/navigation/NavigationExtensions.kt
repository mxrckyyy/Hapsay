package com.campushelper.app.presentation.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * Navigates to a top-level destination the way Google recommends for
 * bottom-bar destinations:
 *
 * - popUpTo the start destination with saveState: clears everything above the
 *   start destination while remembering its previous state.
 * - launchSingleTop: tapping the already-selected item does not stack a copy.
 * - restoreState: reopening a tab resumes where the user left off.
 */
fun <T : Any> NavHostController.navigateToTopLevel(route: T) {
    navigate(route) {
        // Pop up to the start destination of the graph to avoid building up
        // a big back stack as users switch tabs back and forth.
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        // Avoid multiple copies of the same destination when re-selecting it.
        launchSingleTop = true
        // Restore state when re-selecting a previously visited tab.
        restoreState = true
    }
}
