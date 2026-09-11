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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.core.model.Observation
import com.kareem.nexus.ui.design.NexusColors

@Composable
fun ObservationsScreen(contentPadding: PaddingValues, viewModel: MemoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Observation?>(null) }
    var visible by rememberSaveable(query, filter) { mutableIntStateOf(50) }
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(contentPadding), contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Memory", style = MaterialTheme.typography.headlineLarge)
            Text("Find the detail you remember.", color = NexusColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(query, onValueChange = { viewModel.query.value = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Search saved context") }, trailingIcon = {
                    if (query.isNotEmpty()) TextButton(onClick = { viewModel.query.value = "" }) { Text("Clear") }
                })
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MemoryFilter.entries) { item -> FilterChip(filter == item, onClick = { viewModel.filter.value = item }, label = { Text(item.label) }) }
            }
        }
        item { Text("${state.rows.size} results · ${state.total} saved", style = MaterialTheme.typography.labelMedium, color = NexusColors.TextSecondary) }
        state.error?.let { message -> item { Text(message, color = NexusColors.Rose) } }
        if (state.rows.isEmpty()) item {
            Text(if (state.total == 0) "Tap Add or share text, a link or an image to NEXUS to start your memory." else "No matches. Try fewer words or another filter.", modifier = Modifier.padding(vertical = 24.dp), color = NexusColors.TextSecondary)
        }
        items(state.rows.take(visible), key = Observation::id) { row ->
            val label = remember(row.source) { sourceLabel(context, row.source) }
            Surface(Modifier.fillMaxWidth().clickable { selected = row }, color = NexusColors.Surface, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(label, color = NexusColors.Cyan, style = MaterialTheme.typography.labelLarge)
                    ContentText(row.rawText, maxLines = 4)
                    Text(timestamp(row.createdAt), color = NexusColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (state.rows.size > visible) item { TextButton(onClick = { visible += 50 }) { Text("Show more") } }
    }
    selected?.let { ObservationDetails(it) { selected = null } }
}
