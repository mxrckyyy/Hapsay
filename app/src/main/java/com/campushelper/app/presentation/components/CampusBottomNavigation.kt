package com.campushelper.app.presentation.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.campushelper.app.presentation.navigation.navigateToTopLevel
import com.campushelper.app.presentation.navigation.topLevelRoutes

/**
 * The single bottom navigation bar of the app (extracted from
 * CampusHelperApp for clarity - it is still the ONLY bottom bar).
 *
 * Styling: flat dark surface, subtle green selected state, neutral gray
 * unselected items. Selection highlight relies on the system-enforced 48dp
 * touch targets provided by NavigationBarItem.
 */
@Composable
fun CampusBottomNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        // Flat bar - the surface color alone separates it from the background.
        tonalElevation = 0.dp,
    ) {
        topLevelRoutes.forEach { item ->
            val selected = currentDestination?.hasRoute(item.route::class) == true
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTopLevel(item.route) },
                icon = {
                    // contentDescription = null: the visible label carries the
                    // name, so TalkBack announces each destination only once.
                    Icon(item.icon, contentDescription = null)
                },
                label = { Text(item.name) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            )
        }
    }
}
