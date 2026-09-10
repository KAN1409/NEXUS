package com.kareem.nexus.core.model

enum class ObservationType { SHARED_TEXT, SHARED_LINK, IMAGE, NOTIFICATION, APP_USAGE, MANUAL }
enum class EntityType { TOPIC, PERSON, PRODUCT, APP, PLACE, ORGANIZATION, URL, OTHER }
enum class DiscoveryType { DISCOVERY, OPPORTUNITY, COMEBACK, ACTION, WILDCARD }
enum class ActionState { DRAFT, READY_FOR_APPROVAL, APPROVED, EXECUTING, COMPLETED, REJECTED, FAILED }
enum class FeedbackSignal { OPENED, SAVED, DISMISSED, ACTED, REJECTED, SHARED, DWELL }

data class Observation(
    val id: String,
    val type: ObservationType,
    val rawText: String,
    val source: String?,
    val createdAt: Long,
)

data class Interest(
    val id: String,
    val label: String,
    val affinity: Double,
    val momentum: Double,
    val confidence: Double,
    val updatedAt: Long,
)

data class Discovery(
    val id: String,
    val type: DiscoveryType,
    val title: String,
    val summary: String,
    val whyThis: String,
    val score: Double,
    val createdAt: Long,
)

data class PreparedAction(
    val id: String,
    val title: String,
    val description: String,
    val state: ActionState,
    val createdAt: Long,
)
