package com.kareem.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareem.nexus.core.model.*
import com.kareem.nexus.domain.intelligence.ContextIntelligence
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val observationCount: Int = 0,
    val interestCount: Int = 0,
    val observations: List<Observation> = emptyList(),
    val interests: List<Interest> = emptyList(),
    val discoveries: List<Discovery> = emptyList(),
    val actions: List<PreparedAction> = emptyList(),
    val readyActionCount: Int = 0,
    val attention: List<AttentionItem> = emptyList(),
    val situations: List<Situation> = emptyList(),
    val brief: DailyBrief = DailyBrief(
        headline = "NEXUS is getting ready",
        summary = "Your local context will appear here as it becomes useful.",
        attentionCount = 0,
        newSignals = 0,
        topTheme = null,
    ),
    val insights: List<IntelligenceInsight> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NexusRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observationCount(),
        repository.observations(),
        repository.interests(),
        repository.discoveries(),
        repository.actions(),
    ) { observationCount, observations, interests, discoveries, actions ->
        val attention = ContextIntelligence.buildAttention(observations)
        val situations = ContextIntelligence.buildSituations(observations)
        HomeUiState(
            observationCount = observationCount,
            interestCount = interests.size,
            observations = observations,
            interests = interests,
            discoveries = discoveries,
            actions = actions,
            readyActionCount = actions.count { it.state == ActionState.READY_FOR_APPROVAL },
            attention = attention,
            situations = situations,
            brief = ContextIntelligence.buildDailyBrief(observations, interests, attention),
            insights = ContextIntelligence.buildInsights(observations, interests, situations),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun approveAction(id: String) = viewModelScope.launch { repository.approveAction(id) }
    fun deferAction(id: String) = viewModelScope.launch { repository.deferAction(id) }
    fun rejectAction(id: String) = viewModelScope.launch { repository.rejectAction(id) }
    fun startAction(id: String) = viewModelScope.launch { repository.startAction(id) }
    fun completeAction(id: String) = viewModelScope.launch { repository.completeAction(id) }
    fun failAction(id: String) = viewModelScope.launch { repository.failAction(id) }
    fun refreshUnderstanding() = viewModelScope.launch { repository.rebuildUnderstanding() }
}
