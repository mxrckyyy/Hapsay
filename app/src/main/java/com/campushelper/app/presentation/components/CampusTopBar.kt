package com.campushelper.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.campushelper.app.ui.theme.Spacing

/**
 * Dashboard header row: the app identity mark plus the greeting block.
 *
 * It deliberately does NOT include actions. Settings or profile actions must
 * only appear here once a real destination exists - placeholder buttons that
 * do nothing are not allowed in this app.
 */
@Composable
fun CampusTopBar(
    title: String,
    subtitle: String,
    logo: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CampusIconContainer(
            icon = logo, // default 40dp identity mark (no override needed)
        )
        Spacer(Modifier.width(Spacing.smallMedium))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
