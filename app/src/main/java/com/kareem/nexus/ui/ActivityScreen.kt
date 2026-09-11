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
    viewModel: HomeViewModel = hiltViewModel(),
    history: ActivityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity by history.uiState.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("Active") }
    var selected by remember { mutableStateOf<Observation?>(null) }

    val legacyActions = state.actions.filter {
        when (filter) {
            "Later" -> it.state == ActionState.DRAFT
            "Finished" -> it.state in setOf(ActionState.COMPLETED, ActionState.REJECTED, ActionState.FAILED)
            else -> false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            NexusScreenHeader(
                title = "Activity",
                subtitle = "What NEXUS surfaced, what you decided, and what happened next.",
            )
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Active", "Later", "Finished", "Timeline")) { label ->
                    FilterChip(
                        selected = filter == label,
                        onClick = { filter = label },
                        label = { Text(label) },
                    )
                }
            }
        }

        state.error?.let { message -> item { Text(message, color = NexusColors.Rose) } }

        when (filter) {
            "Active" -> {
                item {
                    NexusSectionHeader(
                        title = "Active",
                        subtitle = "Only unresolved signals currently worth your attention.",
                    )
                }
                if (state.topOfMind.isEmpty()) {
                    item {
                        NexusCard {
                            Text("Nothing active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("NEXUS has no unresolved high-value signal right now.", color = NexusColors.TextSecondary)
                        }
                    }
                }
                items(state.topOfMind, key = { it.id }) { item ->
                    val observation = state.observations.firstOrNull { it.id == item.observationId }
                    val actionId = "action_signal_${item.observationId}"
                    val accent = when {
                        item.priority >= .88 -> NexusColors.Rose
                        item.priority >= .75 -> NexusColors.Amber
                        else -> NexusColors.Cyan
                    }
                    NexusCard(accent = accent) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            NexusStatusPill(item.kind.name.replace('_', ' '), accent)
                            Text(timestamp(item.createdAt), color = NexusColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        ContentText(item.summary, maxLines = 4)
                        if (observation != null) {
                            TextButton(onClick = { selected = observation }) { Text("View evidence") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (item.actions.any { it.kind == NexusActionKind.REMIND }) {
                                OutlinedButton(
                                    onClick = { viewModel.deferAction(actionId) },
                                    enabled = actionId !in state.pending,
                                    modifier = Modifier.weight(1f),
                                ) { Text("Later") }
                            }
                            if (item.actions.any { it.kind == NexusActionKind.MARK_RESOLVED }) {
                                Button(
                                    onClick = { viewModel.resolveAction(actionId) },
                                    enabled = actionId !in state.pending,
                                    modifier = Modifier.weight(1f),
                                ) { Text("Done") }
                            }
                        }
                    }
                }
            }

            "Later", "Finished" -> {
                item {
                    NexusSectionHeader(
                        title = filter,
                        subtitle = if (filter == "Later")
                            "Signals you asked NEXUS to bring back later."
                        else
                            "Resolved, dismissed and failed historical actions.",
                    )
                }
                if (legacyActions.isEmpty()) {
                    item {
                        NexusCard {
                            Text("No ${filter.lowercase()} items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                items(legacyActions, key = { it.id }) { action ->
                    val observation = state.observations.firstOrNull { action.id.endsWith(it.id) }
                    val latestSignal = activity.events
                        .asSequence()
                        .filter { it.actionId == action.id }
                        .maxByOrNull { it.createdAt }
                        ?.signal
                    val outcomeLabel = when (latestSignal) {
                        FeedbackSignal.RESOLVED -> "RESOLVED"
                        FeedbackSignal.REJECTED -> "DISMISSED"
                        FeedbackSignal.COMPLETED -> "COMPLETED"
                        FeedbackSignal.FAILED -> "FAILED"
                        else -> action.state.name.replace('_', ' ')
                    }
                    val outcomeAccent = when (latestSignal) {
                        FeedbackSignal.RESOLVED, FeedbackSignal.COMPLETED -> NexusColors.Mint
                        FeedbackSignal.REJECTED, FeedbackSignal.FAILED -> NexusColors.Rose
                        else -> if (filter == "Finished") NexusColors.Mint else NexusColors.Amber
                    }
                    NexusCard(accent = outcomeAccent) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            NexusStatusPill(outcomeLabel, outcomeAccent)
                            Text(timestamp(action.createdAt), color = NexusColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        ContentText(action.description, maxLines = 4)
                        if (observation != null) {
                            TextButton(onClick = { selected = observation }) { Text("View evidence") }
                        }
                    }
                }
            }

            "Timeline" -> {
                item {
                    NexusSectionHeader(
                        title = "Decision timeline",
                        subtitle = "A causal history of what NEXUS suggested and what happened next.",
                    )
                }
                if (activity.events.isEmpty()) {
                    item {
                        NexusCard {
                            Text("No decisions yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Actions you defer, finish or dismiss will appear here.", color = NexusColors.TextSecondary)
                        }
                    }
                }
                items(activity.events, key = { it.id }) { event ->
                    val action = state.actions.firstOrNull { it.id == event.actionId }
                    TimelineEvent(
                        signal = event.signal,
                        title = action?.title ?: "NEXUS action",
                        detail = action?.description.orEmpty(),
                        createdAt = event.createdAt,
                    )
                }
            }
        }
    }

    selected?.let { ObservationDetails(it) { selected = null } }
}

@Composable
private fun TimelineEvent(
    signal: FeedbackSignal,
    title: String,
    detail: String,
    createdAt: Long,
) {
    val accent = when (signal) {
        FeedbackSignal.COMPLETED, FeedbackSignal.RESOLVED -> NexusColors.Mint
        FeedbackSignal.FAILED, FeedbackSignal.REJECTED -> NexusColors.Rose
        FeedbackSignal.DEFERRED -> NexusColors.Amber
        FeedbackSignal.APPROVED, FeedbackSignal.STARTED, FeedbackSignal.ACTED -> NexusColors.Cyan
        FeedbackSignal.SUGGESTED, FeedbackSignal.RESURFACED -> NexusColors.Violet
        else -> NexusColors.TextSecondary
    }
    val label = when (signal) {
        FeedbackSignal.SUGGESTED -> "Suggestion created"
        FeedbackSignal.APPROVED -> "Action approved"
        FeedbackSignal.DEFERRED -> "Saved for later"
        FeedbackSignal.RESURFACED -> "Suggestion resurfaced"
        FeedbackSignal.REJECTED -> "Dismissed"
        FeedbackSignal.RESOLVED -> "Marked done"
        FeedbackSignal.STARTED -> "Action in progress"
        FeedbackSignal.COMPLETED -> "Action completed"
        FeedbackSignal.FAILED -> "Action failed"
        FeedbackSignal.ACTED -> "Action taken"
        FeedbackSignal.OPENED -> "Opened"
        FeedbackSignal.SAVED -> "Saved"
        FeedbackSignal.DISMISSED -> "Dismissed"
        FeedbackSignal.SHARED -> "Shared"
        FeedbackSignal.DWELL -> "Viewed"
    }

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
            Box(Modifier.width(2.dp).height(86.dp).background(NexusColors.BorderSoft))
        }
        Surface(
            modifier = Modifier.weight(1f),
            color = NexusColors.Surface,
            shape = RoundedCornerShape(NexusRadius.Medium),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
