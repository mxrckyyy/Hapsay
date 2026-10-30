package com.campushelper.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.campushelper.app.ui.theme.Spacing

/**
 * A small rounded square that hosts a single icon: the consistent container
 * used for quick-access items, dashboard cards and headers.
 *
 * Flat by design - no shadow or gradient, just an elevated surface with a
 * clipped shape, matching the minimalist direction.
 *
 * Sizing rules:
 *  - container defaults to 40dp (2 x mediumLarge); callers can override for
 *    special cases, but the icon always keeps the container's proportions,
 *  - the icon is 0.55 x its container, so a bigger container also scales its
 *    glyph (22dp for the default 40dp container),
 *  - the container itself is never a touch target - handle tapping outside.
 */
@Composable
fun CampusIconContainer(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = Spacing.mediumLarge * 2,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            // Shape token instead of a raw RoundedCornerShape: the rounding
            // stays consistent with the shape scale in Theme.kt (8dp small).
            .clip(MaterialTheme.shapes.small)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
