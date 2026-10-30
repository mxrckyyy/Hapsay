package com.campushelper.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The secondary action button: quiet outlined style for supporting actions
 * (e.g. "Open Calendar" inside an empty state).
 *
 * Neutral text on a transparent surface with the standard divider color as
 * border - it guides without competing with the green primary action.
 */
@Composable
fun CampusSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
