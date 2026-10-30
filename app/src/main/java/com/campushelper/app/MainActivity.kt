package com.campushelper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.campushelper.app.presentation.CampusHelperApp
import com.campushelper.app.ui.theme.CampusHelperTheme

/**
 * Application entry point ONLY.
 *
 * Responsibilities:
 * 1. Create the Activity.
 * 2. Draw the UI edge-to-edge.
 * 3. Apply the app theme and host the root Compose UI ([CampusHelperApp]).
 *
 * No app logic, navigation, or state lives here - everything is inside
 * the `presentation` packages so screens stay testable and organized.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Phase 1 is dark-only: request LIGHT (white) system bar icons so
        // they remain visible on the dark app surface even when the device
        // system theme is light. Revisit when the light theme is introduced.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            CampusHelperTheme {
                CampusHelperApp()
            }
        }
    }
}
