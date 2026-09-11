package com.kareem.nexus.core.model

enum class AttentionLevel { LOW, MEDIUM, HIGH, URGENT }
enum class AttentionKind { REQUEST, APPOINTMENT, DELIVERY, PAYMENT, FOLLOW_UP, REMINDER, ERROR, GENERAL }
enum class SituationKind { COMMUNICATION, PROJECT, ERRAND, PURCHASE, HEALTH, TRAVEL, OTHER }

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
