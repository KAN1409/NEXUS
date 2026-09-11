package com.kareem.nexus.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemorySearchTest {
    @Test
    fun arabicConcept_matchesEnglishMedicalText() {
        assertTrue(MemorySearch.score("Doctor appointment tomorrow", "كشف") > 0)
    }

    @Test
    fun englishConcept_matchesArabicPaymentText() {
        assertTrue(MemorySearch.score("فاتورة مستحقة للدفع", "invoice") > 0)
    }

    @Test
    fun typoTolerance_stillWorks() {
        assertTrue(MemorySearch.score("Jetpack Compose project", "comopse") > 0)
    }

    @Test
    fun unrelatedQuery_doesNotMatch() {
        assertEquals(0, MemorySearch.score("Kotlin Android build", "restaurant"))
    }

    @Test
    fun vagueArabicNumericRecall_findsTheThingWithTheAmount() {
        val score = MemorySearch.score(
            "CIB payment declined. Amount EGP 5672.07",
            "الحاجة اللي كان فيها 5672 جنيه",
        )
        assertTrue(score > 0)
    }

    @Test
    fun arabicDigits_areNormalizedForRecall() {
        assertTrue(MemorySearch.score("Invoice total 5672 EGP", "٥٦٧٢ جنيه") > 0)
    }

    @Test
    fun vagueScreenshotLanguage_ignoresFillerWords() {
        assertTrue(MemorySearch.score("Screenshot: عرض سعر رخام جلالة", "فين السكرين اللي كان فيها عرض سعر") > 0)
    }
}
