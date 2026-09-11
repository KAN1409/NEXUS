package com.kareem.nexus.domain.intelligence

import com.kareem.nexus.core.model.*
import org.junit.Assert.*
import org.junit.Test

class IntelligenceRegressionTest {
    private fun row(id: String, text: String) = Observation(id, ObservationType.NOTIFICATION, text, "com.whatsapp", 1000)
    @Test fun arabicNormalizationPreservesWordsAndMatchesNumbers() {
        assertEquals("ارسال الفاتوره 123", ContextIntelligence.normalize("إِرسـال الفاتوره ١٢٣"))
        assertTrue(MemorySearch.score("موعد أَحمد يوم ١٢", "احمد 12") > 0)
    }
    @Test fun searchAllowsOneTypoButRequiresEveryQueryTerm() {
        assertTrue(MemorySearch.score("Signed quotation for marble", "qotation marble") > 0)
        assertEquals(0, MemorySearch.score("Signed quotation for marble", "quotation wood"))
    }
    @Test fun shortSubstringsDoNotCreateFalseRequests() {
        assertNull(ContextIntelligence.suggestedActionFor("Sender updated the border wallpaper"))
        assertNull(ContextIntelligence.suggestedActionFor("فرد جديد في المجموعة"))
    }
    @Test fun completedReceiptsStayQuiet() {
        assertNull(ContextIntelligence.suggestedActionFor("Payment successful. Invoice paid today"))
        assertNull(ContextIntelligence.suggestedActionFor("تم الدفع اليوم"))
    }
    @Test fun twoRequestsFromSameAppRemainTwoItems() {
        val items = ContextIntelligence.buildAttention(listOf(row("a", "Please send drawings"), row("b", "Please confirm meeting")), now = 2000)
        assertEquals(2, items.size)
    }
    @Test fun dismissedAndDeferredEvidenceIsSuppressed() {
        val rows = listOf(row("a", "Please send quotation"), row("b", "Meeting tomorrow"))
        val actions = listOf(PreparedAction("action_signal_a", "Request", "", ActionState.REJECTED, 1000), PreparedAction("action_signal_b", "Meeting", "", ActionState.DRAFT, 1000))
        assertTrue(ContextIntelligence.unresolved(rows, actions).isEmpty())
    }
    @Test fun unrelatedMessagesInSameAppAreNotAConversation() {
        assertTrue(ContextIntelligence.buildSituations(listOf(row("a", "Hello Karim"), row("b", "Beautiful weather"))).isEmpty())
    }
    @Test fun oldRequestsDoNotClaimCurrentUrgency() {
        assertTrue(ContextIntelligence.buildAttention(listOf(row("a", "Please reply urgently")), now = 15L * 24 * 60 * 60 * 1000).isEmpty())
    }
}
