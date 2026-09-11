package com.kareem.nexus.data.repository

import androidx.room.withTransaction
import com.kareem.nexus.core.model.*
import com.kareem.nexus.data.local.*
import com.kareem.nexus.domain.action.ActionLifecyclePolicy
import com.kareem.nexus.domain.intelligence.ContextIntelligence
import com.kareem.nexus.domain.intelligence.PersonalIntelligenceEngine
import com.kareem.nexus.domain.intelligence.UnifiedIntelligenceEngine
import com.kareem.nexus.domain.repository.NexusRepository
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class OfflineNexusRepository @Inject constructor(
    private val dao: NexusDao,
    private val database: NexusDatabase,
) : NexusRepository {

    override fun allObservations(): Flow<List<Observation>> = dao.observeAllObservations().map(::mapObservations)
    override fun observations(): Flow<List<Observation>> = dao.observeRecentObservations().map(::mapObservations)

    override fun interests(): Flow<List<Interest>> = dao.observeTopInterests().map { rows ->
        rows.map { Interest(it.id, it.label, it.affinity, it.momentum, it.confidence, it.updatedAt) }
    }

    override fun discoveries(): Flow<List<Discovery>> = dao.observeFeed().map { rows ->
        rows.map { Discovery(it.id, DiscoveryType.valueOf(it.type), it.title, it.summary, it.whyThis, it.score, it.createdAt) }
    }

    override fun readyActions(): Flow<List<PreparedAction>> = dao.observeReadyActions().map(::mapActions)
    override fun actions(): Flow<List<PreparedAction>> = dao.observeAllActions().map(::mapActions)

    override fun actionEvents(): Flow<List<ActionEvent>> = dao.observeFeedback().map { rows ->
        rows.mapNotNull { row ->
            runCatching { ActionEvent(row.id, row.targetId, FeedbackSignal.valueOf(row.signal), row.createdAt) }.getOrNull()
        }
    }

    override fun openLoops(): Flow<List<OpenLoop>> = dao.observeOpenLoops().map { rows -> rows.mapNotNull(::mapOpenLoop) }

    override fun situationBriefs(): Flow<List<SituationBrief>> = dao.observeSituationSnapshots().map { rows ->
        rows.map { row ->
            SituationBrief(
                situationId = row.situationId,
                title = row.title,
                currentState = row.currentState,
                whatChanged = row.whatChanged,
                nextStep = row.nextStep,
                openLoopCount = row.openLoopCount,
                evidenceCount = row.evidenceCount,
                priority = row.priority,
                lastUpdatedAt = row.lastUpdatedAt,
            )
        }
    }

    override fun actionExecutions(): Flow<List<ActionExecution>> = dao.observeActionExecutions().map { rows ->
        rows.mapNotNull { row ->
            runCatching {
                ActionExecution(
                    id = row.id,
                    openLoopId = row.openLoopId,
                    actionKind = NexusActionKind.valueOf(row.actionKind),
                    label = row.label,
                    payload = row.payload,
                    state = ExecutionState.valueOf(row.state),
                    message = row.message,
                    createdAt = row.createdAt,
                    completedAt = row.completedAt,
                )
            }.getOrNull()
        }
    }

    override fun observationCount(): Flow<Int> = dao.observeObservationCount()
    override fun interestCount(): Flow<Int> = dao.observeInterestCount()

    private suspend fun recordEvent(
        actionId: String,
        signal: FeedbackSignal,
        value: Double = 1.0,
        now: Long = System.currentTimeMillis(),
    ) {
        dao.addFeedback(FeedbackEntity(UUID.randomUUID().toString(), actionId, signal.name, value, now))
    }

    private suspend fun transitionAction(id: String, target: ActionState, signal: FeedbackSignal) = database.withTransaction {
        val current = dao.actionById(id) ?: return@withTransaction
        val from = runCatching { ActionState.valueOf(current.state) }.getOrNull() ?: return@withTransaction
        if (!ActionLifecyclePolicy.canTransition(from, target)) return@withTransaction
        val now = System.currentTimeMillis()
        dao.updateActionState(id, target.name, now)
        recordEvent(id, signal, now = now)
    }

    override suspend fun approveAction(id: String) = transitionAction(id, ActionState.APPROVED, FeedbackSignal.APPROVED)

    override suspend fun deferAction(id: String) = database.withTransaction {
        val current = dao.actionById(id) ?: return@withTransaction
        val from = runCatching { ActionState.valueOf(current.state) }.getOrNull() ?: return@withTransaction
        if (!ActionLifecyclePolicy.canTransition(from, ActionState.DRAFT)) return@withTransaction
        val now = System.currentTimeMillis()
        dao.deferAction(id, now)
        recordEvent(id, FeedbackSignal.DEFERRED, value = 0.5, now = now)
    }

    override suspend fun rejectAction(id: String) = transitionAction(id, ActionState.REJECTED, FeedbackSignal.REJECTED)
    override suspend fun resolveAction(id: String) = transitionAction(id, ActionState.REJECTED, FeedbackSignal.RESOLVED)
    override suspend fun startAction(id: String) = transitionAction(id, ActionState.EXECUTING, FeedbackSignal.STARTED)
    override suspend fun completeAction(id: String) = transitionAction(id, ActionState.COMPLETED, FeedbackSignal.COMPLETED)
    override suspend fun failAction(id: String) = transitionAction(id, ActionState.FAILED, FeedbackSignal.FAILED)

    override suspend fun snoozeOpenLoop(id: String, until: Long) = database.withTransaction {
        val loop = dao.openLoopById(id) ?: return@withTransaction
        val now = System.currentTimeMillis()
        dao.updateOpenLoopState(id, OpenLoopState.SNOOZED.name, until, now)
        val actionId = "action_signal_${loop.observationId}"
        val action = dao.actionById(actionId)
        if (action != null) {
            val from = runCatching { ActionState.valueOf(action.state) }.getOrNull()
            if (from != null && ActionLifecyclePolicy.canTransition(from, ActionState.DRAFT)) {
                dao.deferAction(actionId, now)
                recordEvent(actionId, FeedbackSignal.DEFERRED, 0.5, now)
            }
        }
    }

    override suspend fun resolveOpenLoop(id: String) = closeOpenLoop(id, OpenLoopState.RESOLVED, FeedbackSignal.RESOLVED)
    override suspend fun dismissOpenLoop(id: String) = closeOpenLoop(id, OpenLoopState.DISMISSED, FeedbackSignal.DISMISSED)

    private suspend fun closeOpenLoop(id: String, state: OpenLoopState, signal: FeedbackSignal) = database.withTransaction {
        val loop = dao.openLoopById(id) ?: return@withTransaction
        val now = System.currentTimeMillis()
        dao.updateOpenLoopState(id, state.name, null, now)
        val actionId = "action_signal_${loop.observationId}"
        val action = dao.actionById(actionId)
        if (action != null) {
            val from = runCatching { ActionState.valueOf(action.state) }.getOrNull()
            if (from != null && ActionLifecyclePolicy.canTransition(from, ActionState.REJECTED)) {
                dao.updateActionState(actionId, ActionState.REJECTED.name, now)
                recordEvent(actionId, signal, now = now)
            }
        }
    }

    override suspend fun recordActionExecution(
        openLoopId: String?,
        actionKind: NexusActionKind,
        label: String,
        payload: String?,
        state: ExecutionState,
        message: String?,
    ) {
        val now = System.currentTimeMillis()
        dao.upsertActionExecution(
            ActionExecutionEntity(
                id = UUID.randomUUID().toString(),
                openLoopId = openLoopId,
                actionKind = actionKind.name,
                label = label,
                payload = payload,
                state = state.name,
                message = message,
                createdAt = now,
                completedAt = now.takeIf { state != ExecutionState.STARTED },
            )
        )
    }

    override suspend fun captureObservation(
        type: ObservationType,
        rawText: String,
        source: String?,
        metadataJson: String,
    ) = database.withTransaction {
        val clean = rawText.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank()) return@withTransaction

        val identity = when (type) {
            ObservationType.APP_USAGE -> "${type.name}|${source.orEmpty()}"
            ObservationType.NOTIFICATION -> {
                val metadata = runCatching { JSONObject(metadataJson) }.getOrNull()
                val postedAt = metadata?.optLong("postedAt", 0L)?.takeIf { it > 0L }
                val key = metadata?.optString("key").orEmpty().takeIf(String::isNotBlank)
                val eventIdentity = postedAt?.toString() ?: key.orEmpty()
                if (eventIdentity.isBlank()) "${type.name}|${source.orEmpty()}|$clean"
                else "${type.name}|${source.orEmpty()}|$clean|$eventIdentity"
            }
            else -> "${type.name}|${source.orEmpty()}|$clean"
        }

        val digest = sha256(identity)
        val existing = dao.observationById(digest)
        if (existing != null && type != ObservationType.APP_USAGE) return@withTransaction
        dao.upsertObservation(
            ObservationEntity(
                id = digest,
                type = type.name,
                rawText = rawText.trim(),
                normalizedText = ContextIntelligence.normalize(clean),
                source = source,
                metadataJson = metadataJson,
                createdAt = System.currentTimeMillis(),
            )
        )
        if (type == ObservationType.APP_USAGE && source != null) dao.deleteOtherUsageSnapshots(source, digest)
    }

    override suspend fun pruneUsageSources(sources: List<String>) {
        if (sources.isNotEmpty()) dao.deleteUsageOutsideSources(sources)
    }

    override suspend fun rebuildUnderstanding() = withContext(Dispatchers.Default) {
        database.withTransaction { rebuildTransaction() }
    }

    private suspend fun rebuildTransaction() {
        val now = System.currentTimeMillis()
        dao.removeDuplicateUsageSnapshots()
        dao.retireLegacyFocusActions(now)
        dao.wakeSnoozedOpenLoops(now, now)

        dao.deferredActionsReadyToResurface(now - ActionLifecyclePolicy.DEFER_DURATION_MS).forEach { action ->
            val from = runCatching { ActionState.valueOf(action.state) }.getOrNull()
            if (from == ActionState.DRAFT && ActionLifecyclePolicy.canTransition(from, ActionState.READY_FOR_APPROVAL)) {
                dao.updateActionState(action.id, ActionState.READY_FOR_APPROVAL.name, now)
                recordEvent(action.id, FeedbackSignal.RESURFACED, now = now)
            }
        }

        val rows = dao.recentObservationsOnce(300)
        val observations = mapObservations(rows)
        dao.clearInterests()
        dao.clearDiscoveries()

        val situations = materializeUnderstandingAndSituations(observations, now)
        val observationToSituation = buildMap<String, String> {
            situations.forEach { situation -> situation.observationIds.forEach { put(it, situation.id) } }
        }

        val existingLoops = dao.openLoopsOnce().associateBy { it.id }
        val existingActions = dao.actionsOnce().associateBy { it.id }
        val materialized = mutableListOf<OpenLoop>()
        val activeIds = mutableListOf<String>()

        observations.filter { it.type != ObservationType.APP_USAGE }.forEach { observation ->
            val understanding = PersonalIntelligenceEngine.interpret(observation, now)
            persistMemoryAndEntities(observation, understanding)
            val candidate = UnifiedIntelligenceEngine.deriveOpenLoop(
                observation = observation,
                understanding = understanding,
                situationId = observationToSituation[observation.id],
                now = now,
            ) ?: return@forEach

            activeIds += candidate.id
            val existing = existingLoops[candidate.id]
            val action = existingActions["action_signal_${observation.id}"]
            val actionState = action?.state?.let { runCatching { ActionState.valueOf(it) }.getOrNull() }
            val persistedState = when {
                existing?.state == OpenLoopState.RESOLVED.name -> OpenLoopState.RESOLVED
                existing?.state == OpenLoopState.DISMISSED.name -> OpenLoopState.DISMISSED
                existing?.state == OpenLoopState.SNOOZED.name && (existing.snoozedUntil ?: 0L) > now -> OpenLoopState.SNOOZED
                actionState == ActionState.COMPLETED -> OpenLoopState.RESOLVED
                actionState == ActionState.REJECTED -> OpenLoopState.DISMISSED
                else -> candidate.state
            }
            val persisted = candidate.copy(
                state = persistedState,
                snoozedUntil = existing?.snoozedUntil?.takeIf { persistedState == OpenLoopState.SNOOZED },
                createdAt = existing?.createdAt ?: candidate.createdAt,
            )
            dao.upsertOpenLoop(persisted.toEntity())
            materialized += persisted

            val actionId = "action_signal_${observation.id}"
            if (dao.actionById(actionId) == null && dao.actionById("action_commitment_${observation.id}") == null) {
                dao.upsertAction(
                    ActionEntity(
                        id = actionId,
                        title = persisted.title,
                        description = persisted.detail,
                        state = ActionState.READY_FOR_APPROVAL.name,
                        payloadJson = JSONObject()
                            .put("source", persisted.source)
                            .put("observationId", observation.id)
                            .put("kind", persisted.kind.name)
                            .put("primaryAction", persisted.actions.firstOrNull()?.kind?.name)
                            .toString(),
                        createdAt = observation.createdAt,
                        updatedAt = now,
                    )
                )
                recordEvent(actionId, FeedbackSignal.SUGGESTED, now = now)
            }
        }

        if (activeIds.isEmpty()) dao.clearActiveOpenLoops() else dao.deleteActiveOpenLoopsNotIn(activeIds)
        dao.clearSituationSnapshots()
        situations.forEach { situation ->
            val brief = UnifiedIntelligenceEngine.buildSituationBrief(situation, materialized, observations, now)
            dao.upsertSituationSnapshot(brief.toEntity())
        }
    }

    private suspend fun materializeUnderstandingAndSituations(
        observations: List<Observation>,
        now: Long,
    ): List<ContextSituation> {
        dao.clearObservationUnderstandings()
        dao.clearSituationMembers()
        dao.clearSituations()
        observations.filter { it.type != ObservationType.APP_USAGE }.forEach { observation ->
            val understanding = PersonalIntelligenceEngine.interpret(observation, now)
            dao.upsertObservationUnderstanding(
                ObservationUnderstandingEntity(
                    observationId = observation.id,
                    kind = understanding.kind.name,
                    title = understanding.title,
                    summary = understanding.summary,
                    factsJson = factsJson(understanding.facts),
                    actionsJson = actionsJson(understanding.actions),
                    priority = understanding.priority,
                    confidence = understanding.confidence,
                    isNoise = understanding.isNoise,
                    analyzedAt = understanding.analyzedAt,
                )
            )
        }

        val situations = PersonalIntelligenceEngine.buildSituations(observations, limit = 20, now = now)
        situations.forEach { situation ->
            dao.upsertSituation(
                SituationEntity(
                    id = situation.id,
                    title = situation.title,
                    summary = situation.summary,
                    kind = situation.kind.name,
                    state = situation.state.name,
                    factsJson = factsJson(situation.facts),
                    actionsJson = actionsJson(situation.actions),
                    priority = situation.priority,
                    confidence = situation.confidence,
                    createdAt = situation.createdAt,
                    lastUpdatedAt = situation.lastUpdatedAt,
                )
            )
            dao.upsertSituationMembers(situation.observationIds.map { SituationMemberEntity(situation.id, it) })
        }
        return situations
    }

    private suspend fun persistMemoryAndEntities(
        observation: Observation,
        understanding: ObservationUnderstanding,
    ) {
        dao.upsertMemory(
            MemoryEntity(
                id = "memory_${observation.id}",
                observationId = observation.id,
                summary = understanding.summary,
                searchableText = UnifiedIntelligenceEngine.searchableText(observation, understanding),
                importance = UnifiedIntelligenceEngine.memoryImportance(understanding),
                createdAt = observation.createdAt,
            )
        )
        understanding.facts
            .filter { it.kind in setOf(FactKind.PERSON, FactKind.ORGANIZATION, FactKind.PROJECT, FactKind.LOCATION) }
            .forEach { fact ->
                val id = "entity_${sha256("${fact.kind}:${fact.normalizedValue}").take(20)}"
                val previous = dao.knowledgeById(id)
                dao.upsertKnowledge(
                    KnowledgeEntity(
                        id = id,
                        type = fact.kind.name,
                        canonicalName = fact.value,
                        aliasesJson = previous?.aliasesJson ?: "[]",
                        firstSeenAt = previous?.firstSeenAt ?: observation.createdAt,
                        lastSeenAt = maxOf(previous?.lastSeenAt ?: 0L, observation.createdAt),
                    )
                )
            }
    }

    private fun OpenLoop.toEntity() = OpenLoopEntity(
        id = id,
        observationId = observationId,
        situationId = situationId,
        kind = kind.name,
        title = title,
        detail = detail,
        party = party,
        source = source,
        state = state.name,
        priority = priority,
        dueAt = dueAt,
        snoozedUntil = snoozedUntil,
        actionsJson = actionsJson(actions),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun SituationBrief.toEntity() = SituationSnapshotEntity(
        situationId = situationId,
        title = title,
        currentState = currentState,
        whatChanged = whatChanged,
        nextStep = nextStep,
        openLoopCount = openLoopCount,
        evidenceCount = evidenceCount,
        priority = priority,
        lastUpdatedAt = lastUpdatedAt,
    )

    private fun mapOpenLoop(row: OpenLoopEntity): OpenLoop? = runCatching {
        OpenLoop(
            id = row.id,
            observationId = row.observationId,
            situationId = row.situationId,
            kind = OpenLoopKind.valueOf(row.kind),
            title = row.title,
            detail = row.detail,
            party = row.party,
            source = row.source,
            state = OpenLoopState.valueOf(row.state),
            priority = row.priority,
            dueAt = row.dueAt,
            snoozedUntil = row.snoozedUntil,
            actions = parseActions(row.actionsJson),
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }.getOrNull()

    private fun mapObservations(rows: List<ObservationEntity>): List<Observation> = rows.mapNotNull { row ->
        runCatching { Observation(row.id, ObservationType.valueOf(row.type), row.rawText, row.source, row.createdAt) }.getOrNull()
    }

    private fun mapActions(rows: List<ActionEntity>): List<PreparedAction> = rows.mapNotNull { row ->
        runCatching { PreparedAction(row.id, row.title, row.description, ActionState.valueOf(row.state), row.createdAt) }.getOrNull()
    }

    private fun factsJson(facts: List<ExtractedFact>): String = JSONArray().apply {
        facts.forEach { fact ->
            put(JSONObject().put("kind", fact.kind.name).put("value", fact.value)
                .put("normalizedValue", fact.normalizedValue).put("confidence", fact.confidence))
        }
    }.toString()

    private fun actionsJson(actions: List<ContextAction>): String = JSONArray().apply {
        actions.forEach { action ->
            put(JSONObject().put("kind", action.kind.name).put("label", action.label)
                .put("payload", action.payload).put("requiresApproval", action.requiresApproval))
        }
    }.toString()

    private fun parseActions(json: String): List<ContextAction> = runCatching {
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    ContextAction(
                        kind = NexusActionKind.valueOf(item.getString("kind")),
                        label = item.optString("label").ifBlank { item.getString("kind") },
                        payload = item.optString("payload").takeIf { it.isNotBlank() && it != "null" },
                        requiresApproval = item.optBoolean("requiresApproval", false),
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    override suspend fun seedFirstRun() = Unit
}
