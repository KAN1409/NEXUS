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

private data class ActivityRow(
    val id: String,
    val title: String,
    val detail: String,
    val kind: String,
    val createdAt: Long,
)

@Composable
fun ActivityScreen(
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val rows = buildList {
        state.actions.forEach {
            add(ActivityRow("a_${it.id}", it.title, it.description, "ACTION SUGGESTED", it.createdAt))
        }
        state.discoveries.forEach {
            add(ActivityRow("d_${it.id}", it.title, it.summary, "PATTERN DETECTED", it.createdAt))
        }
        state.observations.take(12).forEach {
            add(ActivityRow("o_${it.id}", it.rawText.take(80), it.source.orEmpty(), it.type.name.replace('_', ' '), it.createdAt))
        }
    }.sortedByDescending { it.createdAt }.take(30)

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
            Text("Activity", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                "From observation to action.",
                color = NexusColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(10.dp))
        }

        if (rows.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NexusColors.Surface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        "NEXUS activity will appear here as it observes, understands and prepares suggestions.",
                        modifier = Modifier.padding(20.dp),
                        color = NexusColors.TextSecondary,
                    )
                }
            }
        } else {
            items(rows, key = { it.id }) { row ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NexusColors.Surface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(row.kind, color = NexusColors.Violet, style = MaterialTheme.typography.labelLarge)
                        Text(row.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (row.detail.isNotBlank()) {
                            Text(row.detail, color = NexusColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
