package com.kareem.nexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.core.model.Observation
import com.kareem.nexus.ui.design.NexusColors

@Composable
fun DiscoverScreen(contentPadding: PaddingValues, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Observation?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(contentPadding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Discover", style = MaterialTheme.typography.headlineLarge); Text("Connections grounded in your saved context.", color = NexusColors.TextSecondary) }
        items(state.situations, key = { it.id }) { situation ->
            Surface(color = NexusColors.Surface, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(situation.kind.name, style = MaterialTheme.typography.labelMedium, color = NexusColors.Violet)
                    Text(situation.title, style = MaterialTheme.typography.titleLarge)
                    Text(situation.summary, color = NexusColors.TextSecondary)
                    TextButton(onClick = { expanded = if (expanded == situation.id) null else situation.id }) { Text(if (expanded == situation.id) "Hide evidence" else "See ${situation.observationIds.size} source signals") }
                    if (expanded == situation.id) {
                        state.observations.filter { it.id in situation.observationIds }.forEach { observation ->
                            HorizontalDivider(color = NexusColors.Border)
                            TextButton(onClick = { selected = observation }) { ContentText(observation.rawText, maxLines = 3) }
                        }
                    }
                }
            }
        }
        if (state.interests.isNotEmpty()) item { SectionTitle("Recurring themes", "Relative prominence in recent signals, not a certainty score.") }
        items(state.interests, key = { it.id }) { interest ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(interest.label, style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(progress = { interest.affinity.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = NexusColors.Violet, trackColor = NexusColors.SurfaceRaised)
            }
        }
        if (state.interests.isEmpty() && state.situations.isEmpty()) item { Text("Add a few notes or connect notifications in Settings. Related context will appear here.", color = NexusColors.TextSecondary) }
    }
    selected?.let { ObservationDetails(it) { selected = null } }
}
