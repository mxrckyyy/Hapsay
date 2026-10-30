package com.campushelper.app.presentation.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The primary action button: solid green fill, dark text, 12dp corners and a
 * comfortable 48dp minimum touch height.
 *
 * Use it sparingly - at most one primary action per view so the green accent
 * keeps its meaning.
 */
@Composable
fun CampusPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
