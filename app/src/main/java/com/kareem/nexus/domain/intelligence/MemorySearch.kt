package com.kareem.nexus.domain.intelligence

object MemorySearch {
    fun score(text: String, query: String): Int {
        val haystack = ContextIntelligence.normalize(text)
        val needle = ContextIntelligence.normalize(query)
        if (needle.isBlank()) return 1
        if (haystack.contains(needle)) return 100
        val words = haystack.split(Regex("[^\\p{L}\\p{N}]+"))
        val tokens = needle.split(Regex("\\s+"))
        var score = 0
        for (token in tokens) {
            score += when {
                haystack.contains(token) -> 10
                token.length >= 5 && words.any { oneEditApart(it, token) } -> 2
                else -> return 0
            }
        }
        return score
    }

    private fun oneEditApart(a: String, b: String): Boolean {
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0; var j = 0; var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) { i++; j++; continue }
            if (++edits > 1) return false
            when {
                a.length > b.length -> i++
                b.length > a.length -> j++
                else -> { i++; j++ }
            }
        }
        return edits + (a.length - i) + (b.length - j) <= 1
    }
}
