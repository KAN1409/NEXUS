package com.kareem.nexus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.ui.design.NexusColors
import com.kareem.nexus.ui.design.NexusRadius
import com.kareem.nexus.ui.design.NexusSpacing

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = NexusSpacing.Xl, vertical = NexusSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(NexusSpacing.Lg),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(NexusSpacing.Xs)) {
                Text("NEXUS", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text(
                    "Your world, in context.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NexusColors.TextSecondary,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(NexusSpacing.Md)) {
                StatCard("Observations", state.observationCount.toString(), Modifier.weight(1f))
                StatCard("Interests", state.interestCount.toString(), Modifier.weight(1f))
                StatCard("Ready", state.readyActionCount.toString(), Modifier.weight(1f))
            }
        }

        if (state.interests.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(NexusSpacing.Sm)) {
                    Text("NEXUS is learning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        state.interests.take(3).joinToString("  ·  ") { it.label },
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexusColors.Cyan,
                    )
                }
            }
        }

        item {
            Text("For You", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        if (state.discoveries.isEmpty()) {
            item {
                EmptyForYouCard()
            }
        } else {
            items(state.discoveries, key = { it.id }) { discovery ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NexusColors.SurfaceRaised),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(NexusRadius.Medium),
                ) {
                    Column(
                        Modifier.padding(NexusSpacing.Lg),
                        verticalArrangement = Arrangement.spacedBy(NexusSpacing.Sm),
                    ) {
                        Text(
                            discovery.type.name.replace('_', ' '),
                            style = MaterialTheme.typography.labelMedium,
                            color = NexusColors.Violet,
                        )
                        Text(discovery.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(discovery.summary, style = MaterialTheme.typography.bodyMedium, color = NexusColors.TextSecondary)
                        HorizontalDivider(color = NexusColors.Border)
                        Text(
                            "Why this: ${discovery.whyThis}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NexusColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NexusColors.Surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(NexusRadius.Medium),
    ) {
        Column(
            Modifier.padding(horizontal = NexusSpacing.Md, vertical = NexusSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(NexusSpacing.Xs),
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = NexusColors.TextSecondary)
        }
    }
}

@Composable
private fun EmptyForYouCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = NexusColors.Surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(NexusRadius.Medium),
    ) {
        Column(
            Modifier.padding(NexusSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(NexusSpacing.Sm),
        ) {
            Text("Nothing noisy here", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "NEXUS will surface something when it has a useful reason to.",
                style = MaterialTheme.typography.bodyMedium,
                color = NexusColors.TextSecondary,
            )
        }
    }
}
