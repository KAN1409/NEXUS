package com.kareem.nexus.ui

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
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
import com.kareem.nexus.ui.design.*

@Composable
fun HomeScreen(contentPadding: PaddingValues, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Observation?>(null) }

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

        state.error?.let { message -> item { Text(message, color = NexusColors.Rose) } }

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
                    value = state.topOfMind.size.toString(),
                    label = "Top of mind",
                    hint = if (state.topOfMind.isEmpty()) "All clear" else "Worth your attention",
                    accent = NexusColors.Violet,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            val headline = when (state.topOfMind.size) {
                0 -> "Nothing important is demanding attention"
                1 -> "1 thing is worth your attention"
                else -> "${state.topOfMind.size} things are worth your attention"
            }
            NexusCard(accent = if (state.topOfMind.isEmpty()) NexusColors.Mint else NexusColors.Cyan) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    NexusStatusPill("DAILY BRIEF", if (state.topOfMind.isEmpty()) NexusColors.Mint else NexusColors.Cyan)
                    Text(
                        "${state.brief.newSignals} new signals",
                        color = NexusColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (state.topOfMind.isEmpty())
                        "NEXUS filtered the noise and found nothing that needs a decision right now."
                    else
                        "Only unresolved requests, payments, commitments, failures and follow-ups that survived noise filtering are shown here.",
                    color = NexusColors.TextSecondary,
                )
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

        if (state.topOfMind.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "Top of mind",
                    subtitle = "What changed, what needs you, and the next useful step.",
                )
            }
            items(state.topOfMind, key = { it.id }) { item ->
                val observation = state.observations.firstOrNull { it.id == item.observationId }
                TopOfMindCard(
                    item = item,
                    observation = observation,
                    viewModel = viewModel,
                    pending = state.pending,
                    onEvidence = { selected = it },
                )
            }
        }

        if (state.contextSituations.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "Connected situations",
                    subtitle = "Related signals collapsed into real-world threads.",
                )
            }
            items(state.contextSituations.take(4), key = { it.id }) { situation ->
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
                    if (situation.facts.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(situation.facts.take(4), key = { "${it.kind}:${it.normalizedValue}" }) { fact ->
                                NexusStatusPill(fact.value, NexusColors.Mint)
                            }
                        }
                    }
                }
            }
        }

        if (state.topOfMind.isEmpty() && state.contextSituations.isEmpty()) {
            item {
                NexusCard {
                    Text("NEXUS is quiet on purpose", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (state.observationCount == 0)
                            "Add a note, image or link, or connect notifications in Settings."
                        else
                            "Your recent context is being remembered, but nothing currently looks important enough to interrupt you.",
                        color = NexusColors.TextSecondary,
                    )
                }
            }
        }
    }

    selected?.let { ObservationDetails(it) { selected = null } }
}

@Composable
private fun TopOfMindCard(
    item: TopOfMindItem,
    observation: Observation?,
    viewModel: HomeViewModel,
    pending: Set<String>,
    onEvidence: (Observation) -> Unit,
) {
    val context = LocalContext.current
    val actionId = "action_signal_${item.observationId}"
    val enabled = actionId !in pending
    val accent = when {
        item.priority >= .88 -> NexusColors.Rose
        item.priority >= .75 -> NexusColors.Amber
        item.kind == SignalKind.REQUEST -> NexusColors.Cyan
        else -> NexusColors.Violet
    }
    val label = when {
        item.priority >= .88 -> "HIGH PRIORITY"
        item.kind == SignalKind.REQUEST -> "NEEDS REPLY"
        item.kind == SignalKind.PAYMENT -> "PAYMENT"
        item.kind == SignalKind.APPOINTMENT -> "UPCOMING"
        item.kind == SignalKind.FAILURE -> "NEEDS REVIEW"
        item.kind == SignalKind.DELIVERY -> "ORDER"
        else -> "FOLLOW UP"
    }

    NexusCard(accent = accent) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            NexusStatusPill(label, accent)
            Text(timestamp(item.createdAt), style = MaterialTheme.typography.labelSmall, color = NexusColors.TextMuted)
        }
        Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        ContentText(item.summary, maxLines = 5)

        if (observation != null) {
            TextButton(onClick = { onEvidence(observation) }) { Text("View evidence") }
        }

        val primary = item.actions.firstOrNull { it.kind !in setOf(NexusActionKind.REMIND, NexusActionKind.MARK_RESOLVED) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (primary != null) {
                Button(
                    onClick = {
                        executeContextAction(
                            context = context,
                            action = primary,
                            item = item,
                            observation = observation,
                        )
                    },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(primaryLabel(primary, observation))
                }
            }
            if (item.actions.any { it.kind == NexusActionKind.REMIND }) {
                OutlinedButton(
                    onClick = { viewModel.deferAction(actionId) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, NexusColors.Border),
                ) { Text("Remind tomorrow") }
            }
        }

        if (item.actions.any { it.kind == NexusActionKind.MARK_RESOLVED }) {
            TextButton(onClick = { viewModel.rejectAction(actionId) }, enabled = enabled) {
                Text("Done")
            }
        }
    }
}

private fun primaryLabel(action: ContextAction, observation: Observation?): String = when (action.kind) {
    NexusActionKind.OPEN_SOURCE, NexusActionKind.REPLY -> {
        val source = observation?.source?.substringAfterLast('.')?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }
        if (source.isNullOrBlank() || source.equals("android", true)) action.label else "Open $source"
    }
    NexusActionKind.ADD_TO_CALENDAR -> "Add to calendar"
    NexusActionKind.TRACK -> "Track"
    NexusActionKind.RETRY -> "Open source"
    NexusActionKind.NAVIGATE -> "Navigate"
    NexusActionKind.CALL -> "Call"
    NexusActionKind.COPY -> "Copy"
    else -> action.label
}

private fun executeContextAction(
    context: Context,
    action: ContextAction,
    item: TopOfMindItem,
    observation: Observation?,
) {
    when (action.kind) {
        NexusActionKind.OPEN_SOURCE,
        NexusActionKind.REPLY,
        NexusActionKind.TRACK,
        NexusActionKind.RETRY -> {
            val packageName = observation?.source ?: action.payload
            val launchIntent = packageName?.let(context.packageManager::getLaunchIntentForPackage)
            if (launchIntent != null) {
                context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                Toast.makeText(context, "Source app is not directly launchable", Toast.LENGTH_SHORT).show()
            }
        }
        NexusActionKind.ADD_TO_CALENDAR -> {
            runCatching {
                val intent = Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, item.title)
                    .putExtra(CalendarContract.Events.DESCRIPTION, item.summary)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }.onFailure {
                Toast.makeText(context, "No calendar app available", Toast.LENGTH_SHORT).show()
            }
        }
        NexusActionKind.COPY -> copyText(context, item.summary)
        NexusActionKind.REMIND,
        NexusActionKind.MARK_RESOLVED,
        NexusActionKind.NAVIGATE,
        NexusActionKind.CALL -> Unit
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    NexusSectionHeader(title = title, subtitle = subtitle)
}
