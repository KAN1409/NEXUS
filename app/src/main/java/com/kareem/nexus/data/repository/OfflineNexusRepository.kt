package com.kareem.nexus.data.repository

import com.kareem.nexus.core.model.*
import com.kareem.nexus.data.local.*
import com.kareem.nexus.domain.repository.NexusRepository
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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

    override fun observationCount(): Flow<Int> = dao.observeObservationCount()
    override fun interestCount(): Flow<Int> = dao.observeInterestCount()

    override suspend fun captureObservation(type: ObservationType, rawText: String, source: String?, metadataJson: String) {
        val clean = rawText.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank()) return
        val identity = if (type == ObservationType.APP_USAGE) {
            "${type.name}|${source.orEmpty()}"
        } else {
            "${type.name}|${source.orEmpty()}|$clean"
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
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

    override suspend fun rebuildUnderstanding() {
        dao.removeDuplicateUsageSnapshots()
        val rows = dao.recentObservationsOnce()
        val scores = linkedMapOf<String, Double>()

        fun add(label: String, weight: Double) {
            scores[label] = (scores[label] ?: 0.0) + weight
        }

        rows.forEach { row ->
            val text = "${row.rawText} ${row.source.orEmpty()}".lowercase()
            val usageMinutes = if (row.type == ObservationType.APP_USAGE.name) {
                Regex("""(\d+)\s*min""").find(row.rawText)?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 1.0
            } else 1.0
            val usageWeight = (usageMinutes / 30.0).coerceIn(0.4, 6.0)

            when {
                listOf("whatsapp", "truecaller", "call", "phone", "telegram", "messenger").any(text::contains) -> add("Communication", usageWeight)
                listOf("chatgpt", "cortex", "picbrain", "github", "termux", "notion", "docs").any(text::contains) -> add("AI & productivity", usageWeight)
                listOf("chrome", "search", "browser", "googlequicksearchbox").any(text::contains) -> add("Web & research", usageWeight)
                listOf("instagram", "facebook", "tiktok", "twitter", "reddit").any(text::contains) -> add("Social", usageWeight)
                listOf("netflix", "youtube", "music", "spotify", "media").any(text::contains) -> add("Entertainment", usageWeight)
                listOf("maps", "uber", "careem", "navigation").any(text::contains) -> add("Places & mobility", usageWeight)
                listOf("gallery", "photos", "camera").any(text::contains) -> add("Photos & media", usageWeight)
                listOf("talabat", "food", "restaurant").any(text::contains) -> add("Food", usageWeight)
                row.type == ObservationType.SHARED_LINK.name || row.type == ObservationType.SHARED_TEXT.name || row.type == ObservationType.MANUAL.name -> {
                    add("Saved context", 1.4)
                    if (listOf("android", "app", "kotlin", "compose", "code", "github").any(text::contains)) add("App development", 1.8)
                    if (listOf("design", "ui", "ux", "icon", "visual").any(text::contains)) add("Design", 1.5)
                }
            }
        }

        dao.clearInterests()
        dao.clearDiscoveries()
        val now = System.currentTimeMillis()
        val ranked = scores.entries.sortedByDescending { it.value }.take(6)
        val maxScore = ranked.maxOfOrNull { it.value } ?: 1.0

        ranked.forEachIndexed { index, entry ->
            val confidence = (entry.value / maxScore).coerceIn(0.25, 1.0)
            dao.upsertInterest(
                InterestEntity(
                    id = "interest_${entry.key.lowercase().replace(Regex("[^a-z0-9]+"), "_")}",
                    label = entry.key,
                    affinity = confidence,
                    momentum = (1.0 - index * 0.1).coerceAtLeast(0.35),
                    confidence = confidence,
                    saturation = 0.0,
                    updatedAt = now,
                )
            )
        }

        ranked.take(3).forEachIndexed { index, entry ->
            val supporting = rows.count { row ->
                val t = "${row.rawText} ${row.source.orEmpty()}".lowercase()
                when (entry.key) {
                    "Communication" -> listOf("whatsapp","truecaller","call","phone","telegram","messenger").any(t::contains)
                    "AI & productivity" -> listOf("chatgpt","cortex","picbrain","github","termux","notion","docs").any(t::contains)
                    "Web & research" -> listOf("chrome","search","browser","googlequicksearchbox").any(t::contains)
                    "Social" -> listOf("instagram","facebook","tiktok","twitter","reddit").any(t::contains)
                    "Entertainment" -> listOf("netflix","youtube","music","spotify","media").any(t::contains)
                    "Places & mobility" -> listOf("maps","uber","careem","navigation").any(t::contains)
                    "Photos & media" -> listOf("gallery","photos","camera").any(t::contains)
                    "Food" -> listOf("talabat","food","restaurant").any(t::contains)
                    "App development" -> listOf("android","app","kotlin","compose","code","github").any(t::contains)
                    "Design" -> listOf("design","ui","ux","icon","visual").any(t::contains)
                    else -> row.type != ObservationType.APP_USAGE.name
                }
            }
            dao.upsertDiscovery(
                DiscoveryEntity(
                    id = "discovery_interest_$index",
                    type = DiscoveryType.DISCOVERY.name,
                    title = entry.key,
                    summary = if (supporting > 1) "This theme keeps showing up across your recent context." else "This theme appeared in your recent context.",
                    whyThis = "$supporting recent signal${if (supporting == 1) "" else "s"} contributed.",
                    sourceUrl = null,
                    score = entry.value,
                    dismissed = false,
                    createdAt = now - index,
                )
            )
        }
    }

    override suspend fun seedFirstRun() = Unit
}
