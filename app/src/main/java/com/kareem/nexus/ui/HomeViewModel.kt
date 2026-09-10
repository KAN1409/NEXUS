package com.kareem.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareem.nexus.core.model.Discovery
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val observationCount: Int = 0,
    val interestCount: Int = 0,
    val discoveries: List<Discovery> = emptyList(),
    val readyActionCount: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NexusRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        repository.observationCount(),
        repository.interestCount(),
        repository.discoveries(),
        repository.readyActions(),
    ) { observations, interests, discoveries, actions ->
        HomeUiState(observations, interests, discoveries, actions.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { repository.seedFirstRun() }
    }
}
