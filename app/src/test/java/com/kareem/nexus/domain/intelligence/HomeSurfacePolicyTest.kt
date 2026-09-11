package com.kareem.nexus.domain.intelligence

import com.kareem.nexus.core.model.ContextAction
import com.kareem.nexus.core.model.NexusActionKind
import com.kareem.nexus.core.model.OpenLoop
import com.kareem.nexus.core.model.OpenLoopKind
import com.kareem.nexus.core.model.OpenLoopState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSurfacePolicyTest {
    private fun loop(
        id: String,
        situationId: String?,
        kind: OpenLoopKind,
        title: String,
        detail: String,
        priority: Double,
        state: OpenLoopState = OpenLoopState.OPEN,
        party: String? = null,
        updatedAt: Long = 1000L,
    ) = OpenLoop(
        id = id,
        observationId = "obs-$id",
        situationId = situationId,
        kind = kind,
        title = title,
        detail = detail,
        party = party,
        source = "com.example.source",
        state = state,
        priority = priority,
        dueAt = null,
        snoozedUntil = null,
        actions = listOf(ContextAction(NexusActionKind.MARK_RESOLVED, "Done")),
        createdAt = 100L,
        updatedAt = updatedAt,
    )

    @Test
    fun relatedCibPaymentEvidenceCollapsesButAhmedRemainsIndependent() {
        val result = HomeSurfacePolicy.build(
            listOf(
                loop(
                    id = "ahmed",
                    situationId = null,
                    kind = OpenLoopKind.NEEDS_REPLY,
                    title = "Ahmed needs a reply",
                    detail = "Ahmed — Please send the quotation today",
                    priority = .87,
                    party = "Ahmed",
                ),
                loop(
                    id = "cib-1",
                    situationId = "cib-payment-thread",
                    kind = OpenLoopKind.PAYMENT,
                    title = "CIB payment · EGP 5672",
                    detail = "CIB — payment of 5672 EGP is due today",
                    priority = .97,
                    party = "CIB",
                    updatedAt = 3000L,
                ),
                loop(
                    id = "cib-2",
                    situationId = "cib-payment-thread",
                    kind = OpenLoopKind.PAYMENT,
                    title = "CIB payment",
                    detail = "CIB — please confirm your card payment today",
                    priority = .90,
                    party = "CIB",
                    updatedAt = 2000L,
                ),
            )
        )

        assertEquals(2, result.needsYou.size)
        assertTrue(result.needsYou.any { it.id == "ahmed" })
        assertTrue(result.needsYou.any { it.id == "cib-1" })
    }

    @Test
    fun unrelatedSecurityIssueIsNotCollapsedIntoPaymentEvenInsideSameSituation() {
        val result = HomeSurfacePolicy.build(
            listOf(
                loop(
                    id = "payment",
                    situationId = "cib",
                    kind = OpenLoopKind.PAYMENT,
                    title = "CIB payment",
                    detail = "CIB card payment is due today",
                    priority = .90,
                    party = "CIB",
                ),
                loop(
                    id = "security",
                    situationId = "cib",
                    kind = OpenLoopKind.FAILURE,
                    title = "CIB needs review",
                    detail = "CIB suspicious unauthorized card activity detected",
                    priority = .95,
                    party = "CIB",
                ),
            )
        )

        assertEquals(2, result.needsYou.size)
        assertTrue(result.needsYou.any { it.id == "payment" })
        assertTrue(result.needsYou.any { it.id == "security" })
    }

    @Test
    fun resolvedAndSnoozedLoopsNeverSurface() {
        val result = HomeSurfacePolicy.build(
            listOf(
                loop("open", null, OpenLoopKind.NEEDS_REPLY, "Reply", "Please reply", .8),
                loop("resolved", null, OpenLoopKind.PAYMENT, "Payment", "Payment due", .9, OpenLoopState.RESOLVED),
                loop("snoozed", null, OpenLoopKind.FAILURE, "Failure", "Failed", .9, OpenLoopState.SNOOZED),
            )
        )

        assertEquals(listOf("open"), result.surfaced.map { it.id })
    }
}
