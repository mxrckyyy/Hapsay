package com.campushelper.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.campushelper.app.ui.theme.Spacing

/**
 * Polished empty state for dashboard sections (Schedule, Events, summaries).
 *
 * Rules:
 * - The icon is decorative and muted; the text carries the meaning.
 * - The optional action must be a REAL, working navigation/behavior hook.
 * - It never displays invented data - only honest "not available yet" states.
 */
@Composable
fun CampusEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        // fillMaxWidth guarantees the centered layout even when this is
        // placed inside a start-aligned parent (e.g. CampusCard's Column) -
        // without it, the wrap-content Column would hug the left edge.
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CampusIconContainer(
            icon = icon,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.smallMedium))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.small))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(Modifier.height(Spacing.medium))
            CampusSecondaryButton(
                text = actionLabel,
                onClick = onActionClick,
            )
        }
    }
}
