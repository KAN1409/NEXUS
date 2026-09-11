package com.kareem.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareem.nexus.core.model.*
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ActivityUiState(
    val actions: List<PreparedAction> = emptyList(),
    val events: List<ActionEvent> = emptyList(),
    val observations: List<Observation> = emptyList(),
    val openLoops: List<OpenLoop> = emptyList(),
    val executions: List<ActionExecution> = emptyList(),
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    repository: NexusRepository,
) : ViewModel() {
    val uiState: StateFlow<ActivityUiState> = combine(
        repository.actions(),
        repository.actionEvents(),
        repository.observations(),
        repository.openLoops(),
        repository.actionExecutions(),
    ) { actions, events, observations, openLoops, executions ->
        ActivityUiState(
            actions = actions,
            events = events,
            observations = observations,
            openLoops = openLoops,
            executions = executions,
        )
    }.catch { error ->
        if (error is CancellationException) throw error
        emit(ActivityUiState())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActivityUiState(),
    )
}
