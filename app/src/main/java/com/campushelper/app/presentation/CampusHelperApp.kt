package com.campushelper.app.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.campushelper.app.presentation.components.CampusBottomNavigation
import com.campushelper.app.presentation.navigation.CampusHelperNavHost

/**
 * Root Compose UI: a SINGLE Scaffold whose bottomBar is the Campus Helper
 * bottom navigation, hosting the single NavHost with the five destinations.
 *
 * App structure rules:
 * - exactly one Scaffold and one bottom bar (they live here),
 * - the NavHost decorates as little as possible (no second Scaffold),
 * - screens never depend on the Scaffold; they are pure content.
 */
@Composable
fun CampusHelperApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            CampusBottomNavigation(navController = navController)
        },
    ) { innerPadding ->
        CampusHelperNavHost(
            navController = navController,
            // innerPadding keeps content out from under the bottom bar and
            // system bars (edge-to-edge).
            modifier = Modifier.padding(innerPadding),
        )
    }
}
