package com.kareem.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareem.nexus.core.model.*
import com.kareem.nexus.domain.intelligence.MemorySearch
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class MemoryFilter(val label: String) { ALL("All"), NOTIFICATIONS("Notifications"), NOTES("Notes"), LINKS("Links"), IMAGES("Images"), USAGE("Usage") }
data class MemoryUiState(val rows: List<Observation> = emptyList(), val total: Int = 0, val error: String? = null)

@OptIn(FlowPreview::class)
@HiltViewModel
class MemoryViewModel @Inject constructor(repository: NexusRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(MemoryFilter.ALL)
    val state = combine(repository.allObservations(), query.debounce(180), filter) { rows, text, selected ->
        val matches = rows.asSequence().filter {
            when (selected) {
                MemoryFilter.ALL -> it.type != ObservationType.APP_USAGE
                MemoryFilter.NOTIFICATIONS -> it.type == ObservationType.NOTIFICATION
                MemoryFilter.NOTES -> it.type in setOf(ObservationType.MANUAL, ObservationType.SHARED_TEXT)
                MemoryFilter.LINKS -> it.type == ObservationType.SHARED_LINK
                MemoryFilter.IMAGES -> it.type == ObservationType.IMAGE
                MemoryFilter.USAGE -> it.type == ObservationType.APP_USAGE
            }
        }.map { it to MemorySearch.score(it.rawText + " " + it.source.orEmpty(), text) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<Observation, Int>> { it.second }.thenByDescending { it.first.createdAt })
            .map { it.first }.toList()
        MemoryUiState(matches, rows.size)
    }.flowOn(Dispatchers.Default).catch { error ->
        if (error is CancellationException) throw error
        emit(MemoryUiState(error = "Could not load Memory. Reopen NEXUS to retry."))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemoryUiState())
}
