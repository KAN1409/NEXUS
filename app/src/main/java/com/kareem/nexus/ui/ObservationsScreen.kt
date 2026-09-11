package com.kareem.nexus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
    var visible by rememberSaveable(query, filter) { mutableIntStateOf(80) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item {
            NexusScreenHeader(
                title = "Memory",
                subtitle = "Everything NEXUS has seen, saved and made searchable.",
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search people, amounts, screenshots or ideas") },
                leadingIcon = {
                    NexusIcon(
                        type = NexusIconType.Discover,
                        modifier = Modifier.size(20.dp),
                        primary = NexusColors.TextSecondary,
                        secondary = NexusColors.Cyan,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        TextButton(onClick = { viewModel.query.value = "" }) { Text("Clear") }
                    }
                },
                shape = RoundedCornerShape(NexusRadius.Medium),
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
                            "Tap Add or share text, a link or an image to start building your local memory."
                        else
                            "Try how you remember it instead of the exact wording. NEXUS also searches related concepts and OCR text.",
                        color = NexusColors.TextSecondary,
                    )
                }
            }
        }

        items(state.rows.take(visible), key = Observation::id) { row ->
            val label = remember(row.source) { sourceLabel(context, row.source) }
            MemoryEvidenceRow(
                row = row,
                sourceLabel = label,
                onClick = { selected = row },
            )
        }

        if (state.rows.size > visible) {
            item {
                OutlinedButton(
                    onClick = { visible += 80 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Show more") }
            }
        }
    }

    selected?.let { ObservationDetails(it) { selected = null } }
}

@Composable
private fun MemoryEvidenceRow(
    row: Observation,
    sourceLabel: String,
    onClick: () -> Unit,
) {
    val accent = when (row.type) {
        ObservationType.NOTIFICATION -> NexusColors.Cyan
        ObservationType.MANUAL, ObservationType.SHARED_TEXT -> NexusColors.Violet
        ObservationType.SHARED_LINK -> NexusColors.Mint
        ObservationType.IMAGE -> NexusColors.Amber
        ObservationType.APP_USAGE -> NexusColors.TextMuted
    }
    val icon = when (row.type) {
        ObservationType.NOTIFICATION -> NexusIconType.Activity
        ObservationType.MANUAL, ObservationType.SHARED_TEXT -> NexusIconType.Saved
        ObservationType.SHARED_LINK -> NexusIconType.Link
        ObservationType.IMAGE -> NexusIconType.Image
        ObservationType.APP_USAGE -> NexusIconType.Discover
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = NexusColors.Surface,
        shape = RoundedCornerShape(NexusRadius.Medium),
        border = BorderStroke(1.dp, NexusColors.BorderSoft),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = accent.copy(alpha = .12f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    NexusIcon(
                        type = icon,
                        modifier = Modifier.size(24.dp),
                        primary = accent,
                        secondary = if (accent == NexusColors.Violet) NexusColors.Cyan else NexusColors.Violet,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        sourceLabel,
                        color = accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        timestamp(row.createdAt),
                        color = NexusColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                NexusStatusPill(row.type.memoryLabel(), accent)
                ContentText(row.rawText, maxLines = if (row.type == ObservationType.IMAGE) 4 else 3)
            }
        }
    }
}

private fun ObservationType.memoryLabel(): String = when (this) {
    ObservationType.NOTIFICATION -> "NOTIFICATION"
    ObservationType.MANUAL -> "NOTE"
    ObservationType.SHARED_TEXT -> "SHARED TEXT"
    ObservationType.SHARED_LINK -> "LINK"
    ObservationType.IMAGE -> "IMAGE"
    ObservationType.APP_USAGE -> "APP USAGE"
}
