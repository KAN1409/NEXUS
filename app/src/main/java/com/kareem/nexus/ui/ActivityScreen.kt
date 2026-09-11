package com.kareem.nexus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.core.model.*
import com.kareem.nexus.ui.design.*

@Composable
fun ActivityScreen(
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
    history: ActivityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity by history.uiState.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("Active") }
    var selected by remember { mutableStateOf<Observation?>(null) }

    val actions = state.actions.filter {
        when (filter) {
            "Active" -> it.state in setOf(ActionState.READY_FOR_APPROVAL, ActionState.APPROVED, ActionState.EXECUTING)
            "Later" -> it.state == ActionState.DRAFT
            "Finished" -> it.state in setOf(ActionState.COMPLETED, ActionState.REJECTED, ActionState.FAILED)
            else -> false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            NexusScreenHeader(
                title = "Activity",
                subtitle = "From observation to decision to outcome.",
            )
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Active", "Later", "Finished", "Timeline")) { label ->
                    FilterChip(
                        selected = filter == label,
                        onClick = { filter = label },
                        label = { Text(label) },
                    )
                }
            }
        }

        state.error?.let { message -> item { Text(message, color = NexusColors.Rose) } }

        if (filter == "Timeline") {
            item {
                NexusSectionHeader(
                    title = "Decision timeline",
                    subtitle = "A causal history of what NEXUS suggested and what happened next.",
                )
            }

            if (activity.events.isEmpty()) {
                item {
                    NexusCard {
                        Text("No decisions yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Approve, defer or dismiss a suggestion and the history will appear here.",
                            color = NexusColors.TextSecondary,
                        )
                    }
                }
            }

            items(activity.events, key = { it.id }) { event ->
                val action = state.actions.firstOrNull { it.id == event.actionId }
                TimelineEvent(
                    signal = event.signal,
                    title = action?.title ?: "NEXUS action",
                    detail = action?.description.orEmpty(),
                    createdAt = event.createdAt,
                )
            }
        } else {
            item {
                val subtitle = when (filter) {
                    "Active" -> "Suggestions waiting for a decision or outcome."
                    "Later" -> "Deferred actions that can resurface after their wait period."
                    else -> "Completed, dismissed and failed actions."
                }
                NexusSectionHeader(filter, subtitle)
            }

            if (actions.isEmpty()) {
                item {
                    NexusCard {
                        Text("No ${filter.lowercase()} actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("This section will update automatically as you review suggestions.", color = NexusColors.TextSecondary)
                    }
                }
            }

            items(actions, key = { it.id }) { action ->
                ActionCard(
                    action = action,
                    viewModel = viewModel,
                    pending = state.pending,
                    observation = state.observations.firstOrNull { action.id.endsWith(it.id) },
                    onEvidence = { selected = it },
                )
            }
        }
    }

    selected?.let { ObservationDetails(it) { selected = null } }
}

@Composable
private fun TimelineEvent(
    signal: FeedbackSignal,
    title: String,
    detail: String,
    createdAt: Long,
) {
    val accent = when (signal) {
        FeedbackSignal.COMPLETED -> NexusColors.Mint
        FeedbackSignal.FAILED, FeedbackSignal.REJECTED -> NexusColors.Rose
        FeedbackSignal.DEFERRED -> NexusColors.Amber
        FeedbackSignal.APPROVED, FeedbackSignal.STARTED, FeedbackSignal.ACTED -> NexusColors.Cyan
        FeedbackSignal.SUGGESTED, FeedbackSignal.RESURFACED -> NexusColors.Violet
        else -> NexusColors.TextSecondary
    }
    val label = when (signal) {
        FeedbackSignal.SUGGESTED -> "Suggestion created"
        FeedbackSignal.APPROVED -> "Action approved"
        FeedbackSignal.DEFERRED -> "Saved for later"
        FeedbackSignal.RESURFACED -> "Suggestion resurfaced"
        FeedbackSignal.REJECTED -> "Suggestion dismissed"
        FeedbackSignal.STARTED -> "Action in progress"
        FeedbackSignal.COMPLETED -> "Action completed"
        FeedbackSignal.FAILED -> "Action failed"
        FeedbackSignal.ACTED -> "Action taken"
        FeedbackSignal.OPENED -> "Opened"
        FeedbackSignal.SAVED -> "Saved"
        FeedbackSignal.DISMISSED -> "Dismissed"
        FeedbackSignal.SHARED -> "Shared"
        FeedbackSignal.DWELL -> "Viewed"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.width(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(10.dp).background(accent, CircleShape),
            )
            Box(
                Modifier.width(2.dp).height(86.dp).background(NexusColors.BorderSoft),
            )
        }
        Surface(
            modifier = Modifier.weight(1f),
            color = NexusColors.Surface,
            shape = RoundedCornerShape(NexusRadius.Medium),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    NexusStatusPill(label.uppercase(), accent)
                    Text(timestamp(createdAt), color = NexusColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (detail.isNotBlank()) ContentText(detail, maxLines = 3)
            }
        }
    }
}
