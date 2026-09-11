package com.kareem.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareem.nexus.core.model.*
import com.kareem.nexus.domain.intelligence.ContextIntelligence
import com.kareem.nexus.domain.intelligence.HomeSurfacePolicy
import com.kareem.nexus.domain.intelligence.PersonalIntelligenceEngine
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
    val openLoops: List<OpenLoop> = emptyList(),
    val needsYou: List<OpenLoop> = emptyList(),
    val waitingOn: List<OpenLoop> = emptyList(),
    val upcoming: List<OpenLoop> = emptyList(),
    val situationBriefs: List<SituationBrief> = emptyList(),
    val recentChanges: List<SituationBrief> = emptyList(),
)

private data class LegacyHomeContent(
    val observationCount: Int,
    val observations: List<Observation>,
    val interests: List<Interest>,
    val discoveries: List<Discovery>,
    val actions: List<PreparedAction>,
)

private data class UnifiedHomeContent(
    val loops: List<OpenLoop>,
    val briefs: List<SituationBrief>,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NexusRepository,
) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    private val pending = MutableStateFlow<Set<String>>(emptySet())

    private val legacy = combine(
        repository.observationCount(),
        repository.observations(),
        repository.interests(),
        repository.discoveries(),
        repository.actions(),
    ) { observationCount, observations, interests, discoveries, actions ->
        LegacyHomeContent(observationCount, observations, interests, discoveries, actions)
    }

    private val unified = combine(repository.openLoops(), repository.situationBriefs()) { loops, briefs ->
        UnifiedHomeContent(loops, briefs)
    }

    private val content = combine(legacy, unified) { legacyState, unifiedState ->
        val observations = legacyState.observations
        val actions = legacyState.actions
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

        val surface = HomeSurfacePolicy.build(unifiedState.loops)

        val attention = ContextIntelligence.buildAttention(ContextIntelligence.unresolved(observations, visibleActions))
        val legacySituations = ContextIntelligence.buildSituations(observations)
        val topOfMind = PersonalIntelligenceEngine.topOfMind(observations, visibleActions)
        val contextSituations = PersonalIntelligenceEngine.buildSituations(observations, limit = 20)
        val recentChanges = unifiedState.briefs
            .filter { it.evidenceCount > 1 }
            .sortedByDescending { it.lastUpdatedAt }
            .take(6)

        HomeUiState(
            observationCount = legacyState.observationCount,
            interestCount = legacyState.interests.size,
            observations = observations,
            interests = legacyState.interests,
            discoveries = legacyState.discoveries,
            actions = visibleActions,
            readyActionCount = surface.surfaced.size,
            attention = attention,
            situations = legacySituations,
            topOfMind = topOfMind,
            contextSituations = contextSituations,
            brief = ContextIntelligence.buildDailyBrief(observations, legacyState.interests, attention),
            insights = ContextIntelligence.buildInsights(observations, legacyState.interests, legacySituations),
            openLoops = unifiedState.loops,
            needsYou = surface.needsYou,
            waitingOn = surface.waitingOn,
            upcoming = surface.upcoming,
            situationBriefs = unifiedState.briefs,
            recentChanges = recentChanges,
        )
    }.flowOn(Dispatchers.Default).catch { cause ->
        if (cause is CancellationException) throw cause
        emit(HomeUiState(error = "Could not load your context. Reopen NEXUS to retry."))
    }

    // Home is a persistent intelligence surface, not a transient screen. Keep its repository
    // subscriptions hot while the Activity is alive so captures and background enrichment cannot
    // leave For You showing stale state when the user returns from Add/Memory/Settings.
    val uiState = combine(content, error, pending) { state, message, busy ->
        state.copy(error = message ?: state.error, pending = busy)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

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
    fun resolveAction(id: String) = action(id) { repository.resolveAction(id) }
    fun startAction(id: String) = action(id) { repository.startAction(id) }
    fun completeAction(id: String) = action(id) { repository.completeAction(id) }
    fun failAction(id: String) = action(id) { repository.failAction(id) }
    fun refreshUnderstanding() = action("refresh") { repository.rebuildUnderstanding() }

    fun snoozeOpenLoop(id: String, until: Long) = action(id) { repository.snoozeOpenLoop(id, until) }
    fun resolveOpenLoop(id: String) = action(id) { repository.resolveOpenLoop(id) }
    fun dismissOpenLoop(id: String) = action(id) { repository.dismissOpenLoop(id) }
    fun recordExecution(loop: OpenLoop, contextAction: ContextAction, success: Boolean, message: String?) =
        action("execution-${loop.id}-${System.nanoTime()}") {
            repository.recordActionExecution(
                openLoopId = loop.id,
                actionKind = contextAction.kind,
                label = contextAction.label,
                payload = contextAction.payload,
                state = if (success) ExecutionState.SUCCEEDED else ExecutionState.FAILED,
                message = message,
            )
        }
}
