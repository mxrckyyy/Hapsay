package com.campushelper.app.presentation.expenses

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.campushelper.app.presentation.components.PlaceholderScreen

/** Expenses screen placeholder. Expense tracking is implemented in the Expenses phase. */
@Composable
fun ExpensesScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Expenses",
        description = "Record expenses, view history and monthly totals.",
        modifier = modifier,
    )
}
