package com.campushelper.app.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campushelper.app.ui.theme.Spacing

/**
 * Section title row used across the dashboard: bold title on the left and an
 * optional text action ("View all", "Open Calendar") on the right.
 *
 * The action only renders when both a label and a handler are provided.
 */
@Composable
fun CampusSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onActionClick != null) {
            // heightIn(min = 48dp) keeps the whole action row comfortable to
            // touch even though TextButton's default height is smaller.
            TextButton(
                onClick = onActionClick,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
