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
import com.kareem.nexus.core.model.SituationBrief
import com.kareem.nexus.ui.design.*

@Composable
fun DiscoverScreen(contentPadding: PaddingValues, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Observation?>(null) }
    var filter by rememberSaveable { mutableStateOf("Active") }
    val filters = listOf("Active", "Changed", "All")

    val rows = remember(state.situationBriefs, filter) {
        when (filter) {
            "Active" -> state.situationBriefs.filter { it.openLoopCount > 0 }
            "Changed" -> state.situationBriefs.filter { it.evidenceCount > 1 }.sortedByDescending { it.lastUpdatedAt }
            else -> state.situationBriefs
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            NexusScreenHeader(
                title = "Situations",
                subtitle = "Threads that connect evidence, current state, open loops and the next useful step.",
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

        if (rows.isEmpty()) {
            item {
                NexusCard(accent = NexusColors.Mint) {
                    Text(
                        if (state.observationCount == 0) "No situations yet" else "No ${filter.lowercase()} situations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "NEXUS only creates a Situation after related evidence forms a real thread. One random notification is not enough.",
                        color = NexusColors.TextSecondary,
                    )
                }
            }
        }

        items(rows, key = SituationBrief::situationId) { brief ->
            val accent = when {
                brief.priority >= .85 -> NexusColors.Rose
                brief.openLoopCount > 0 -> NexusColors.Cyan
                brief.evidenceCount > 2 -> NexusColors.Violet
                else -> NexusColors.Mint
            }
            val situation = state.contextSituations.firstOrNull { it.id == brief.situationId }

            NexusCard(accent = accent) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    NexusStatusPill(
                        if (brief.openLoopCount > 0) "${brief.openLoopCount} OPEN" else "CONTEXT",
                        accent,
                    )
                    Text(
                        "${brief.evidenceCount} evidence",
                        color = NexusColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(brief.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Text("CURRENT STATE", color = NexusColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                ContentText(brief.currentState, maxLines = 4)

                HorizontalDivider(color = NexusColors.BorderSoft)
                Text("WHAT CHANGED", color = NexusColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                Text(brief.whatChanged, color = NexusColors.TextSecondary)

                brief.nextStep?.let { next ->
                    Surface(color = NexusColors.CyanSoft, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("NEXT", color = NexusColors.Cyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(next, color = NexusColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (situation != null && situation.observationIds.isNotEmpty()) {
                    TextButton(onClick = { expanded = if (expanded == brief.situationId) null else brief.situationId }) {
                        Text(if (expanded == brief.situationId) "Hide evidence" else "Show evidence")
                    }
                    if (expanded == brief.situationId) {
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
                                        Text(timestamp(observation.createdAt), color = NexusColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    selected?.let { ObservationDetails(it) { selected = null } }
}
