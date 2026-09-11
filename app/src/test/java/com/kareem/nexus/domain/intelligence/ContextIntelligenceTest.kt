package com.kareem.nexus.domain.intelligence

import com.kareem.nexus.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        val attention = ContextIntelligence.buildAttention(listOf(observation), now = 500L)
        assertEquals(1, attention.size)
        assertEquals(AttentionKind.REQUEST, attention.first().kind)
        assertEquals(AttentionLevel.MEDIUM, attention.first().level)
        assertTrue(ContextIntelligence.suggestedActionFor(observation.rawText, observation.source) != null)
    }

    @Test
    fun explicitUrgency_becomesUrgent() {
        val observation = Observation(
            id = "urgent",
            type = ObservationType.NOTIFICATION,
            rawText = "Please confirm the quotation ASAP",
            source = "com.example.mail",
            createdAt = 100L,
        )
        val attention = ContextIntelligence.buildAttention(listOf(observation), now = 500L)
        assertEquals(AttentionLevel.URGENT, attention.first().level)
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
        val attention = ContextIntelligence.buildAttention(listOf(observation), now = 500L)
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
        assertTrue(ContextIntelligence.buildAttention(listOf(observation), now = 500L).isEmpty())
        assertNull(ContextIntelligence.suggestedActionFor(observation.rawText, observation.source))
    }

    @Test
    fun feedbackSolicitation_isSuppressed() {
        val text = "We'd love your thoughts on your recent order. Tell us what you think."
        assertTrue(ContextIntelligence.shouldSuppress(text, "com.store.app"))
        assertNull(ContextIntelligence.suggestedActionFor(text, "com.store.app"))
    }

    @Test
    fun socialStoryNotification_isSuppressed() {
        val text = "Neveen Ahmed — added to their Story"
        assertTrue(ContextIntelligence.shouldSuppress(text, "com.social.app"))
        assertTrue(
            ContextIntelligence.buildAttention(
                listOf(Observation("4", ObservationType.NOTIFICATION, text, "com.social.app", 100L)),
                now = 500L,
            ).isEmpty()
        )
    }

    @Test
    fun nexusSelfNotification_isSuppressed() {
        assertTrue(
            ContextIntelligence.shouldSuppress(
                "Your image is ready to review",
                "com.kareem.nexus",
            )
        )
    }

    @Test
    fun realArabicRequest_survivesNoiseFilter() {
        val text = "عميلنا العزيز برجاء ارسال الاستفسار الخاص بسيادتكم"
        assertTrue(ContextIntelligence.suggestedActionFor(text, "com.messaging") != null)
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
