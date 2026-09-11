package com.kareem.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareem.nexus.core.model.*
import com.kareem.nexus.domain.intelligence.ContextIntelligence
import com.kareem.nexus.domain.intelligence.PersonalIntelligenceEngine
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val error: String? = null,
    val pending: Set<String> = emptySet(),
    val observationCount: Int = 0,
    val interestCount: Int = 0,
    val observations: List<Observation> = emptyList(),
    val interests: List<Interest> = emptyList(),
    val discoveries: List<Discovery> = emptyList(),
    val actions: List<PreparedAction> = emptyList(),
    val readyActionCount: Int = 0,
    val attention: List<AttentionItem> = emptyList(),
    val situations: List<Situation> = emptyList(),
    val topOfMind: List<TopOfMindItem> = emptyList(),
    val contextSituations: List<ContextSituation> = emptyList(),
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

    private val error = MutableStateFlow<String?>(null)
    private val pending = MutableStateFlow<Set<String>>(emptySet())

    private val content = combine(
        repository.observationCount(),
        repository.observations(),
        repository.interests(),
        repository.discoveries(),
        repository.actions(),
    ) { observationCount, observations, interests, discoveries, actions ->
        val byId = observations.associateBy { it.id }
        fun sourceObservation(action: PreparedAction): Observation? {
            val rawId = action.id.removePrefix("action_signal_").removePrefix("action_commitment_")
            return byId[rawId]
        }

        val visibleActions = actions.filter { action ->
            when (action.state) {
                ActionState.READY_FOR_APPROVAL, ActionState.DRAFT -> {
                    val source = sourceObservation(action)
                    source == null || !ContextIntelligence.shouldSuppress(source.rawText, source.source)
                }
                else -> true
            }
        }

        val unresolved = ContextIntelligence.unresolved(observations, visibleActions)
        val attention = ContextIntelligence.buildAttention(unresolved)
        val situations = ContextIntelligence.buildSituations(observations)
        val topOfMind = PersonalIntelligenceEngine.topOfMind(observations, visibleActions)
        val contextSituations = PersonalIntelligenceEngine.buildSituations(observations)

        HomeUiState(
            observationCount = observationCount,
            interestCount = interests.size,
            observations = observations,
            interests = interests,
            discoveries = discoveries,
            actions = visibleActions,
            readyActionCount = topOfMind.size,
            attention = attention,
            situations = situations,
            topOfMind = topOfMind,
            contextSituations = contextSituations,
            brief = ContextIntelligence.buildDailyBrief(observations, interests, attention),
            insights = ContextIntelligence.buildInsights(observations, interests, situations),
        )
    }.flowOn(Dispatchers.Default).catch { cause ->
        if (cause is CancellationException) throw cause
        emit(HomeUiState(error = "Could not load your context. Reopen NEXUS to retry."))
    }

    val uiState = combine(content, error, pending) { state, message, busy ->
        state.copy(error = message ?: state.error, pending = busy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun action(id: String, block: suspend () -> Unit) {
        if (id in pending.value) return
        pending.update { it + id }
        viewModelScope.launch {
            try {
                block()
                error.value = null
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                error.value = "Could not save this change. Please retry."
            } finally {
                pending.update { it - id }
            }
        }
    }

    fun approveAction(id: String) = action(id) { repository.approveAction(id) }
    fun deferAction(id: String) = action(id) { repository.deferAction(id) }
    fun rejectAction(id: String) = action(id) { repository.rejectAction(id) }
    fun startAction(id: String) = action(id) { repository.startAction(id) }
    fun completeAction(id: String) = action(id) { repository.completeAction(id) }
    fun failAction(id: String) = action(id) { repository.failAction(id) }
    fun refreshUnderstanding() = action("refresh") { repository.rebuildUnderstanding() }
}
