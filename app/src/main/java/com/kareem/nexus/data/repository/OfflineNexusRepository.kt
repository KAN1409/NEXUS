package com.kareem.nexus.data.repository

import com.kareem.nexus.core.model.*
import com.kareem.nexus.data.local.*
import com.kareem.nexus.domain.action.ActionLifecyclePolicy
import com.kareem.nexus.domain.intelligence.ContextIntelligence
import com.kareem.nexus.domain.intelligence.PersonalIntelligenceEngine
import com.kareem.nexus.domain.repository.NexusRepository
import java.security.MessageDigest
import java.util.UUID
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineNexusRepository @Inject constructor(
    private val dao: NexusDao,
    private val database: NexusDatabase,
) : NexusRepository {

    override fun allObservations(): Flow<List<Observation>> = dao.observeAllObservations().map { rows ->
        rows.map { Observation(it.id, ObservationType.valueOf(it.type), it.rawText, it.source, it.createdAt) }
    }

    override fun observations(): Flow<List<Observation>> = dao.observeRecentObservations().map { rows ->
        rows.map { Observation(it.id, ObservationType.valueOf(it.type), it.rawText, it.source, it.createdAt) }
    }

    override fun interests(): Flow<List<Interest>> = dao.observeTopInterests().map { rows ->
        rows.map { Interest(it.id, it.label, it.affinity, it.momentum, it.confidence, it.updatedAt) }
    }

    override fun discoveries(): Flow<List<Discovery>> = dao.observeFeed().map { rows ->
        rows.map { Discovery(it.id, DiscoveryType.valueOf(it.type), it.title, it.summary, it.whyThis, it.score, it.createdAt) }
    }

    override fun readyActions(): Flow<List<PreparedAction>> = dao.observeReadyActions().map { rows ->
        rows.map { PreparedAction(it.id, it.title, it.description, ActionState.valueOf(it.state), it.createdAt) }
    }

    override fun actions(): Flow<List<PreparedAction>> = dao.observeAllActions().map { rows ->
        rows.map { PreparedAction(it.id, it.title, it.description, ActionState.valueOf(it.state), it.createdAt) }
    }

    override fun actionEvents(): Flow<List<ActionEvent>> = dao.observeFeedback().map { rows ->
        rows.mapNotNull { row ->
            runCatching {
                ActionEvent(
                    id = row.id,
                    actionId = row.targetId,
                    signal = FeedbackSignal.valueOf(row.signal),
                    createdAt = row.createdAt,
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
        dao.addFeedback(
            FeedbackEntity(
                id = UUID.randomUUID().toString(),
                targetId = actionId,
                signal = signal.name,
                value = value,
                createdAt = now,
            )
        )
    }

    private suspend fun transitionAction(
        id: String,
        target: ActionState,
        signal: FeedbackSignal,
    ) = database.withTransaction {
        val current = dao.actionById(id) ?: return@withTransaction
        val from = runCatching { ActionState.valueOf(current.state) }.getOrNull() ?: return@withTransaction
        if (!ActionLifecyclePolicy.canTransition(from, target)) return@withTransaction

        val now = System.currentTimeMillis()
        dao.updateActionState(id, target.name, now)
        recordEvent(id, signal, now = now)
    }

    override suspend fun approveAction(id: String) =
        transitionAction(id, ActionState.APPROVED, FeedbackSignal.APPROVED)

    override suspend fun deferAction(id: String) = database.withTransaction {
        val current = dao.actionById(id) ?: return@withTransaction
        val from = runCatching { ActionState.valueOf(current.state) }.getOrNull() ?: return@withTransaction
        if (!ActionLifecyclePolicy.canTransition(from, ActionState.DRAFT)) return@withTransaction

        val now = System.currentTimeMillis()
        dao.deferAction(id, now)
        recordEvent(id, FeedbackSignal.DEFERRED, value = 0.5, now = now)
    }

    override suspend fun rejectAction(id: String) =
        transitionAction(id, ActionState.REJECTED, FeedbackSignal.REJECTED)

    override suspend fun resolveAction(id: String) =
        transitionAction(id, ActionState.REJECTED, FeedbackSignal.RESOLVED)

    override suspend fun startAction(id: String) =
        transitionAction(id, ActionState.EXECUTING, FeedbackSignal.STARTED)

    override suspend fun completeAction(id: String) =
        transitionAction(id, ActionState.COMPLETED, FeedbackSignal.COMPLETED)

    override suspend fun failAction(id: String) =
        transitionAction(id, ActionState.FAILED, FeedbackSignal.FAILED)

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
                val key = metadata?.optString("key").orEmpty().takeIf { it.isNotBlank() }
                val eventIdentity = postedAt?.toString() ?: key.orEmpty()
                if (eventIdentity.isBlank()) {
                    "${type.name}|${source.orEmpty()}|$clean"
                } else {
                    "${type.name}|${source.orEmpty()}|$clean|$eventIdentity"
                }
            }
            else -> "${type.name}|${source.orEmpty()}|$clean"
        }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) }

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

        if (type == ObservationType.APP_USAGE && source != null) {
            dao.deleteOtherUsageSnapshots(source, digest)
        }
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

        val deferred = dao.deferredActionsReadyToResurface(
            cutoff = now - ActionLifecyclePolicy.DEFER_DURATION_MS,
        )
        deferred.forEach { action ->
            val from = runCatching { ActionState.valueOf(action.state) }.getOrNull()
            if (from == ActionState.DRAFT && ActionLifecyclePolicy.canTransition(from, ActionState.READY_FOR_APPROVAL)) {
                dao.updateActionState(action.id, ActionState.READY_FOR_APPROVAL.name, now)
                recordEvent(action.id, FeedbackSignal.RESURFACED, now = now)
            }
        }

        val rows = dao.recentObservationsOnce()
        val domainRows = rows.mapNotNull { row ->
            runCatching {
                Observation(
                    id = row.id,
                    type = ObservationType.valueOf(row.type),
                    rawText = row.rawText,
                    source = row.source,
                    createdAt = row.createdAt,
                )
            }.getOrNull()
        }

        materializePersonalIntelligence(domainRows, now)

        val scores = linkedMapOf<String, Double>()

        fun add(label: String, weight: Double) {
            scores[label] = (scores[label] ?: 0.0) + weight
        }

        rows.forEach { row ->
            val text = "${row.rawText} ${row.source.orEmpty()}".lowercase()
            val usageMinutes = if (row.type == ObservationType.APP_USAGE.name) {
                Regex("""(\d+)\s*min""")
                    .find(row.rawText)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toDoubleOrNull()
                    ?: 1.0
            } else {
                1.0
            }
            val usageWeight = (usageMinutes / 30.0).coerceIn(0.35, 3.0)

            when {
                listOf("whatsapp", "truecaller", "call", "phone", "telegram", "messenger").any(text::contains) ->
                    add("Communication", usageWeight)
                listOf("chatgpt", "cortex", "picbrain", "github", "termux", "notion", "docs").any(text::contains) ->
                    add("AI & productivity", usageWeight)
                listOf("chrome", "search", "browser", "googlequicksearchbox").any(text::contains) ->
                    add("Web & research", usageWeight)
                listOf("instagram", "facebook", "tiktok", "twitter", "reddit").any(text::contains) ->
                    add("Social", usageWeight)
                listOf("netflix", "youtube", "music", "spotify", "media").any(text::contains) ->
                    add("Entertainment", usageWeight)
                listOf("maps", "uber", "careem", "navigation").any(text::contains) ->
                    add("Places & mobility", usageWeight)
                listOf("gallery", "photos", "camera").any(text::contains) ->
                    add("Photos & media", usageWeight)
                listOf("talabat", "food", "restaurant").any(text::contains) ->
                    add("Food", usageWeight)
                row.type in setOf(
                    ObservationType.SHARED_LINK.name,
                    ObservationType.SHARED_TEXT.name,
                    ObservationType.MANUAL.name,
                ) -> {
                    add("Saved context", 1.4)
                    if (listOf("android", "app", "kotlin", "compose", "code", "github").any(text::contains)) {
                        add("App development", 1.8)
                    }
                    if (listOf("design", "ui", "ux", "icon", "visual").any(text::contains)) {
                        add("Design", 1.5)
                    }
                }
            }
        }

        dao.clearInterests()
        dao.clearDiscoveries()

        val ranked = scores.entries.sortedByDescending { it.value }.take(6)
        val maxScore = ranked.maxOfOrNull { it.value } ?: 1.0

        ranked.forEachIndexed { index, entry ->
            val slug = entry.key.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            val confidence = (entry.value / maxScore).coerceIn(0.25, 1.0)
            dao.upsertInterest(
                InterestEntity(
                    id = "interest_$slug",
                    label = entry.key,
                    affinity = confidence,
                    momentum = (1.0 - index * 0.1).coerceAtLeast(0.35),
                    confidence = confidence,
                    saturation = 0.0,
                    updatedAt = now,
                )
            )
        }

        ranked.take(3).forEach { entry ->
            val slug = entry.key.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            val supporting = rows
                .filter { row ->
                    val t = "${row.rawText} ${row.source.orEmpty()}".lowercase()
                    when (entry.key) {
                        "Communication" -> listOf("whatsapp", "truecaller", "call", "phone", "telegram", "messenger").any(t::contains)
                        "AI & productivity" -> listOf("chatgpt", "cortex", "picbrain", "github", "termux", "notion", "docs").any(t::contains)
                        "Web & research" -> listOf("chrome", "search", "browser", "googlequicksearchbox").any(t::contains)
                        "Social" -> listOf("instagram", "facebook", "tiktok", "twitter", "reddit").any(t::contains)
                        "Entertainment" -> listOf("netflix", "youtube", "music", "spotify", "media").any(t::contains)
                        "Places & mobility" -> listOf("maps", "uber", "careem", "navigation").any(t::contains)
                        "Photos & media" -> listOf("gallery", "photos", "camera").any(t::contains)
                        "Food" -> listOf("talabat", "food", "restaurant").any(t::contains)
                        "App development" -> listOf("android", "app", "kotlin", "compose", "code", "github").any(t::contains)
                        "Design" -> listOf("design", "ui", "ux", "icon", "visual").any(t::contains)
                        else -> row.type != ObservationType.APP_USAGE.name
                    }
                }
                .map { row -> if (row.type == ObservationType.APP_USAGE.name) "usage:${row.source}" else row.id }
                .distinct()
                .size

            dao.upsertDiscovery(
                DiscoveryEntity(
                    id = "discovery_$slug",
                    type = DiscoveryType.DISCOVERY.name,
                    title = entry.key,
                    summary = if (supporting > 1) {
                        "This theme keeps showing up across your recent context."
                    } else {
                        "This theme appeared in your recent context."
                    },
                    whyThis = "$supporting recent signal${if (supporting == 1) "" else "s"} contributed.",
                    sourceUrl = null,
                    score = entry.value,
                    dismissed = false,
                    createdAt = now,
                )
            )
        }

        val actionable = domainRows
            .filter { it.type in setOf(ObservationType.NOTIFICATION, ObservationType.MANUAL, ObservationType.SHARED_TEXT) }
            .filter { it.createdAt >= now - 7L * 24 * 60 * 60 * 1000 }
            .map { it to PersonalIntelligenceEngine.interpret(it, now) }
            .filter { (_, understanding) ->
                !understanding.isNoise &&
                    understanding.kind != SignalKind.INFORMATION &&
                    understanding.actions.isNotEmpty() &&
                    understanding.priority >= 0.45
            }

        actionable.forEach { (observation, understanding) ->
            val id = "action_signal_${observation.id}"
            if (dao.actionById(id) == null && dao.actionById("action_commitment_${observation.id}") == null) {
                dao.upsertAction(
                    ActionEntity(
                        id = id,
                        title = understanding.title,
                        description = understanding.summary,
                        state = ActionState.READY_FOR_APPROVAL.name,
                        payloadJson = JSONObject()
                            .put("source", observation.source)
                            .put("observationId", observation.id)
                            .put("kind", understanding.kind.name)
                            .put("primaryAction", understanding.actions.firstOrNull()?.kind?.name)
                            .toString(),
                        createdAt = observation.createdAt,
                        updatedAt = now,
                    )
                )
                recordEvent(id, FeedbackSignal.SUGGESTED, now = now)
            }
        }
    }

    private suspend fun materializePersonalIntelligence(observations: List<Observation>, now: Long) {
        dao.clearObservationUnderstandings()
        dao.clearSituationMembers()
        dao.clearSituations()

        observations
            .filter { it.type != ObservationType.APP_USAGE }
            .forEach { observation ->
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

        PersonalIntelligenceEngine.buildSituations(observations, now = now).forEach { situation ->
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
            dao.upsertSituationMembers(
                situation.observationIds.map { observationId ->
                    SituationMemberEntity(situation.id, observationId)
                }
            )
        }
    }

    private fun factsJson(facts: List<ExtractedFact>): String = JSONArray().apply {
        facts.forEach { fact ->
            put(
                JSONObject()
                    .put("kind", fact.kind.name)
                    .put("value", fact.value)
                    .put("normalizedValue", fact.normalizedValue)
                    .put("confidence", fact.confidence)
            )
        }
    }.toString()

    private fun actionsJson(actions: List<ContextAction>): String = JSONArray().apply {
        actions.forEach { action ->
            put(
                JSONObject()
                    .put("kind", action.kind.name)
                    .put("label", action.label)
                    .put("payload", action.payload)
                    .put("requiresApproval", action.requiresApproval)
            )
        }
    }.toString()

    override suspend fun seedFirstRun() = Unit
}
