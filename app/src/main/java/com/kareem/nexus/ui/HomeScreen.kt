package com.kareem.nexus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.kareem.nexus.ui.design.NexusColors

@Composable
fun HomeScreen(contentPadding: PaddingValues, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Observation?>(null) }
    val active = state.actions.filter { it.state in setOf(ActionState.READY_FOR_APPROVAL, ActionState.APPROVED, ActionState.EXECUTING) }
    LazyColumn(Modifier.fillMaxSize().padding(contentPadding), contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("NEXUS", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Text("Your day, connected.", color = NexusColors.TextSecondary)
                }
                TextButton(onClick = { viewModel.refreshUnderstanding() }, enabled = "refresh" !in state.pending) { Text("Refresh") }
            }
        }
        state.error?.let { message -> item { Text(message, color = NexusColors.Rose) } }
        item {
            Surface(color = NexusColors.SurfaceRaised, shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, NexusColors.Cyan.copy(alpha = .25f))) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("DAILY BRIEF", style = MaterialTheme.typography.labelMedium, color = NexusColors.Cyan)
                    Text(state.brief.headline, style = MaterialTheme.typography.headlineSmall)
                    Text(state.brief.summary, color = NexusColors.TextSecondary)
                    HorizontalDivider(color = NexusColors.Border)
                    Text("${state.brief.newSignals} new signals · ${state.readyActionCount} ready to review", style = MaterialTheme.typography.labelLarge, color = NexusColors.Cyan)
                }
            }
        }
        if (active.isNotEmpty()) {
            item { SectionTitle("Next actions", "Review the evidence, then decide.") }
            items(active, key = { "action_${it.id}" }) { action ->
                ActionCard(action, viewModel, state.pending, state.observations.firstOrNull { action.id.endsWith(it.id) }) { selected = it }
            }
        }
        val actionObservationIds = active.map { it.id.removePrefix("action_signal_").removePrefix("action_commitment_") }.toSet()
        val attention = state.attention.filterNot { it.id in actionObservationIds }
        if (attention.isNotEmpty()) {
            item { SectionTitle("Needs attention", "Possible requests and upcoming commitments.") }
            items(attention, key = { "attention_${it.id}" }) { item ->
                Surface(Modifier.fillMaxWidth().clickable { selected = state.observations.firstOrNull { it.id == item.id } }, color = NexusColors.Surface, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.kind.name.replace('_', ' '), color = if (item.level == AttentionLevel.URGENT) NexusColors.Amber else NexusColors.Cyan, style = MaterialTheme.typography.labelMedium)
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        ContentText(item.summary, maxLines = 3)
                        Text("Review source", color = NexusColors.Cyan, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if (state.situations.isNotEmpty()) {
            item { SectionTitle("Connected context", "Topic connections to explore in Discover.") }
            items(state.situations.take(3), key = { it.id }) { situation ->
                Surface(color = NexusColors.Surface, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(situation.title, style = MaterialTheme.typography.titleMedium)
                        Text(situation.summary, color = NexusColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (active.isEmpty() && attention.isEmpty()) item {
            Text(if (state.observationCount == 0) "Start with a note or share something to NEXUS. Connect notifications in Settings when you're ready." else "Nothing else needs a decision right now. Your saved context is in Memory.", color = NexusColors.TextSecondary)
        }
    }
    selected?.let { ObservationDetails(it) { selected = null } }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NexusColors.TextSecondary)
    }
}

@Composable
fun ActionCard(action: PreparedAction, viewModel: HomeViewModel, pending: Set<String>, observation: Observation?, onEvidence: (Observation) -> Unit) {
    val context = LocalContext.current
    val enabled = action.id !in pending
    Surface(color = NexusColors.Surface, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(when (action.state) {
                ActionState.READY_FOR_APPROVAL -> "READY FOR REVIEW"
                ActionState.EXECUTING -> "IN PROGRESS"
                ActionState.DRAFT -> "LATER · 24 HOURS"
                else -> action.state.name
            }, style = MaterialTheme.typography.labelMedium, color = if (action.state == ActionState.COMPLETED) NexusColors.Mint else NexusColors.Cyan)
            Text(action.title, style = MaterialTheme.typography.titleMedium)
            ContentText(action.description, maxLines = 5)
            Text(timestamp(action.createdAt), style = MaterialTheme.typography.labelSmall, color = NexusColors.TextSecondary)
            if (observation != null) TextButton(onClick = { onEvidence(observation) }) { Text("Review original evidence") }
            else TextButton(onClick = { copyText(context, action.description) }) { Text("Copy details") }
            when (action.state) {
                ActionState.READY_FOR_APPROVAL -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.approveAction(action.id) }, enabled = enabled, modifier = Modifier.weight(1f)) { Text("Approve") }
                        OutlinedButton(onClick = { viewModel.deferAction(action.id) }, enabled = enabled, modifier = Modifier.weight(1f)) { Text("Later") }
                    }
                    TextButton(onClick = { viewModel.rejectAction(action.id) }, enabled = enabled) { Text("Dismiss") }
                }
                ActionState.APPROVED -> {
                    Button(onClick = { viewModel.startAction(action.id) }, enabled = enabled) { Text("Start tracking") }
                    TextButton(onClick = { viewModel.rejectAction(action.id) }, enabled = enabled) { Text("Dismiss") }
                }
                ActionState.EXECUTING -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.completeAction(action.id) }, enabled = enabled) { Text("Mark done") }
                    OutlinedButton(onClick = { viewModel.failAction(action.id) }, enabled = enabled) { Text("Failed") }
                }
                else -> Unit
            }
        }
    }
}
