package com.kareem.nexus.core.model

enum class OpenLoopKind {
    NEEDS_REPLY,
    NEEDS_ACTION,
    WAITING_ON,
    UPCOMING,
    PAYMENT,
    DELIVERY,
    FAILURE,
    FOLLOW_UP,
}

enum class OpenLoopState { OPEN, WAITING, SNOOZED, RESOLVED, DISMISSED }

enum class ExecutionState { STARTED, SUCCEEDED, FAILED }

data class OpenLoop(
    val id: String,
    val observationId: String,
    val situationId: String?,
    val kind: OpenLoopKind,
    val title: String,
    val detail: String,
    val party: String?,
    val source: String?,
    val state: OpenLoopState,
    val priority: Double,
    val dueAt: Long?,
    val snoozedUntil: Long?,
    val actions: List<ContextAction>,
    val createdAt: Long,
    val updatedAt: Long,
)

data class SituationBrief(
    val situationId: String,
    val title: String,
    val currentState: String,
    val whatChanged: String,
    val nextStep: String?,
    val openLoopCount: Int,
    val evidenceCount: Int,
    val priority: Double,
    val lastUpdatedAt: Long,
)

data class ActionExecution(
    val id: String,
    val openLoopId: String?,
    val actionKind: NexusActionKind,
    val label: String,
    val payload: String?,
    val state: ExecutionState,
    val message: String?,
    val createdAt: Long,
    val completedAt: Long?,
)
