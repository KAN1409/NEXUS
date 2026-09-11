package com.kareem.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareem.nexus.core.model.Discovery
import com.kareem.nexus.core.model.Interest
import com.kareem.nexus.core.model.Observation
import com.kareem.nexus.core.model.PreparedAction
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val observationCount: Int = 0,
    val interestCount: Int = 0,
    val observations: List<Observation> = emptyList(),
    val interests: List<Interest> = emptyList(),
    val discoveries: List<Discovery> = emptyList(),
    val actions: List<PreparedAction> = emptyList(),
    val readyActionCount: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: NexusRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        repository.observationCount(),
        repository.observations(),
        repository.interests(),
        repository.discoveries(),
        repository.actions(),
    ) { observationCount, observations, interests, discoveries, actions ->
        HomeUiState(
            observationCount = observationCount,
            interestCount = interests.size,
            observations = observations,
            interests = interests,
            discoveries = discoveries,
            actions = actions,
            readyActionCount = actions.count { it.state.name == "READY_FOR_APPROVAL" },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
