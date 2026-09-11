package com.kareem.nexus.data.repository

import com.kareem.nexus.core.model.*
import com.kareem.nexus.data.local.*
import com.kareem.nexus.domain.action.ActionLifecyclePolicy
import com.kareem.nexus.domain.repository.NexusRepository
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineNexusRepository @Inject constructor(
    private val dao: NexusDao,
) : NexusRepository {

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
                id = "event_${actionId}_${signal.name}_$now",
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
    ) {
        val current = dao.actionById(id) ?: return
        val from = runCatching { ActionState.valueOf(current.state) }.getOrNull() ?: return
        if (!ActionLifecyclePolicy.canTransition(from, target)) return

        val now = System.currentTimeMillis()
        dao.updateActionState(id, target.name, now)
        recordEvent(id, signal, now = now)
    }

    override suspend fun approveAction(id: String) =
        transitionAction(id, ActionState.APPROVED, FeedbackSignal.APPROVED)

    override suspend fun deferAction(id: String) {
        val current = dao.actionById(id) ?: return
        val from = runCatching { ActionState.valueOf(current.state) }.getOrNull() ?: return
        if (!ActionLifecyclePolicy.canTransition(from, ActionState.DRAFT)) return

        val now = System.currentTimeMillis()
        dao.deferAction(id, now)
        recordEvent(id, FeedbackSignal.DEFERRED, value = 0.5, now = now)
    }

    override suspend fun rejectAction(id: String) =
        transitionAction(id, ActionState.REJECTED, FeedbackSignal.REJECTED)

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
    ) {
        val clean = rawText.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank()) return

        val identity = if (type == ObservationType.APP_USAGE) {
            "${type.name}|${source.orEmpty()}"
        } else {
            "${type.name}|${source.orEmpty()}|$clean"
        }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) }

        dao.upsertObservation(
            ObservationEntity(
                id = digest,
                type = type.name,
                rawText = rawText.trim(),
                normalizedText = clean.lowercase(),
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

    override suspend fun rebuildUnderstanding() {
        val now = System.currentTimeMillis()

        dao.removeDuplicateUsageSnapshots()

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
                .map { row ->
                    if (row.type == ObservationType.APP_USAGE.name) "usage:${row.source}" else row.id
                }
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

        val commitmentSignals = rows
            .filter { it.type == ObservationType.NOTIFICATION.name }
            .filter { row ->
                val t = row.rawText.lowercase()
                listOf(
                    "appointment", "tomorrow", "reminder",
                    "موعد", "غداً", "غدا", "بكره", "بكرة",
                ).any(t::contains)
            }
            .take(2)

        commitmentSignals.forEach { row ->
            val id = "action_commitment_${row.id}"
            if (dao.actionById(id) == null) {
                dao.upsertAction(
                    ActionEntity(
                        id = id,
                        title = "Review upcoming commitment",
                        description = row.rawText.take(180),
                        state = ActionState.READY_FOR_APPROVAL.name,
                        payloadJson = "{}",
                        createdAt = row.createdAt,
                        updatedAt = now,
                    )
                )
                recordEvent(id, FeedbackSignal.SUGGESTED, now = now)
            }
        }

        ranked.firstOrNull()?.let { top ->
            val slug = top.key.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            val id = "action_focus_$slug"
            if (dao.actionById(id) == null) {
                dao.upsertAction(
                    ActionEntity(
                        id = id,
                        title = "Review ${top.key}",
                        description = "NEXUS detected this as your strongest recent pattern and prepared it for review.",
                        state = ActionState.READY_FOR_APPROVAL.name,
                        payloadJson = "{}",
                        createdAt = now,
                        updatedAt = now,
                    )
                )
                recordEvent(id, FeedbackSignal.SUGGESTED, now = now)
            }
        }
    }

    override suspend fun seedFirstRun() = Unit
}
