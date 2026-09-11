package com.kareem.nexus.core.model

enum class AttentionLevel { LOW, MEDIUM, HIGH, URGENT }
enum class AttentionKind { REQUEST, APPOINTMENT, DELIVERY, PAYMENT, FOLLOW_UP, REMINDER, ERROR, GENERAL }
enum class SituationKind { COMMUNICATION, PROJECT, ERRAND, PURCHASE, HEALTH, TRAVEL, OTHER }

enum class SignalKind {
    REQUEST,
    PAYMENT,
    APPOINTMENT,
    DELIVERY,
    FAILURE,
    FOLLOW_UP,
    REMINDER,
    INFORMATION,
}

enum class ResolutionState { OPEN, SNOOZED, IN_PROGRESS, RESOLVED, DISMISSED }

enum class FactKind {
    PERSON,
    ORGANIZATION,
    PROJECT,
    AMOUNT,
    CURRENCY,
    DATE,
    TIME,
    LOCATION,
    ORDER,
    URL,
    OTHER,
}

enum class NexusActionKind {
    OPEN_SOURCE,
    REPLY,
    REMIND,
    ADD_TO_CALENDAR,
    NAVIGATE,
    CALL,
    COPY,
    TRACK,
    RETRY,
    MARK_RESOLVED,
}

data class ExtractedFact(
    val kind: FactKind,
    val value: String,
    val normalizedValue: String = value,
    val confidence: Double = 0.5,
)

data class ContextAction(
    val kind: NexusActionKind,
    val label: String,
    val payload: String? = null,
    val requiresApproval: Boolean = false,
)

data class ObservationUnderstanding(
    val observationId: String,
    val kind: SignalKind,
    val title: String,
    val summary: String,
    val facts: List<ExtractedFact>,
    val actions: List<ContextAction>,
    val priority: Double,
    val confidence: Double,
    val isNoise: Boolean,
    val analyzedAt: Long,
)

data class TopOfMindItem(
    val id: String,
    val observationId: String,
    val title: String,
    val summary: String,
    val kind: SignalKind,
    val priority: Double,
    val confidence: Double,
    val actions: List<ContextAction>,
    val createdAt: Long,
)

data class ContextSituation(
    val id: String,
    val title: String,
    val summary: String,
    val kind: SituationKind,
    val state: ResolutionState,
    val observationIds: List<String>,
    val facts: List<ExtractedFact>,
    val actions: List<ContextAction>,
    val priority: Double,
    val confidence: Double,
    val createdAt: Long,
    val lastUpdatedAt: Long,
)

data class AttentionItem(
    val id: String,
    val title: String,
    val summary: String,
    val source: String?,
    val kind: AttentionKind,
    val level: AttentionLevel,
    val createdAt: Long,
)

data class Situation(
    val id: String,
    val title: String,
    val summary: String,
    val kind: SituationKind,
    val observationIds: List<String>,
    val lastUpdatedAt: Long,
)

data class DailyBrief(
    val headline: String,
    val summary: String,
    val attentionCount: Int,
    val newSignals: Int,
    val topTheme: String?,
)

data class IntelligenceInsight(
    val id: String,
    val title: String,
    val summary: String,
    val evidence: String,
    val score: Double,
)
