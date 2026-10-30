package com.campushelper.app.presentation.notes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.campushelper.app.presentation.components.PlaceholderScreen

/** Notes screen placeholder. Note CRUD is implemented in the Notes phase. */
@Composable
fun NotesScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Notes",
        description = "Create, edit, delete and view saved notes.",
        modifier = modifier,
    )
}
