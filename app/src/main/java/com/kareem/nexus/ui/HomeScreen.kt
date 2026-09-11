package com.kareem.nexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.core.model.*
import com.kareem.nexus.ui.design.*

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionable = state.actions.filter {
        it.state in setOf(ActionState.READY_FOR_APPROVAL, ActionState.APPROVED, ActionState.EXECUTING)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = NexusSpacing.Xl, vertical = NexusSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(NexusSpacing.Lg),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(NexusSpacing.Xs)) {
                Text("NEXUS", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text("Your world, in context.", style = MaterialTheme.typography.bodyLarge, color = NexusColors.TextSecondary)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NexusColors.SurfaceRaised),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(NexusRadius.Medium),
            ) {
                Column(
                    Modifier.padding(NexusSpacing.Lg),
                    verticalArrangement = Arrangement.spacedBy(NexusSpacing.Sm),
                ) {
                    Text("TODAY", style = MaterialTheme.typography.labelMedium, color = NexusColors.Cyan)
                    Text(state.brief.headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(state.brief.summary, color = NexusColors.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(NexusSpacing.Md)) {
                        BriefMetric("New signals", state.brief.newSignals.toString(), Modifier.weight(1f))
                        BriefMetric("Needs attention", state.brief.attentionCount.toString(), Modifier.weight(1f))
                    }
                }
            }
        }

        if (state.attention.isNotEmpty()) {
            item {
                SectionHeader("Needs Attention", "Things NEXUS thinks may need a decision or follow-up.")
            }
            items(state.attention.take(4), key = { "attention_" + it.id }) { item ->
                AttentionCard(item)
            }
        }

        if (actionable.isNotEmpty()) {
            item {
                SectionHeader("Ready Actions", "Prepared actions waiting for your decision.")
            }
            items(actionable.take(4), key = { "action_" + it.id }) { action ->
                ActionCard(action, viewModel)
            }
        }

        if (state.situations.isNotEmpty()) {
            item {
                SectionHeader("Situations", "Separate signals NEXUS connected into the same real-world context.")
            }
            items(state.situations.take(4), key = { it.id }) { situation ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NexusColors.Surface),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(NexusRadius.Medium),
                ) {
                    Column(
                        Modifier.padding(NexusSpacing.Lg),
                        verticalArrangement = Arrangement.spacedBy(NexusSpacing.Sm),
                    ) {
                        Text(situation.kind.name.replace('_', ' '), color = NexusColors.Violet, style = MaterialTheme.typography.labelMedium)
                        Text(situation.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(situation.summary, color = NexusColors.TextSecondary)
                        Text(
                            situation.observationIds.size.toString() + " connected signals",
                            style = MaterialTheme.typography.bodySmall,
                            color = NexusColors.Cyan,
                        )
                    }
                }
            }
        }

        item {
            SectionHeader("What NEXUS Knows", "A compact view of the context model behind the suggestions.")
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(NexusSpacing.Md)) {
                StatCard("Observations", state.observationCount.toString(), Modifier.weight(1f))
                StatCard("Interests", state.interestCount.toString(), Modifier.weight(1f))
                StatCard("Ready", state.readyActionCount.toString(), Modifier.weight(1f))
            }
        }

        if (state.insights.isNotEmpty()) {
            items(state.insights.take(3), key = { it.id }) { insight ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NexusColors.Surface),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(NexusRadius.Medium),
                ) {
                    Column(
                        Modifier.padding(NexusSpacing.Lg),
                        verticalArrangement = Arrangement.spacedBy(NexusSpacing.Sm),
                    ) {
                        Text("INSIGHT", color = NexusColors.Violet, style = MaterialTheme.typography.labelMedium)
                        Text(insight.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(insight.summary, color = NexusColors.TextSecondary)
                        Text(insight.evidence, color = NexusColors.Cyan, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (state.attention.isEmpty() && actionable.isEmpty() && state.situations.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NexusColors.Surface),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(NexusRadius.Medium),
                ) {
                    Column(Modifier.padding(NexusSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NexusSpacing.Sm)) {
                        Text("Nothing noisy here", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "NEXUS is watching your local context and will surface something only when it has a useful reason.",
                            color = NexusColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(subtitle, color = NexusColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AttentionCard(item: AttentionItem) {
    val accent = when (item.level) {
        AttentionLevel.URGENT, AttentionLevel.HIGH -> NexusColors.Cyan
        AttentionLevel.MEDIUM -> NexusColors.Violet
        AttentionLevel.LOW -> NexusColors.TextSecondary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NexusColors.SurfaceRaised),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(NexusRadius.Medium),
    ) {
        Column(Modifier.padding(NexusSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NexusSpacing.Sm)) {
            Text(
                item.level.name + " · " + item.kind.name.replace('_', ' '),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
            )
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(item.summary, color = NexusColors.TextSecondary)
            item.source?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = NexusColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ActionCard(action: PreparedAction, viewModel: HomeViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NexusColors.SurfaceRaised),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(NexusRadius.Medium),
    ) {
        Column(Modifier.padding(NexusSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NexusSpacing.Sm)) {
            Text(
                when (action.state) {
                    ActionState.READY_FOR_APPROVAL -> "WAITING FOR YOU"
                    ActionState.APPROVED -> "APPROVED"
                    ActionState.EXECUTING -> "IN PROGRESS"
                    else -> action.state.name.replace('_', ' ')
                },
                style = MaterialTheme.typography.labelMedium,
                color = NexusColors.Cyan,
            )
            Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(action.description, color = NexusColors.TextSecondary)

            when (action.state) {
                ActionState.READY_FOR_APPROVAL -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NexusSpacing.Sm)) {
                        Button(onClick = { viewModel.approveAction(action.id) }, modifier = Modifier.weight(1f)) { Text("Approve") }
                        OutlinedButton(onClick = { viewModel.deferAction(action.id) }, modifier = Modifier.weight(1f)) { Text("Later") }
                    }
                    TextButton(onClick = { viewModel.rejectAction(action.id) }) { Text("Dismiss") }
                }
                ActionState.APPROVED -> Button(
                    onClick = { viewModel.startAction(action.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Start") }
                ActionState.EXECUTING -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NexusSpacing.Sm)) {
                    Button(onClick = { viewModel.completeAction(action.id) }, modifier = Modifier.weight(1f)) { Text("Complete") }
                    OutlinedButton(onClick = { viewModel.failAction(action.id) }, modifier = Modifier.weight(1f)) { Text("Failed") }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NexusColors.Surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(NexusRadius.Medium),
    ) {
        Column(
            Modifier.padding(horizontal = NexusSpacing.Md, vertical = NexusSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(NexusSpacing.Xs),
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = NexusColors.TextSecondary)
        }
    }
}

@Composable
private fun BriefMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = NexusColors.Surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(NexusRadius.Medium),
    ) {
        Column(Modifier.padding(NexusSpacing.Md), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = NexusColors.TextSecondary)
        }
    }
}
