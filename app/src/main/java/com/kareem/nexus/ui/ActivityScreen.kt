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
import com.kareem.nexus.core.model.FeedbackSignal
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
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionsById = state.actions.associateBy { it.id }

    val rows = buildList {
        state.events.forEach { event ->
            val action = actionsById[event.actionId]
            val kind = when (event.signal) {
                FeedbackSignal.SUGGESTED -> "SUGGESTED"
                FeedbackSignal.APPROVED -> "USER APPROVED"
                FeedbackSignal.DEFERRED -> "DEFERRED FOR 24H"
                FeedbackSignal.RESURFACED -> "RESURFACED"
                FeedbackSignal.REJECTED -> "USER DISMISSED"
                FeedbackSignal.STARTED -> "ACTION STARTED"
                FeedbackSignal.COMPLETED -> "ACTION COMPLETED"
                FeedbackSignal.FAILED -> "ACTION FAILED"
                FeedbackSignal.SAVED -> "SAVED"
                FeedbackSignal.ACTED -> "ACTION TAKEN"
                FeedbackSignal.DISMISSED -> "DISMISSED"
                FeedbackSignal.OPENED -> "OPENED"
                FeedbackSignal.SHARED -> "SHARED"
                FeedbackSignal.DWELL -> "VIEWED"
            }
            add(
                ActivityRow(
                    id = "e_${event.id}",
                    title = action?.title ?: "NEXUS action",
                    detail = action?.description.orEmpty(),
                    kind = kind,
                    createdAt = event.createdAt,
                )
            )
        }

        state.discoveries.forEach {
            add(
                ActivityRow(
                    id = "d_${it.id}",
                    title = it.title,
                    detail = it.summary,
                    kind = "INFERRED PATTERN",
                    createdAt = it.createdAt,
                )
            )
        }

        state.observations
            .filter { it.type.name != "APP_USAGE" }
            .take(12)
            .forEach {
                add(
                    ActivityRow(
                        id = "o_${it.id}",
                        title = it.rawText.take(80),
                        detail = it.source.orEmpty(),
                        kind = "OBSERVED · ${it.type.name.replace('_', ' ')}",
                        createdAt = it.createdAt,
                    )
                )
            }
    }
        .distinctBy { it.id }
        .sortedByDescending { it.createdAt }
        .take(40)

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
                "Observed → inferred → suggested → decided → result.",
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
