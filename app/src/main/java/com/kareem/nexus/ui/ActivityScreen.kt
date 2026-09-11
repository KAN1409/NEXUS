package com.kareem.nexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.core.model.*
import com.kareem.nexus.ui.design.NexusColors

@Composable
fun ActivityScreen(contentPadding: PaddingValues, viewModel: HomeViewModel = hiltViewModel(), history: ActivityViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity by history.uiState.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("Active") }
    var selected by remember { mutableStateOf<Observation?>(null) }
    val actions = state.actions.filter {
        when (filter) {
            "Active" -> it.state in setOf(ActionState.READY_FOR_APPROVAL, ActionState.APPROVED, ActionState.EXECUTING)
            "Later" -> it.state == ActionState.DRAFT
            else -> it.state in setOf(ActionState.COMPLETED, ActionState.REJECTED, ActionState.FAILED)
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(contentPadding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Activity", style = MaterialTheme.typography.headlineLarge); Text("Your decisions and what happened next.", color = NexusColors.TextSecondary) }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Active", "Later", "Finished", "Timeline")) { label -> FilterChip(filter == label, onClick = { filter = label }, label = { Text(label) }) }
        } }
        state.error?.let { message -> item { Text(message, color = NexusColors.Rose) } }
        if (filter == "Timeline") {
            if (activity.events.isEmpty()) item { Text("Decisions will appear here as you review suggestions.", color = NexusColors.TextSecondary) }
            items(activity.events, key = { it.id }) { event ->
                val action = state.actions.firstOrNull { it.id == event.actionId }
                Surface(color = NexusColors.Surface, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(event.signal.name.replace('_', ' '), color = NexusColors.Cyan, style = MaterialTheme.typography.labelMedium)
                        Text(action?.title ?: "Action", style = MaterialTheme.typography.titleMedium)
                        action?.let { ContentText(it.description, maxLines = 3) }
                        Text(timestamp(event.createdAt), color = NexusColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            if (actions.isEmpty()) item { Text("No ${filter.lowercase()} actions.", color = NexusColors.TextSecondary) }
            items(actions, key = { it.id }) { action ->
                ActionCard(action, viewModel, state.pending, state.observations.firstOrNull { action.id.endsWith(it.id) }) { selected = it }
            }
        }
    }
    selected?.let { ObservationDetails(it) { selected = null } }
}
