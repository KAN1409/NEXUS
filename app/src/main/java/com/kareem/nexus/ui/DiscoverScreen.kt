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
                "Connections, patterns and changes NEXUS found in your context.",
                color = NexusColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(10.dp))
        }

        if (state.situations.isNotEmpty()) {
            item {
                Text("Connected situations", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            items(state.situations, key = { "discover_" + it.id }) { situation ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NexusColors.Surface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(situation.kind.name.replace('_', ' '), color = NexusColors.Cyan, style = MaterialTheme.typography.labelLarge)
                        Text(situation.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(situation.summary, color = NexusColors.TextSecondary)
                        Text(
                            situation.observationIds.size.toString() + " signals connected",
                            color = NexusColors.Violet,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        if (state.insights.isNotEmpty()) {
            item {
                Text("Insights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            items(state.insights, key = { "insight_" + it.id }) { insight ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NexusColors.Surface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("INSIGHT", color = NexusColors.Violet, style = MaterialTheme.typography.labelLarge)
                        Text(insight.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(insight.summary, color = NexusColors.TextSecondary)
                        HorizontalDivider()
                        Text(insight.evidence, color = NexusColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (state.situations.isEmpty() && state.insights.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NexusColors.Surface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        "NEXUS needs a little more context before it can connect meaningful situations.",
                        modifier = Modifier.padding(20.dp),
                        color = NexusColors.TextSecondary,
                    )
                }
            }
        }
    }
}
