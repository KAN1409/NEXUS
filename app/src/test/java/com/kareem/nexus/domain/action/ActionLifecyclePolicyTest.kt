package com.kareem.nexus.domain.action

import com.kareem.nexus.core.model.ActionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionLifecyclePolicyTest {

    @Test
    fun readyAction_allowsOnlyUserDecisionTransitions() {
        assertTrue(ActionLifecyclePolicy.canTransition(ActionState.READY_FOR_APPROVAL, ActionState.APPROVED))
        assertTrue(ActionLifecyclePolicy.canTransition(ActionState.READY_FOR_APPROVAL, ActionState.DRAFT))
        assertTrue(ActionLifecyclePolicy.canTransition(ActionState.READY_FOR_APPROVAL, ActionState.REJECTED))
        assertFalse(ActionLifecyclePolicy.canTransition(ActionState.READY_FOR_APPROVAL, ActionState.COMPLETED))
    }

    @Test
    fun approvedAction_requiresStartBeforeResult() {
        assertTrue(ActionLifecyclePolicy.canTransition(ActionState.APPROVED, ActionState.EXECUTING))
        assertFalse(ActionLifecyclePolicy.canTransition(ActionState.APPROVED, ActionState.COMPLETED))
        assertFalse(ActionLifecyclePolicy.canTransition(ActionState.APPROVED, ActionState.FAILED))
    }

    @Test
    fun executingAction_canOnlyFinish() {
        assertTrue(ActionLifecyclePolicy.canTransition(ActionState.EXECUTING, ActionState.COMPLETED))
        assertTrue(ActionLifecyclePolicy.canTransition(ActionState.EXECUTING, ActionState.FAILED))
        assertFalse(ActionLifecyclePolicy.canTransition(ActionState.EXECUTING, ActionState.APPROVED))
    }

    @Test
    fun terminalStates_areFinal() {
        ActionState.entries
            .filter { it in setOf(ActionState.COMPLETED, ActionState.REJECTED, ActionState.FAILED) }
            .forEach { terminal ->
                ActionState.entries.forEach { target ->
                    assertFalse(ActionLifecyclePolicy.canTransition(terminal, target))
                }
            }
    }

    @Test
    fun deferredAction_canOnlyResurfaceToReady() {
        assertTrue(ActionLifecyclePolicy.canTransition(ActionState.DRAFT, ActionState.READY_FOR_APPROVAL))
        assertFalse(ActionLifecyclePolicy.canTransition(ActionState.DRAFT, ActionState.APPROVED))
    }
}
