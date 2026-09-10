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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.ui.design.NexusColors

@Composable
fun DiscoverScreen(
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = contentPadding.calculateTopPadding() + 28.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Discover", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                "Patterns NEXUS found in your recent context.",
                color = NexusColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(10.dp))
        }

        if (state.discoveries.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NexusColors.Surface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        "Capture usage, share something, or add a note. NEXUS will turn repeated signals into patterns here.",
                        modifier = Modifier.padding(20.dp),
                        color = NexusColors.TextSecondary,
                    )
                }
            }
        } else {
            items(state.discoveries, key = { it.id }) { discovery ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NexusColors.Surface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("PATTERN", color = NexusColors.Violet, style = MaterialTheme.typography.labelLarge)
                        Text(discovery.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(discovery.summary, color = NexusColors.TextSecondary)
                        HorizontalDivider()
                        Text("Why this: ${discovery.whyThis}", color = NexusColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
