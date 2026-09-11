package com.kareem.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareem.nexus.core.model.*
import com.kareem.nexus.domain.intelligence.MemorySearch
import com.kareem.nexus.domain.intelligence.OnDeviceAi
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class MemoryFilter(val label: String) {
    ALL("All"),
    NOTIFICATIONS("Notifications"),
    NOTES("Notes"),
    LINKS("Links"),
    IMAGES("Images"),
    USAGE("Usage"),
}

data class MemoryUiState(
    val rows: List<Observation> = emptyList(),
    val total: Int = 0,
    val semanticAssist: Boolean = false,
    val error: String? = null,
)

private data class SearchExpansion(
    val query: String = "",
    val alternatives: List<String> = emptyList(),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MemoryViewModel @Inject constructor(
    repository: NexusRepository,
    onDeviceAi: OnDeviceAi,
) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(MemoryFilter.ALL)

    private val expansion = query
        .debounce(550)
        .mapLatest { text ->
            val clean = text.trim()
            if (clean.length < 4) SearchExpansion(clean)
            else SearchExpansion(clean, onDeviceAi.expandSearchQuery(clean))
        }
        .catch { cause ->
            if (cause is CancellationException) throw cause
            emit(SearchExpansion(query.value.trim()))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchExpansion())

    val state = combine(
        repository.allObservations(),
        query.debounce(100),
        filter,
        expansion,
    ) { rows, text, selected, expanded ->
        val cleanQuery = text.trim()
        val semanticTerms = expanded.alternatives.takeIf { expanded.query == cleanQuery }.orEmpty()

        val matches = rows.asSequence()
            .filter { row ->
                when (selected) {
                    MemoryFilter.ALL -> row.type != ObservationType.APP_USAGE
                    MemoryFilter.NOTIFICATIONS -> row.type == ObservationType.NOTIFICATION
                    MemoryFilter.NOTES -> row.type in setOf(ObservationType.MANUAL, ObservationType.SHARED_TEXT)
                    MemoryFilter.LINKS -> row.type == ObservationType.SHARED_LINK
                    MemoryFilter.IMAGES -> row.type == ObservationType.IMAGE
                    MemoryFilter.USAGE -> row.type == ObservationType.APP_USAGE
                }
            }
            .map { row ->
                val searchable = row.rawText + " " + row.source.orEmpty()
                val direct = MemorySearch.score(searchable, cleanQuery)
                val semantic = if (direct > 0 || semanticTerms.isEmpty()) {
                    0
                } else {
                    semanticTerms.maxOfOrNull { alternative -> MemorySearch.score(searchable, alternative) }?.let { score ->
                        if (score > 0) (score * 0.72).toInt().coerceAtLeast(1) else 0
                    } ?: 0
                }
                row to maxOf(direct, semantic)
            }
            .filter { cleanQuery.isBlank() || it.second > 0 }
            .sortedWith(
                compareByDescending<Pair<Observation, Int>> { it.second }
                    .thenByDescending { it.first.createdAt }
            )
            .map { it.first }
            .toList()

        MemoryUiState(
            rows = matches,
            total = rows.size,
            semanticAssist = cleanQuery.isNotBlank() && semanticTerms.isNotEmpty(),
        )
    }.flowOn(Dispatchers.Default).catch { error ->
        if (error is CancellationException) throw error
        emit(MemoryUiState(error = "Could not load Memory. Reopen NEXUS to retry."))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemoryUiState())
}
