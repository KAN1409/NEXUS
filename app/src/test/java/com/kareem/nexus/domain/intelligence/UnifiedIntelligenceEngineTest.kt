package com.kareem.nexus.domain.intelligence

import com.kareem.nexus.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedIntelligenceEngineTest {
    private val now = 1_800_000_000_000L

    @Test
    fun directRequest_becomesNeedsReplyOpenLoop() {
        val observation = Observation(
            id = "request",
            type = ObservationType.NOTIFICATION,
            rawText = "Ahmed — Please send the quotation today",
            source = "com.whatsapp",
            createdAt = now - 1_000,
        )
        val understanding = PersonalIntelligenceEngine.interpret(observation, now)
        val loop = UnifiedIntelligenceEngine.deriveOpenLoop(observation, understanding, now = now)

        assertNotNull(loop)
        assertEquals(OpenLoopKind.NEEDS_REPLY, loop!!.kind)
        assertTrue(loop.title.contains("Ahmed", ignoreCase = true))
        assertNotNull(loop.dueAt)
        assertTrue(loop.actions.any { it.kind == NexusActionKind.REMIND })
        assertTrue(loop.actions.any { it.kind == NexusActionKind.MARK_RESOLVED })
    }

    @Test
    fun waitingLanguage_becomesWaitingOn() {
        val observation = Observation(
            id = "waiting",
            type = ObservationType.MANUAL,
            rawText = "مستني عرض السعر من Ahmed",
            source = "NEXUS",
            createdAt = now - 2_000,
        )
        val understanding = ObservationUnderstanding(
            observationId = observation.id,
            kind = SignalKind.FOLLOW_UP,
            title = "Follow up",
            summary = observation.rawText,
            facts = listOf(ExtractedFact(FactKind.ORGANIZATION, "Ahmed")),
            actions = listOf(ContextAction(NexusActionKind.OPEN_SOURCE, "Open source", observation.source)),
            priority = .7,
            confidence = .8,
            isNoise = false,
            analyzedAt = now,
        )

        val loop = UnifiedIntelligenceEngine.deriveOpenLoop(observation, understanding, now = now)
        assertEquals(OpenLoopKind.WAITING_ON, loop!!.kind)
        assertEquals(OpenLoopState.WAITING, loop.state)
    }

    @Test
    fun passiveInformation_doesNotBecomeOpenLoop() {
        val observation = Observation("info", ObservationType.NOTIFICATION, "Your photo backup is complete", "photos", now)
        val understanding = ObservationUnderstanding(
            observation.id, SignalKind.INFORMATION, "Info", observation.rawText, emptyList(), emptyList(), .1, .7, false, now
        )
        assertEquals(null, UnifiedIntelligenceEngine.deriveOpenLoop(observation, understanding, now = now))
    }
}
