package com.kareem.nexus.domain.intelligence

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
        assertTrue(MemorySearch.score("Kotlin Android build", "restaurant") == 0)
    }
}
