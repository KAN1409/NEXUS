package com.kareem.nexus.domain.intelligence

import com.kareem.nexus.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextIntelligenceTest {

    @Test
    fun requestNotification_becomesAttentionAndAction() {
        val observation = Observation(
            id = "1",
            type = ObservationType.NOTIFICATION,
            rawText = "Please send the signed quotation today",
            source = "com.example.mail",
            createdAt = 100L,
        )
        val attention = ContextIntelligence.buildAttention(listOf(observation))
        assertEquals(1, attention.size)
        assertEquals(AttentionKind.REQUEST, attention.first().kind)
        assertEquals(AttentionLevel.URGENT, attention.first().level)
        assertTrue(ContextIntelligence.suggestedActionFor(observation.rawText) != null)
    }

    @Test
    fun appointmentNotification_isHighPriority() {
        val observation = Observation(
            id = "2",
            type = ObservationType.NOTIFICATION,
            rawText = "موعد الكشف بكرة الساعة 5",
            source = "com.example.calendar",
            createdAt = 200L,
        )
        val attention = ContextIntelligence.buildAttention(listOf(observation))
        assertEquals(AttentionKind.APPOINTMENT, attention.first().kind)
        assertEquals(AttentionLevel.HIGH, attention.first().level)
    }

    @Test
    fun unrelatedNotification_staysQuiet() {
        val observation = Observation(
            id = "3",
            type = ObservationType.NOTIFICATION,
            rawText = "New wallpaper available",
            source = "com.example.wallpaper",
            createdAt = 300L,
        )
        assertTrue(ContextIntelligence.buildAttention(listOf(observation)).isEmpty())
        assertEquals(null, ContextIntelligence.suggestedActionFor(observation.rawText))
    }

    @Test
    fun relatedSignals_formSituation() {
        val rows = listOf(
            Observation("1", ObservationType.NOTIFICATION, "Android build finished", "com.github.android", 100L),
            Observation("2", ObservationType.MANUAL, "Need to review Kotlin Compose app", "NEXUS", 200L),
        )
        val situations = ContextIntelligence.buildSituations(rows)
        assertTrue(situations.any { it.kind == SituationKind.PROJECT })
    }
}
