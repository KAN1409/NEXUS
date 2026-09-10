package com.kareem.nexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("NEXUS", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text("Personal intelligence, built local-first.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Observations", state.observationCount.toString(), Modifier.weight(1f))
                    StatCard("Interests", state.interestCount.toString(), Modifier.weight(1f))
                    StatCard("Ready", state.readyActionCount.toString(), Modifier.weight(1f))
                }
            }
            item { Text("For You", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(state.discoveries, key = { it.id }) { discovery ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(discovery.type.name.replace('_', ' '), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(discovery.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(discovery.summary, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider()
                        Text("Why this: ${discovery.whyThis}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
