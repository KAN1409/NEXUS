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
    }

    override suspend fun seedFirstRun() = Unit
}
