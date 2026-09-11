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
import com.kareem.nexus.ui.design.*

@Composable
fun DiscoverScreen(contentPadding: PaddingValues, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Observation?>(null) }
    var filter by rememberSaveable { mutableStateOf("All") }
    val filters = listOf("All", "Situations", "Themes", "Insights")

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            NexusScreenHeader(
                title = "Discover",
                subtitle = "Patterns, threads and connections grounded in your context.",
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

        if ((filter == "All" || filter == "Situations") && state.situations.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "Connected situations",
                    subtitle = "Multiple observations that appear to belong to the same real-world thread.",
                )
            }
            items(state.situations, key = { it.id }) { situation ->
                NexusCard(accent = NexusColors.Cyan) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NexusStatusPill("CONNECTED", NexusColors.Cyan)
                        Text(
                            "${situation.observationIds.size} signals",
                            style = MaterialTheme.typography.labelSmall,
                            color = NexusColors.TextMuted,
                        )
                    }
                    Text(situation.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(situation.summary, color = NexusColors.TextSecondary)
                    TextButton(onClick = {
                        expanded = if (expanded == situation.id) null else situation.id
                    }) {
                        Text(if (expanded == situation.id) "Hide evidence" else "See source signals")
                    }
                    if (expanded == situation.id) {
                        state.observations
                            .filter { it.id in situation.observationIds }
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

        if ((filter == "All" || filter == "Insights") && state.insights.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "What changed",
                    subtitle = "Evidence-backed findings rather than generic categories.",
                )
            }
            items(state.insights.take(6), key = { it.id }) { insight ->
                NexusCard(accent = NexusColors.Mint) {
                    NexusStatusPill("INSIGHT", NexusColors.Mint)
                    Text(insight.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(insight.summary, color = NexusColors.TextSecondary)
                    Text(insight.evidence, color = NexusColors.Mint, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if ((filter == "All" || filter == "Themes") && state.interests.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "Recurring themes",
                    subtitle = "Relative prominence in recent signals, not a certainty score.",
                )
            }
            items(state.interests, key = { it.id }) { interest ->
                NexusCard {
                    val prominence = interest.affinity.toFloat().coerceIn(0f, 1f)
                    val status = when {
                        prominence >= .80f -> "TRENDING"
                        prominence >= .55f -> "ACTIVE"
                        else -> "EMERGING"
                    }
                    val accent = when (status) {
                        "TRENDING" -> NexusColors.Violet
                        "ACTIVE" -> NexusColors.Cyan
                        else -> NexusColors.Mint
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NexusStatusPill(status, accent)
                        Text(
                            "${(prominence * 100).toInt()}% prominence",
                            color = NexusColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    NexusProgressRow(
                        title = interest.label,
                        progress = prominence,
                        detail = "${(interest.confidence * 100).toInt()}% confidence",
                        accent = accent,
                    )
                }
            }
        }

        if (state.interests.isEmpty() && state.situations.isEmpty() && state.insights.isEmpty()) {
            item {
                NexusCard {
                    Text("Not enough context yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Add notes or connect notifications. Discover will stay quiet until there is enough evidence for a useful connection.",
                        color = NexusColors.TextSecondary,
                    )
                }
            }
        }
    }

    selected?.let { ObservationDetails(it) { selected = null } }
}
