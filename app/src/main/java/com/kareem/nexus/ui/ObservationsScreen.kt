package com.kareem.nexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.core.model.Observation
import com.kareem.nexus.core.model.ObservationType
import com.kareem.nexus.ui.design.NexusColors

@Composable
fun ObservationsScreen(
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val usage = state.observations.filter { it.type == ObservationType.APP_USAGE }
    val memories = state.observations.filterNot { it.type == ObservationType.APP_USAGE }

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
                "What NEXUS has observed so far.",
                color = NexusColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(16.dp))
        }

        if (usage.isNotEmpty()) {
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
                            "Top apps from the latest 24-hour usage snapshot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        usage.take(6).forEach { observation ->
                            Text(
                                observation.rawText,
                                color = NexusColors.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (usage.size > 6) {
                            Text(
                                "+${usage.size - 6} more apps",
                                color = NexusColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        if (memories.isEmpty() && usage.isEmpty()) {
            item {
                Surface(color = NexusColors.Surface, shape = MaterialTheme.shapes.large) {
                    Text(
                        "Share a link, text or image to NEXUS. Your captured context will appear here.",
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
                    Text(observation.rawText, maxLines = 5, style = MaterialTheme.typography.bodyLarge)
                    observation.source?.let {
                        Text(it, color = NexusColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
