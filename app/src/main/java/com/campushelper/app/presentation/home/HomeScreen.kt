package com.campushelper.app.presentation.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campushelper.app.presentation.components.CampusCard
import com.campushelper.app.presentation.components.CampusEmptyState
import com.campushelper.app.presentation.components.CampusIconContainer
import com.campushelper.app.presentation.components.CampusSectionHeader
import com.campushelper.app.presentation.components.CampusTopBar
import com.campushelper.app.ui.theme.Spacing

/**
 * Home screen - the student dashboard.
 *
 * State flow (unidirectional): HomeViewModel exposes StateFlow<HomeUiState>;
 * this composable only collects and renders it. All interactions are reported
 * upward through the `onOpen...` callbacks, which are wired to the existing
 * navigation destinations in CampusHelperNavHost.
 *
 * Honest data policy:
 *  - schedule/events/productivity render empty states because no data
 *    sources exist yet (verified, not assumed),
 *  - the weather card explicitly says the integration is not complete and
 *    shows no fabricated conditions.
 */
@Composable
fun HomeScreen(
    onOpenNotes: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSchedule: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    // Collect state in a lifecycle-aware way (pauses when not visible).
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // One subtle entrance for the whole dashboard (fade + tiny rise).
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entranceProgress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "homeEntrance",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Deferred read: the layer block runs outside recomposition.
                alpha = entranceProgress
                translationY = (1f - entranceProgress) * 12f
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.medium, vertical = Spacing.medium),
    ) {
        // --- Section A: header -------------------------------------------
        CampusTopBar(
            title = "Good day",
            subtitle = "Welcome to Campus Helper",
            logo = Icons.Filled.School,
        )

        // --- Section B: actual current date from the device clock --------
        Spacer(Modifier.height(Spacing.small))
        Text(
            text = uiState.dateLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // --- Section C: quick access -------------------------------------
        Spacer(Modifier.height(Spacing.large))
        CampusSectionHeader(title = "Quick access")
        Spacer(Modifier.height(Spacing.smallMedium))
        QuickAccessRow(
            onOpenNotes = onOpenNotes,
            onOpenExpenses = onOpenExpenses,
            onOpenCalendar = onOpenCalendar,
            onOpenSchedule = onOpenSchedule,
        )

        // --- Section D: today's schedule ---------------------------------
        Spacer(Modifier.height(Spacing.large))
        CampusSectionHeader(title = "Today's schedule")
        Spacer(Modifier.height(Spacing.smallMedium))
        CampusCard {
            // Inspection: no Course/Schedule table exists yet, so the state
            // can never say "has classes" - an empty state is the truth.
            if (!uiState.hasScheduleToday) {
                CampusEmptyState(
                    icon = Icons.Filled.School,
                    title = "No classes scheduled yet",
                    subtitle = "Your daily class schedule will appear here.",
                    actionLabel = "Go to Schedule",
                    onActionClick = onOpenSchedule,
                )
            }
        }

        // --- Section E: upcoming events ----------------------------------
        Spacer(Modifier.height(Spacing.large))
        CampusSectionHeader(title = "Upcoming events")
        Spacer(Modifier.height(Spacing.smallMedium))
        CampusCard {
            // Inspection: no EventDao/calendar storage exists yet.
            if (!uiState.hasSavedEvents) {
                CampusEmptyState(
                    icon = Icons.Outlined.Event,
                    title = "No upcoming events",
                    subtitle = "Quizzes, deadlines and academic activities you add will appear here.",
                    actionLabel = "Open Calendar",
                    onActionClick = onOpenCalendar,
                )
            }
        }

        // --- Section F: Cebu weather (API not integrated yet) -------------
        // Same header + card rhythm as sections D/E/G so the dashboard reads
        // as one set of titled sections, not a scatter of unrelated cards.
        Spacer(Modifier.height(Spacing.large))
        CampusSectionHeader(title = "Cebu weather")
        Spacer(Modifier.height(Spacing.smallMedium))
        CampusCard {
            // Inspection: no weather API/service exists yet. The empty state
            // is the truth (AGENTS.md: no fake weather) - no temperature,
            // condition icon or forecast is ever rendered.
            CampusEmptyState(
                icon = Icons.Outlined.CloudOff,
                title = "Not available yet",
                subtitle = "Live conditions and forecast for Cebu will appear here once the weather integration is implemented.",
            )
        }

        // --- Section G: productivity overview -----------------------------
        Spacer(Modifier.height(Spacing.large))
        CampusSectionHeader(title = "Productivity overview")
        Spacer(Modifier.height(Spacing.smallMedium))
        CampusCard {
            // Notes/expenses/classes have no repositories yet, so no metric
            // can be computed - showing a number would be fabricated data.
            CampusEmptyState(
                icon = Icons.Outlined.Insights,
                title = "Nothing to summarize yet",
                subtitle = "Notes, expenses, events and classes will appear here once you start tracking them.",
            )
        }

        // Breathing room above the bottom bar.
        Spacer(Modifier.height(Spacing.extraLarge))
    }
}

/** Quick access grid: four fixed destinations, equal columns, one tap each. */
@Composable
private fun QuickAccessRow(
    onOpenNotes: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSchedule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        // A small gap keeps adjacent tiles' tap targets and ripples separate
        // so the grid never behaves (or looks) like one merged control.
        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
    ) {
        QuickAccessItem(
            label = "Notes",
            icon = Icons.AutoMirrored.Filled.List,
            onClick = onOpenNotes,
            modifier = Modifier.weight(1f),
        )
        QuickAccessItem(
            label = "Expenses",
            icon = Icons.Filled.ShoppingCart,
            onClick = onOpenExpenses,
            modifier = Modifier.weight(1f),
        )
        QuickAccessItem(
            label = "Calendar",
            icon = Icons.Filled.DateRange,
            onClick = onOpenCalendar,
            modifier = Modifier.weight(1f),
        )
        QuickAccessItem(
            label = "Schedule",
            icon = Icons.Filled.Schedule,
            onClick = onOpenSchedule,
            modifier = Modifier.weight(1f),
        )
    }
}

/** One quick-access tile: icon in a consistent green container + label. */
@Composable
private fun QuickAccessItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // clip() keeps the ripple inside the rounded tile; the whole column is
    // one touch target (well above 48dp high and 1/4 of the screen wide).
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.small, horizontal = Spacing.extraSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CampusIconContainer(icon = icon)
        Spacer(Modifier.height(Spacing.small))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            // Ellipsis instead of clipping: the label stays readable at
            // unusual font scales or very narrow screens.
            overflow = TextOverflow.Ellipsis,
        )
    }
}
