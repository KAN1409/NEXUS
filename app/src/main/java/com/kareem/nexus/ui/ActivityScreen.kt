package com.kareem.nexus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.core.model.*
import com.kareem.nexus.ui.design.*

@Composable
fun ActivityScreen(
    contentPadding: PaddingValues,
    history: ActivityViewModel = hiltViewModel(),
) {
    val state by history.uiState.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("Timeline") }
    var selected by remember { mutableStateOf<Observation?>(null) }

    val loops = remember(state.openLoops, filter) {
        when (filter) {
            "Active" -> state.openLoops.filter { it.state in setOf(OpenLoopState.OPEN, OpenLoopState.WAITING) }
            "Later" -> state.openLoops.filter { it.state == OpenLoopState.SNOOZED }
            "Finished" -> state.openLoops.filter { it.state in setOf(OpenLoopState.RESOLVED, OpenLoopState.DISMISSED) }
            else -> emptyList()
        }.sortedByDescending { it.updatedAt }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 108.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            NexusScreenHeader(
                title = "Activity",
                subtitle = "What NEXUS noticed, what you did, and what happened next.",
            )
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Timeline", "Active", "Later", "Finished")) { label ->
                    FilterChip(
                        selected = filter == label,
                        onClick = { filter = label },
                        label = { Text(label) },
                    )
                }
            }
        }

        if (filter == "Timeline") {
            item {
                NexusSectionHeader(
                    title = "Outcome timeline",
                    subtitle = "Real Android actions first, followed by NEXUS decisions and state changes.",
                )
            }

            if (state.executions.isEmpty() && state.events.isEmpty()) {
                item {
                    NexusCard(accent = NexusColors.Mint) {
                        Text("Nothing has happened yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Open an app, create a calendar event, schedule a reminder or resolve an open loop and the outcome will appear here.",
                            color = NexusColors.TextSecondary,
                        )
                    }
                }
            }

            items(state.executions, key = { "exec_${it.id}" }) { execution ->
                val accent = when (execution.state) {
                    ExecutionState.SUCCEEDED -> NexusColors.Mint
                    ExecutionState.FAILED -> NexusColors.Rose
                    ExecutionState.STARTED -> NexusColors.Cyan
                }
                TimelineCard(
                    accent = accent,
                    label = execution.state.name,
                    title = execution.label,
                    detail = execution.message ?: execution.actionKind.name.replace('_', ' '),
                    createdAt = execution.createdAt,
                )
            }

            items(state.events.take(100), key = { "event_${it.id}" }) { event ->
                val action = state.actions.firstOrNull { it.id == event.actionId }
                val accent = when (event.signal) {
                    FeedbackSignal.RESOLVED, FeedbackSignal.COMPLETED -> NexusColors.Mint
                    FeedbackSignal.FAILED, FeedbackSignal.REJECTED, FeedbackSignal.DISMISSED -> NexusColors.Rose
                    FeedbackSignal.DEFERRED, FeedbackSignal.RESURFACED -> NexusColors.Amber
                    else -> NexusColors.Violet
                }
                TimelineCard(
                    accent = accent,
                    label = event.signal.name.replace('_', ' '),
                    title = action?.title ?: "NEXUS decision",
                    detail = action?.description.orEmpty(),
                    createdAt = event.createdAt,
                )
            }
        } else {
            item {
                NexusSectionHeader(
                    title = filter,
                    subtitle = when (filter) {
                        "Active" -> "Open loops NEXUS is still tracking."
                        "Later" -> "Snoozed loops that will return when their reminder is due."
                        else -> "Resolved or dismissed loops kept as searchable history."
                    },
                )
            }
            if (loops.isEmpty()) {
                item {
                    NexusCard {
                        Text("No ${filter.lowercase()} loops", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(loops, key = { it.id }) { loop ->
                val accent = when (loop.state) {
                    OpenLoopState.OPEN -> NexusColors.Cyan
                    OpenLoopState.WAITING -> NexusColors.Amber
                    OpenLoopState.SNOOZED -> NexusColors.Violet
                    OpenLoopState.RESOLVED -> NexusColors.Mint
                    OpenLoopState.DISMISSED -> NexusColors.TextMuted
                }
                NexusCard(accent = accent) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NexusStatusPill(loop.state.name, accent)
                        Text(timestamp(loop.updatedAt), color = NexusColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(loop.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ContentText(loop.detail, maxLines = 4)
                    TextButton(onClick = {
                        selected = state.observations.firstOrNull { it.id == loop.observationId }
                    }) { Text("View evidence") }
                }
            }
        }
    }

    selected?.let { ObservationDetails(it) { selected = null } }
}

@Composable
private fun TimelineCard(
    accent: androidx.compose.ui.graphics.Color,
    label: String,
    title: String,
    detail: String,
    createdAt: Long,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.width(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(10.dp).background(accent, CircleShape))
            Box(Modifier.width(2.dp).height(78.dp).background(NexusColors.BorderSoft))
        }
        Surface(
            modifier = Modifier.weight(1f),
            color = NexusColors.Surface,
            shape = RoundedCornerShape(NexusRadius.Medium),
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    NexusStatusPill(label.uppercase(), accent)
                    Text(timestamp(createdAt), color = NexusColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (detail.isNotBlank()) ContentText(detail, maxLines = 3)
            }
        }
    }
}
