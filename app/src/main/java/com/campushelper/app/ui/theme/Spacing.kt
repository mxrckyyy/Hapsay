package com.campushelper.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * App-wide spacing scale (4 / 8 / 12 / 16 / 20 / 24 / 32 dp).
 *
 * Screens must use these values instead of raw dp numbers so vertical rhythm
 * stays consistent across every feature. If you need a value that is not
 * here, extend this scale deliberately - do not scatter one-off dp values
 * across screen code.
 */
object Spacing {
    /** Tiny gaps inside a component (icon-to-label). */
    val extraSmall = 4.dp

    /** Small gaps: compact list internal spacing. */
    val small = 8.dp

    /** Small-to-medium gaps: content padding inside cards. */
    val smallMedium = 12.dp

    /** Standard gap between components; also the screen side padding. */
    val medium = 16.dp

    /** Gaps before large call-to-action blocks. */
    val mediumLarge = 20.dp

    /** Section separation. */
    val large = 24.dp

    /** Large screen-level gaps. */
    val extraLarge = 32.dp

    // Convenience alias used by screen layouts.
    val screenPadding = medium
}
