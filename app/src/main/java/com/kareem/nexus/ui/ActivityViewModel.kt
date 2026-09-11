package com.kareem.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareem.nexus.core.model.ActionEvent
import com.kareem.nexus.core.model.Discovery
import com.kareem.nexus.core.model.Observation
import com.kareem.nexus.core.model.PreparedAction
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ActivityUiState(
    val actions: List<PreparedAction> = emptyList(),
    val events: List<ActionEvent> = emptyList(),
    val discoveries: List<Discovery> = emptyList(),
    val observations: List<Observation> = emptyList(),
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    repository: NexusRepository,
) : ViewModel() {
    val uiState: StateFlow<ActivityUiState> = combine(
        repository.actions(),
        repository.actionEvents(),
        repository.discoveries(),
        repository.observations(),
    ) { actions, events, discoveries, observations ->
        ActivityUiState(
            actions = actions,
            events = events,
            discoveries = discoveries,
            observations = observations,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActivityUiState(),
    )
}
