package com.kareem.nexus.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "observations", indices = [Index("createdAt")])
data class ObservationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val rawText: String,
    val normalizedText: String,
    val source: String?,
    val metadataJson: String,
    val createdAt: Long,
)

@Entity(tableName = "interests", indices = [Index("updatedAt")])
data class InterestEntity(
    @PrimaryKey val id: String,
    val label: String,
    val affinity: Double,
    val momentum: Double,
    val confidence: Double,
    val saturation: Double,
    val updatedAt: Long,
)

@Entity(tableName = "entities")
data class KnowledgeEntity(
    @PrimaryKey val id: String,
    val type: String,
    val canonicalName: String,
    val aliasesJson: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
)

@Entity(tableName = "memories", indices = [Index("createdAt")])
data class MemoryEntity(
    @PrimaryKey val id: String,
    val observationId: String,
    val summary: String,
    val searchableText: String,
    val importance: Double,
    val createdAt: Long,
)

@Entity(tableName = "discoveries", indices = [Index("score"), Index("createdAt")])
data class DiscoveryEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val summary: String,
    val whyThis: String,
    val sourceUrl: String?,
    val score: Double,
    val dismissed: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "actions", indices = [Index("state"), Index("createdAt")])
data class ActionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val state: String,
    val payloadJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "feedback", indices = [Index("targetId"), Index("createdAt")])
data class FeedbackEntity(
    @PrimaryKey val id: String,
    val targetId: String,
    val signal: String,
    val value: Double,
    val createdAt: Long,
)

@Entity(
    tableName = "observation_understanding",
    indices = [Index("kind"), Index("priority"), Index("analyzedAt")],
)
data class ObservationUnderstandingEntity(
    @PrimaryKey val observationId: String,
    val kind: String,
    val title: String,
    val summary: String,
    val factsJson: String,
    val actionsJson: String,
    val priority: Double,
    val confidence: Double,
    val isNoise: Boolean,
    val analyzedAt: Long,
)

@Entity(
    tableName = "situations",
    indices = [Index("state"), Index("priority"), Index("lastUpdatedAt")],
)
data class SituationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val kind: String,
    val state: String,
    val factsJson: String,
    val actionsJson: String,
    val priority: Double,
    val confidence: Double,
    val createdAt: Long,
    val lastUpdatedAt: Long,
)

@Entity(
    tableName = "situation_members",
    primaryKeys = ["situationId", "observationId"],
    indices = [Index("situationId"), Index("observationId")],
)
data class SituationMemberEntity(
    val situationId: String,
    val observationId: String,
)

@Entity(
    tableName = "open_loops",
    indices = [Index("state"), Index("kind"), Index("priority"), Index("updatedAt"), Index("situationId")],
)
data class OpenLoopEntity(
    @PrimaryKey val id: String,
    val observationId: String,
    val situationId: String?,
    val kind: String,
    val title: String,
    val detail: String,
    val party: String?,
    val source: String?,
    val state: String,
    val priority: Double,
    val dueAt: Long?,
    val snoozedUntil: Long?,
    val actionsJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "situation_snapshots",
    indices = [Index("priority"), Index("lastUpdatedAt")],
)
data class SituationSnapshotEntity(
    @PrimaryKey val situationId: String,
    val title: String,
    val currentState: String,
    val whatChanged: String,
    val nextStep: String?,
    val openLoopCount: Int,
    val evidenceCount: Int,
    val priority: Double,
    val lastUpdatedAt: Long,
)

@Entity(
    tableName = "action_executions",
    indices = [Index("openLoopId"), Index("state"), Index("createdAt")],
)
data class ActionExecutionEntity(
    @PrimaryKey val id: String,
    val openLoopId: String?,
    val actionKind: String,
    val label: String,
    val payload: String?,
    val state: String,
    val message: String?,
    val createdAt: Long,
    val completedAt: Long?,
)
