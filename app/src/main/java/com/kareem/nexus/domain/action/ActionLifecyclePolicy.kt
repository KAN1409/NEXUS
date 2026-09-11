package com.kareem.nexus.domain.action

import com.kareem.nexus.core.model.ActionState

object ActionLifecyclePolicy {
    const val DEFER_DURATION_MS: Long = 24L * 60L * 60L * 1000L

    fun canTransition(from: ActionState, to: ActionState): Boolean = when (from) {
        ActionState.DRAFT -> to == ActionState.READY_FOR_APPROVAL
        ActionState.READY_FOR_APPROVAL -> to in setOf(
            ActionState.APPROVED,
            ActionState.DRAFT,
            ActionState.REJECTED,
        )
        ActionState.APPROVED -> to in setOf(
            ActionState.EXECUTING,
            ActionState.REJECTED,
        )
        ActionState.EXECUTING -> to in setOf(
            ActionState.COMPLETED,
            ActionState.FAILED,
        )
        ActionState.COMPLETED,
        ActionState.REJECTED,
        ActionState.FAILED -> false
    }
}
