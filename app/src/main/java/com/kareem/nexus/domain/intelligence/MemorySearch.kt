package com.kareem.nexus.domain.intelligence

object MemorySearch {
    private val conceptGroups = listOf(
        setOf("doctor", "clinic", "hospital", "appointment", "medical", "دكتور", "طبيب", "عياده", "عيادة", "مستشفي", "مستشفى", "كشف", "موعد"),
        setOf("payment", "invoice", "bill", "money", "paid", "دفع", "فاتوره", "فاتورة", "سداد", "مبلغ", "فلوس"),
        setOf("delivery", "shipment", "order", "courier", "توصيل", "شحنه", "شحنة", "طلب", "اوردر", "أوردر", "مندوب"),
        setOf("meeting", "appointment", "booking", "reservation", "اجتماع", "موعد", "حجز", "زياره", "زيارة"),
        setOf("android", "kotlin", "compose", "github", "termux", "apk", "build", "app", "development", "برمجه", "برمجة", "تطبيق"),
        setOf("design", "ui", "ux", "visual", "icon", "تصميم", "واجهه", "واجهة", "ايقونه", "أيقونة"),
        setOf("photo", "image", "picture", "screenshot", "gallery", "صوره", "صورة", "سكرين", "لقطه", "لقطة"),
        setOf("travel", "flight", "hotel", "trip", "booking", "سفر", "طيران", "فندق", "رحله", "رحلة", "حجز"),
        setOf("quotation", "purchase", "vendor", "contractor", "procurement", "quote", "عرض", "سعر", "مقاول", "مورد", "شراء", "توريد"),
        setOf("reminder", "followup", "follow-up", "follow", "pending", "تذكير", "متابعه", "متابعة", "معلق", "منتظر"),
    ).map { group -> group.map(ContextIntelligence::normalize).toSet() }

    fun score(text: String, query: String): Int {
        val haystack = ContextIntelligence.normalize(text)
        val needle = ContextIntelligence.normalize(query)
        if (needle.isBlank()) return 1
        if (haystack.contains(needle)) return 100

        val words = haystack.split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotBlank() }
        val tokens = needle.split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        var score = 0
        for (token in tokens) {
            val concept = conceptAlternatives(token)
            val tokenScore = when {
                haystack.contains(token) -> 20
                concept.any { alternative -> haystack.contains(alternative) } -> 12
                token.length >= 5 && words.any { oneEditOrTransposeApart(it, token) } -> 5
                concept.any { alternative ->
                    alternative.length >= 5 && words.any { word -> oneEditOrTransposeApart(word, alternative) }
                } -> 3
                else -> 0
            }
            if (tokenScore == 0) return 0
            score += tokenScore
        }

        return score + if (tokens.size > 1) 5 else 0
    }

    private fun conceptAlternatives(token: String): Set<String> =
        conceptGroups.firstOrNull { token in it }.orEmpty() - token

    private fun oneEditOrTransposeApart(a: String, b: String): Boolean {
        if (a == b) return true
        if (kotlin.math.abs(a.length - b.length) > 1) return false

        if (a.length == b.length) {
            val mismatch = a.indices.filter { a[it] != b[it] }
            if (mismatch.size == 1) return true
            if (mismatch.size == 2) {
                val first = mismatch[0]
                val second = mismatch[1]
                if (second == first + 1 && a[first] == b[second] && a[second] == b[first]) return true
            }
            return false
        }

        val longer = if (a.length > b.length) a else b
        val shorter = if (a.length > b.length) b else a
        var i = 0
        var j = 0
        var edits = 0
        while (i < longer.length && j < shorter.length) {
            if (longer[i] == shorter[j]) {
                i++
                j++
            } else {
                if (++edits > 1) return false
                i++
            }
        }
        if (i < longer.length) edits++
        return edits <= 1
    }
}
