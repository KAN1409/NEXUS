package com.kareem.nexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.kareem.nexus.core.model.ObservationType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.core.model.Observation
import com.kareem.nexus.ui.design.NexusColors

@Composable
fun ObservationsScreen(contentPadding: PaddingValues, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = contentPadding.calculateTopPadding() + 28.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Memory", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("What NEXUS has observed so far.", color = NexusColors.TextSecondary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
        }
        if (state.observations.isEmpty()) item {
            Surface(color = NexusColors.Surface, shape = MaterialTheme.shapes.large) {
                Text("Share a link, text or image to NEXUS. Your captured context will appear here.", modifier = Modifier.padding(20.dp), color = NexusColors.TextSecondary)
            }
        }
        items(state.observations, key = Observation::id) { observation ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NexusColors.Surface,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    if (observation.type != ObservationType.APP_USAGE) {
                        observation.source?.let { Text(it, color = NexusColors.TextSecondary, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}
