package com.kareem.nexus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.core.model.*
import com.kareem.nexus.domain.intelligence.ContextIntelligence
import com.kareem.nexus.ui.design.*

@Composable
fun HomeScreen(contentPadding: PaddingValues, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Observation?>(null) }
    val active = state.actions.filter {
        it.state in setOf(ActionState.READY_FOR_APPROVAL, ActionState.APPROVED, ActionState.EXECUTING)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            NexusScreenHeader(
                title = "NEXUS",
                subtitle = "Your world, in context.",
                trailing = {
                    TextButton(
                        onClick = viewModel::refreshUnderstanding,
                        enabled = "refresh" !in state.pending,
                    ) { Text("Refresh") }
                },
            )
        }

        state.error?.let { message ->
            item { Text(message, color = NexusColors.Rose) }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NexusMetricTile(
                    value = state.observationCount.toString(),
                    label = "Observations",
                    hint = "+${state.brief.newSignals} today",
                    modifier = Modifier.weight(1f),
                )
                NexusMetricTile(
                    value = state.interestCount.toString(),
                    label = "Interests",
                    hint = state.brief.topTheme ?: "Learning",
                    accent = NexusColors.Mint,
                    modifier = Modifier.weight(1f),
                )
                NexusMetricTile(
                    value = state.readyActionCount.toString(),
                    label = "Ready",
                    hint = if (state.readyActionCount > 0) "Actions for you" else "All clear",
                    accent = NexusColors.Violet,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            NexusCard(accent = NexusColors.Cyan) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    NexusStatusPill("DAILY BRIEF", NexusColors.Cyan)
                    Text(
                        "${state.brief.newSignals} new signals",
                        color = NexusColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    state.brief.headline,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(state.brief.summary, color = NexusColors.TextSecondary)
            }
        }

        if (state.interests.isNotEmpty()) {
            item {
                NexusSectionHeader("NEXUS is learning")
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.interests.take(6), key = { it.id }) { interest ->
                        NexusStatusPill(interest.label, NexusColors.Cyan)
                    }
                }
            }
        }

        if (active.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "Action for you",
                    subtitle = "Only signals that survived NEXUS noise filtering.",
                )
            }
            items(active.take(6), key = { "action_${it.id}" }) { action ->
                val observation = state.observations.firstOrNull { action.id.endsWith(it.id) }
                ActionCard(
                    action = action,
                    viewModel = viewModel,
                    pending = state.pending,
                    observation = observation,
                    onEvidence = { selected = it },
                )
            }
        }

        val actionObservationIds = active
            .map { it.id.removePrefix("action_signal_").removePrefix("action_commitment_") }
            .toSet()
        val attention = state.attention.filterNot { it.id in actionObservationIds }
        if (attention.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "Needs attention",
                    subtitle = "Potential requests, commitments and failures.",
                )
            }
            items(attention.take(4), key = { "attention_${it.id}" }) { item ->
                val accent = when (item.level) {
                    AttentionLevel.URGENT -> NexusColors.Rose
                    AttentionLevel.HIGH -> NexusColors.Amber
                    else -> NexusColors.Cyan
                }
                NexusCard(
                    modifier = Modifier.clickable {
                        selected = state.observations.firstOrNull { it.id == item.id }
                    },
                    accent = accent,
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NexusStatusPill(item.level.name, accent)
                        Text(
                            item.kind.name.replace('_', ' '),
                            color = NexusColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ContentText(item.summary, maxLines = 3)
                    Text("Review original evidence", color = NexusColors.Cyan, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (state.situations.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "Connected context",
                    subtitle = "Separate signals NEXUS thinks belong together.",
                )
            }
            items(state.situations.take(3), key = { it.id }) { situation ->
                NexusCard(accent = NexusColors.Violet) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NexusStatusPill(situation.kind.name.replace('_', ' '), NexusColors.Violet)
                        Text(
                            "${situation.observationIds.size} signals",
                            style = MaterialTheme.typography.labelSmall,
                            color = NexusColors.TextMuted,
                        )
                    }
                    Text(situation.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(situation.summary, color = NexusColors.TextSecondary)
                }
            }
        }

        if (active.isEmpty() && attention.isEmpty()) {
            item {
                NexusCard {
                    Text("Nothing needs a decision right now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (state.observationCount == 0)
                            "Add a note or share something to NEXUS. Connect notifications in Settings when you're ready."
                        else
                            "NEXUS is still observing. It will stay quiet rather than manufacture generic suggestions.",
                        color = NexusColors.TextSecondary,
                    )
                }
            }
        }
    }

    selected?.let { ObservationDetails(it) { selected = null } }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    NexusSectionHeader(title = title, subtitle = subtitle)
}

@Composable
fun ActionCard(
    action: PreparedAction,
    viewModel: HomeViewModel,
    pending: Set<String>,
    observation: Observation?,
    onEvidence: (Observation) -> Unit,
) {
    val context = LocalContext.current
    val enabled = action.id !in pending
    val level = observation?.let { ContextIntelligence.buildAttention(listOf(it)).firstOrNull()?.level }
    val accent = when (level) {
        AttentionLevel.URGENT -> NexusColors.Rose
        AttentionLevel.HIGH -> NexusColors.Amber
        else -> NexusColors.Cyan
    }
    val stateLabel = when (action.state) {
        ActionState.READY_FOR_APPROVAL -> "READY FOR REVIEW"
        ActionState.APPROVED -> "APPROVED"
        ActionState.EXECUTING -> "IN PROGRESS"
        ActionState.DRAFT -> "LATER · 24 HOURS"
        ActionState.COMPLETED -> "COMPLETED"
        ActionState.REJECTED -> "DISMISSED"
        ActionState.FAILED -> "FAILED"
    }

    NexusCard(accent = accent) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            NexusStatusPill(stateLabel, if (action.state == ActionState.COMPLETED) NexusColors.Mint else accent)
            Text(timestamp(action.createdAt), style = MaterialTheme.typography.labelSmall, color = NexusColors.TextMuted)
        }
        Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ContentText(action.description, maxLines = 5)

        if (observation != null) {
            TextButton(onClick = { onEvidence(observation) }) { Text("Review original evidence") }
        } else {
            TextButton(onClick = { copyText(context, action.description) }) { Text("Copy details") }
        }

        when (action.state) {
            ActionState.READY_FOR_APPROVAL -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.approveAction(action.id) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    ) { Text("Approve") }
                    OutlinedButton(
                        onClick = { viewModel.deferAction(action.id) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, NexusColors.Border),
                    ) { Text("Later") }
                }
                TextButton(onClick = { viewModel.rejectAction(action.id) }, enabled = enabled) { Text("Dismiss") }
            }
            ActionState.APPROVED -> {
                Button(onClick = { viewModel.startAction(action.id) }, enabled = enabled) { Text("Start tracking") }
                TextButton(onClick = { viewModel.rejectAction(action.id) }, enabled = enabled) { Text("Dismiss") }
            }
            ActionState.EXECUTING -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.completeAction(action.id) }, enabled = enabled) { Text("Mark done") }
                    OutlinedButton(onClick = { viewModel.failAction(action.id) }, enabled = enabled) { Text("Failed") }
                }
            }
            else -> Unit
        }
    }
}
