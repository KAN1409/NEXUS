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
                token.length >= 5 && words.any { oneEditApart(it, token) } -> 5
                concept.any { alternative ->
                    alternative.length >= 5 && words.any { word -> oneEditApart(word, alternative) }
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

    private fun oneEditApart(a: String, b: String): Boolean {
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                i++
                j++
                continue
            }
            if (++edits > 1) return false
            when {
                a.length > b.length -> i++
                b.length > a.length -> j++
                else -> {
                    i++
                    j++
                }
            }
        }
        return edits + (a.length - i) + (b.length - j) <= 1
    }
}
