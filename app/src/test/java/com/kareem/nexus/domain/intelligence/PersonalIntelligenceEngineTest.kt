package com.kareem.nexus.domain.intelligence

import com.kareem.nexus.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalIntelligenceEngineTest {
    private val now = 1_800_000_000_000L

    @Test
    fun cibPayment_extractsPaymentAmountAndUsefulActions() {
        val observation = Observation(
            id = "cib",
            type = ObservationType.NOTIFICATION,
            rawText = "CIB — لقد تم رفض المعاملة لعدم كفاية رصيد البطاقة. للسداد يرجى دفع 5672.07 EGP",
            source = "com.cib.mobile",
            createdAt = now - 1_000,
        )

        val result = PersonalIntelligenceEngine.interpret(observation, now)

        assertEquals(SignalKind.PAYMENT, result.kind)
        assertFalse(result.isNoise)
        assertTrue(result.facts.any { it.kind == FactKind.AMOUNT && it.normalizedValue == "5672.07" })
        assertTrue(result.facts.any { it.kind == FactKind.CURRENCY && it.value == "EGP" })
        assertTrue(result.actions.any { it.kind == NexusActionKind.OPEN_SOURCE })
        assertTrue(result.actions.any { it.kind == NexusActionKind.REMIND })
    }

    @Test
    fun realArabicRequest_isTopOfMind() {
        val observation = Observation(
            id = "alfa",
            type = ObservationType.NOTIFICATION,
            rawText = "Alfa Labs — عميلنا العزيز يرجى ارسال الاستفسار الخاص بسيادتكم",
            source = "com.messaging",
            createdAt = now - 1_000,
        )

        val top = PersonalIntelligenceEngine.topOfMind(listOf(observation), now = now)

        assertEquals(1, top.size)
        assertEquals(SignalKind.REQUEST, top.first().kind)
        assertTrue(top.first().actions.any { it.kind == NexusActionKind.REPLY })
    }

    @Test
    fun socialAndReviewNoise_neverInterrupts() {
        val rows = listOf(
            Observation("1", ObservationType.NOTIFICATION, "Neveen Ahmed — added to their Story", "com.social", now - 1_000),
            Observation("2", ObservationType.NOTIFICATION, "We'd love your thoughts on your recent order", "com.store", now - 2_000),
        )

        assertTrue(PersonalIntelligenceEngine.topOfMind(rows, now = now).isEmpty())
        assertTrue(rows.all { PersonalIntelligenceEngine.interpret(it, now).isNoise })
    }

    @Test
    fun fileFailure_becomesFailureNotGenericApproval() {
        val observation = Observation(
            id = "files",
            type = ObservationType.NOTIFICATION,
            rawText = "Couldn't move items — Something went wrong.",
            source = "com.sec.android.app.myfiles",
            createdAt = now - 500,
        )

        val result = PersonalIntelligenceEngine.interpret(observation, now)

        assertEquals(SignalKind.FAILURE, result.kind)
        assertTrue(result.title.contains("failed", ignoreCase = true))
        assertTrue(result.actions.any { it.kind == NexusActionKind.RETRY })
    }

    @Test
    fun relatedOrganizationSignals_formOneSituation() {
        val rows = listOf(
            Observation("1", ObservationType.NOTIFICATION, "CIB — payment of 100 EGP is due today", "com.cib.mobile", now - 1_000),
            Observation("2", ObservationType.NOTIFICATION, "CIB — please confirm your card payment", "com.cib.mobile", now - 2_000),
        )

        val situations = PersonalIntelligenceEngine.buildSituations(rows, now = now)

        assertEquals(1, situations.size)
        assertEquals(2, situations.first().observationIds.size)
        assertTrue(situations.first().title.contains("CIB", ignoreCase = true))
    }

    @Test
    fun terminalLegacyAction_hidesResolvedTopOfMindSignal() {
        val observation = Observation(
            id = "done",
            type = ObservationType.NOTIFICATION,
            rawText = "Please send the quotation today",
            source = "com.mail",
            createdAt = now - 1_000,
        )
        val actions = listOf(
            PreparedAction(
                id = "action_signal_done",
                title = "Send quotation",
                description = observation.rawText,
                state = ActionState.REJECTED,
                createdAt = observation.createdAt,
            )
        )

        assertTrue(PersonalIntelligenceEngine.topOfMind(listOf(observation), actions, now = now).isEmpty())
    }

    @Test
    fun deferredSignal_movesOutOfTopOfMindUntilResurfaced() {
        val observation = Observation(
            id = "later",
            type = ObservationType.NOTIFICATION,
            rawText = "Please confirm the quotation today",
            source = "com.mail",
            createdAt = now - 1_000,
        )
        val deferred = PreparedAction(
            id = "action_signal_later",
            title = "Confirm quotation",
            description = observation.rawText,
            state = ActionState.DRAFT,
            createdAt = observation.createdAt,
        )
        val resurfaced = deferred.copy(state = ActionState.READY_FOR_APPROVAL)

        assertTrue(PersonalIntelligenceEngine.topOfMind(listOf(observation), listOf(deferred), now = now).isEmpty())
        assertEquals(1, PersonalIntelligenceEngine.topOfMind(listOf(observation), listOf(resurfaced), now = now).size)
    }
}
