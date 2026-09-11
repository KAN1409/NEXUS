package com.kareem.nexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.kareem.nexus.core.model.ObservationType
import com.kareem.nexus.ui.design.NexusColors

private enum class MemoryFilter(val label: String) {
    ALL("All"),
    NOTIFICATIONS("Notifications"),
    NOTES("Notes"),
    LINKS("Links"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservationsScreen(
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(MemoryFilter.ALL) }

    val usage = state.observations.filter { it.type == ObservationType.APP_USAGE }
    val memories = state.observations
        .filterNot { it.type == ObservationType.APP_USAGE }
        .filter { observation ->
            val filterMatch = when (filter) {
                MemoryFilter.ALL -> true
                MemoryFilter.NOTIFICATIONS -> observation.type == ObservationType.NOTIFICATION
                MemoryFilter.NOTES -> observation.type == ObservationType.MANUAL || observation.type == ObservationType.SHARED_TEXT
                MemoryFilter.LINKS -> observation.type == ObservationType.SHARED_LINK
            }
            val queryMatch = query.isBlank() ||
                observation.rawText.contains(query, ignoreCase = true) ||
                observation.source.orEmpty().contains(query, ignoreCase = true)
            filterMatch && queryMatch
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = contentPadding.calculateTopPadding() + 28.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Memory", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                "Search everything NEXUS has observed locally.",
                color = NexusColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search memory") },
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MemoryFilter.entries.forEach { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(item.label) },
                    )
                }
            }
        }

        if (usage.isNotEmpty() && query.isBlank() && filter == MemoryFilter.ALL) {
            item(key = "usage_summary") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NexusColors.Surface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("APP USAGE SUMMARY", color = NexusColors.Violet, style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Top apps from the latest 24-hour snapshot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        usage.take(6).forEach { observation ->
                            Text(observation.rawText, color = NexusColors.TextSecondary)
                        }
                        if (usage.size > 6) {
                            Text(
                                "+" + (usage.size - 6) + " more apps",
                                color = NexusColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        if (memories.isEmpty()) {
            item {
                Surface(color = NexusColors.Surface, shape = MaterialTheme.shapes.large) {
                    Text(
                        if (query.isBlank()) "No memories match this filter yet." else "No memory matches your search.",
                        modifier = Modifier.padding(20.dp),
                        color = NexusColors.TextSecondary,
                    )
                }
            }
        }

        items(memories, key = Observation::id) { observation ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NexusColors.Surface,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        when (observation.type) {
                            ObservationType.APP_USAGE -> "APP USAGE"
                            ObservationType.SHARED_LINK -> "LINK"
                            ObservationType.SHARED_TEXT -> "SHARED"
                            ObservationType.IMAGE -> "IMAGE"
                            ObservationType.NOTIFICATION -> "NOTIFICATION"
                            ObservationType.MANUAL -> "NOTE"
                        },
                        color = NexusColors.Violet,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(observation.rawText, maxLines = 6, style = MaterialTheme.typography.bodyLarge)
                    observation.source?.let {
                        Text(it, color = NexusColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
