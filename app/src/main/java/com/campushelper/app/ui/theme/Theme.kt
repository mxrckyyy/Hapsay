package com.campushelper.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/*
 * App theme - Phase 1 is DARK ONLY.
 *
 * The light theme will be introduced in a dedicated theming phase. Because the
 * app is dark-only for now, there is no `isSystemInDarkTheme()` branching and
 * dynamic (Material You) extraction is disabled - it would replace the green
 * brand identity with the user's wallpaper colors.
 */
private val CampusColorScheme = darkColorScheme(
    // Accent
    primary = GreenPrimary,
    onPrimary = GreenDeep,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenSoft,
    secondary = GreenSecondary,
    onSecondary = GreenDeep,
    secondaryContainer = GreenContainer,
    onSecondaryContainer = GreenSoft,
    tertiary = GreenSecondary,

    // Text
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,

    // Surfaces
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceSecondary,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceSecondary,
    surfaceContainerHighest = DarkSurfaceElevated,

    // Borders
    outline = DarkDivider,
    outlineVariant = DarkDivider,

    // Status colors
    error = ErrorRed,
)

/**
 * Corner rounding is deliberately restrained:
 * small controls 8dp - standard cards 12dp - large sections 16dp.
 */
private val CampusShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun CampusHelperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CampusColorScheme,
        typography = CampusTypography,
        shapes = CampusShapes,
        content = content,
    )
}
