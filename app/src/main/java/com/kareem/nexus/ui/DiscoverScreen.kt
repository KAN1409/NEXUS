package com.kareem.nexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.core.model.Observation
import com.kareem.nexus.core.model.SignalKind
import com.kareem.nexus.ui.design.*

@Composable
fun DiscoverScreen(contentPadding: PaddingValues, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Observation?>(null) }
    var filter by rememberSaveable { mutableStateOf("All") }
    val filters = listOf("All", "Situations", "Changes", "Themes")

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            NexusScreenHeader(
                title = "Discover",
                subtitle = "The people, projects and situations NEXUS connected from your evidence.",
            )
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(item) },
                    )
                }
            }
        }

        if ((filter == "All" || filter == "Situations") && state.contextSituations.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "Connected situations",
                    subtitle = "Several signals collapsed into one thread so you can understand the situation, not the notifications.",
                )
            }
            items(state.contextSituations, key = { it.id }) { situation ->
                NexusCard(accent = if (situation.priority >= .8) NexusColors.Amber else NexusColors.Cyan) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NexusStatusPill(
                            if (situation.priority >= .8) "NEEDS ATTENTION" else "CONNECTED",
                            if (situation.priority >= .8) NexusColors.Amber else NexusColors.Cyan,
                        )
                        Text(
                            "${situation.observationIds.size} signals",
                            style = MaterialTheme.typography.labelSmall,
                            color = NexusColors.TextMuted,
                        )
                    }
                    Text(situation.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(situation.summary, color = NexusColors.TextSecondary)

                    if (situation.facts.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(situation.facts.take(5), key = { "${it.kind}:${it.normalizedValue}" }) { fact ->
                                NexusStatusPill(fact.value, NexusColors.Mint)
                            }
                        }
                    }

                    TextButton(onClick = { expanded = if (expanded == situation.id) null else situation.id }) {
                        Text(if (expanded == situation.id) "Hide evidence" else "See source evidence")
                    }
                    if (expanded == situation.id) {
                        state.observations
                            .filter { it.id in situation.observationIds }
                            .sortedByDescending { it.createdAt }
                            .forEach { observation ->
                                HorizontalDivider(color = NexusColors.BorderSoft)
                                TextButton(
                                    onClick = { selected = observation },
                                    contentPadding = PaddingValues(vertical = 4.dp),
                                ) {
                                    Column(Modifier.fillMaxWidth()) {
                                        ContentText(observation.rawText, maxLines = 3)
                                        Text(
                                            timestamp(observation.createdAt),
                                            color = NexusColors.TextMuted,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                    }
                }
            }
        }

        if ((filter == "All" || filter == "Changes") && state.topOfMind.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "What changed",
                    subtitle = "Current evidence that may change what you do next.",
                )
            }
            items(state.topOfMind.take(5), key = { "change_${it.id}" }) { item ->
                val accent = when (item.kind) {
                    SignalKind.PAYMENT, SignalKind.FAILURE -> NexusColors.Amber
                    SignalKind.REQUEST -> NexusColors.Cyan
                    SignalKind.APPOINTMENT -> NexusColors.Violet
                    else -> NexusColors.Mint
                }
                NexusCard(accent = accent) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NexusStatusPill(item.kind.name.replace('_', ' '), accent)
                        Text(
                            "${(item.confidence * 100).toInt()}% confidence",
                            color = NexusColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ContentText(item.summary, maxLines = 4)
                    TextButton(onClick = {
                        selected = state.observations.firstOrNull { it.id == item.observationId }
                    }) { Text("View evidence") }
                }
            }
        }

        if ((filter == "All" || filter == "Themes") && state.interests.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "Background themes",
                    subtitle = "Longer-term context. These are supporting signals, not tasks.",
                )
            }
            items(state.interests.take(6), key = { it.id }) { interest ->
                NexusCard {
                    val prominence = interest.affinity.toFloat().coerceIn(0f, 1f)
                    val accent = if (prominence >= .7f) NexusColors.Violet else NexusColors.Mint
                    NexusProgressRow(
                        title = interest.label,
                        progress = prominence,
                        detail = "${(prominence * 100).toInt()}% prominence",
                        accent = accent,
                    )
                }
            }
        }

        if (state.contextSituations.isEmpty() && state.topOfMind.isEmpty() && state.interests.isEmpty()) {
            item {
                NexusCard {
                    Text("No meaningful connection yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "NEXUS will wait for enough evidence instead of inventing patterns from one notification.",
                        color = NexusColors.TextSecondary,
                    )
                }
            }
        }
    }

    selected?.let { ObservationDetails(it) { selected = null } }
}
