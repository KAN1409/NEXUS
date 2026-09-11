package com.kareem.nexus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.core.model.Observation
import com.kareem.nexus.core.model.ObservationType
import com.kareem.nexus.ui.design.*

@Composable
fun ObservationsScreen(contentPadding: PaddingValues, viewModel: MemoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Observation?>(null) }
    var visible by rememberSaveable(query, filter) { mutableIntStateOf(50) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            NexusScreenHeader(
                title = "Memory",
                subtitle = "Everything NEXUS has observed and saved.",
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search memory") },
                placeholder = { Text("Person, topic, app, phrase or idea") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        TextButton(onClick = { viewModel.query.value = "" }) { Text("Clear") }
                    }
                },
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MemoryFilter.entries) { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { viewModel.filter.value = item },
                        label = { Text(item.label) },
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${state.rows.size} results",
                    style = MaterialTheme.typography.labelMedium,
                    color = NexusColors.TextSecondary,
                )
                Text(
                    "${state.total} saved",
                    style = MaterialTheme.typography.labelMedium,
                    color = NexusColors.Cyan,
                )
            }
        }

        state.error?.let { message -> item { Text(message, color = NexusColors.Rose) } }

        if (state.rows.isEmpty()) {
            item {
                NexusCard {
                    Text(
                        if (state.total == 0) "Memory is empty" else "No matches",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (state.total == 0)
                            "Tap Add or share text, a link or an image to NEXUS to start building Memory."
                        else
                            "Try fewer words, a related concept or another filter.",
                        color = NexusColors.TextSecondary,
                    )
                }
            }
        }

        items(state.rows.take(visible), key = Observation::id) { row ->
            val label = remember(row.source) { sourceLabel(context, row.source) }
            val accent = when (row.type) {
                ObservationType.NOTIFICATION -> NexusColors.Cyan
                ObservationType.MANUAL, ObservationType.SHARED_TEXT -> NexusColors.Violet
                ObservationType.SHARED_LINK -> NexusColors.Mint
                ObservationType.IMAGE -> NexusColors.Amber
                ObservationType.APP_USAGE -> NexusColors.TextMuted
            }
            NexusCard(
                modifier = Modifier.clickable { selected = row },
                accent = accent,
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(label, color = accent, style = MaterialTheme.typography.labelLarge)
                        NexusStatusPill(row.type.memoryLabel(), accent)
                    }
                    Text(
                        timestamp(row.createdAt),
                        color = NexusColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                ContentText(row.rawText, maxLines = 5)
            }
        }

        if (state.rows.size > visible) {
            item {
                OutlinedButton(
                    onClick = { visible += 50 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Show more") }
            }
        }
    }

    selected?.let { ObservationDetails(it) { selected = null } }
}

private fun ObservationType.memoryLabel(): String = when (this) {
    ObservationType.NOTIFICATION -> "NOTIFICATION"
    ObservationType.MANUAL -> "NOTE"
    ObservationType.SHARED_TEXT -> "SHARED TEXT"
    ObservationType.SHARED_LINK -> "LINK"
    ObservationType.IMAGE -> "IMAGE"
    ObservationType.APP_USAGE -> "APP USAGE"
}
