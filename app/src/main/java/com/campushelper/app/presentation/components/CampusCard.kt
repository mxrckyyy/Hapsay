package com.campushelper.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campushelper.app.ui.theme.Spacing

/**
 * The standard dashboard card: flat (no elevation or shadow), primary surface
 * color, 12dp corners, uniform inner padding.
 *
 * Cards on Campus Helper never carry elevation so the visual hierarchy comes
 * from color contrast only - part of the minimalist direction.
 */
@Composable
fun CampusCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.medium),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.medium, // 12dp per the shape scale
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}
