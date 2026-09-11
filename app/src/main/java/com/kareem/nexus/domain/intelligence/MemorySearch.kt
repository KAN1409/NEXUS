package com.kareem.nexus.domain.intelligence

object MemorySearch {
    private val conceptGroups = listOf(
        setOf("doctor", "clinic", "hospital", "appointment", "medical", "دكتور", "طبيب", "عياده", "عيادة", "مستشفي", "مستشفى", "كشف", "موعد"),
        setOf("payment", "invoice", "bill", "money", "paid", "دفع", "فاتوره", "فاتورة", "سداد", "مبلغ", "فلوس"),
        setOf("egp", "le", "pound", "pounds", "جنيه", "جنيهات", "ج.م"),
        setOf("delivery", "shipment", "order", "courier", "توصيل", "شحنه", "شحنة", "طلب", "اوردر", "أوردر", "مندوب"),
        setOf("meeting", "appointment", "booking", "reservation", "اجتماع", "موعد", "حجز", "زياره", "زيارة"),
        setOf("android", "kotlin", "compose", "github", "termux", "apk", "build", "app", "development", "برمجه", "برمجة", "تطبيق"),
        setOf("design", "ui", "ux", "visual", "icon", "تصميم", "واجهه", "واجهة", "ايقونه", "أيقونة"),
        setOf("photo", "image", "picture", "screenshot", "gallery", "صوره", "صورة", "سكرين", "سكرينشوت", "لقطه", "لقطة"),
        setOf("travel", "flight", "hotel", "trip", "booking", "سفر", "طيران", "فندق", "رحله", "رحلة", "حجز"),
        setOf("quotation", "purchase", "vendor", "contractor", "procurement", "quote", "عرض", "سعر", "مقاول", "مورد", "شراء", "توريد"),
        setOf("reminder", "followup", "follow-up", "follow", "pending", "waiting", "تذكير", "متابعه", "متابعة", "معلق", "منتظر", "مستني"),
    ).map { group -> group.map(::normalize).toSet() }

    private val stopWords = setOf(
        "the", "a", "an", "that", "this", "thing", "one", "show", "find", "me", "where", "was", "were", "had", "with", "about",
        "فين", "اين", "أين", "وريني", "هات", "هاتلي", "الحاجه", "الحاجة", "اللي", "كان", "كانت", "فيها", "فيه", "بتاع", "بتاعة", "عن", "على", "من",
    ).map(::normalize).toSet()

    fun score(text: String, query: String): Int {
        val haystack = normalize(text)
        val needle = normalize(query)
        if (needle.isBlank()) return 1
        if (haystack.contains(needle)) return 140

        val words = haystack.split(Regex("[^\\p{L}\\p{N}.]+"))
            .filter { it.isNotBlank() }
        val rawTokens = needle.split(Regex("[^\\p{L}\\p{N}.]+"))
            .filter { it.isNotBlank() }
        val tokens = rawTokens.filterNot { it in stopWords }.ifEmpty { rawTokens }
        val numericTokens = tokens.filter { token -> token.any(Char::isDigit) }

        // Remembered numbers are high-signal anchors: when present, the evidence must contain them.
        if (numericTokens.any { token -> !haystack.contains(token) }) return 0

        var matched = 0
        var total = 0
        tokens.forEach { token ->
            val concept = conceptAlternatives(token)
            val tokenScore = when {
                token.any(Char::isDigit) && haystack.contains(token) -> 70
                haystack.contains(token) -> 30
                concept.any(haystack::contains) -> 19
                token.length >= 5 && words.any { oneEditOrTransposeApart(it, token) } -> 8
                concept.any { alternative -> alternative.length >= 5 && words.any { word -> oneEditOrTransposeApart(word, alternative) } } -> 5
                else -> 0
            }
            if (tokenScore > 0) matched++
            total += tokenScore
        }

        if (matched == 0) return 0
        val requiredMatches = when {
            numericTokens.isNotEmpty() && tokens.size > 1 -> 2
            tokens.size <= 2 -> tokens.size
            else -> (tokens.size + 1) / 2
        }.coerceAtLeast(1)
        if (matched < requiredMatches) return 0

        return total + matched * 6
    }

    fun meaningfulTokens(query: String): List<String> {
        val normalized = normalize(query)
        return normalized.split(Regex("[^\\p{L}\\p{N}.]+"))
            .filter { it.isNotBlank() && it !in stopWords }
    }

    private fun conceptAlternatives(token: String): Set<String> =
        conceptGroups.firstOrNull { token in it }.orEmpty() - token

    private fun normalize(value: String): String = normalizeDigits(ContextIntelligence.normalize(value))

    private fun normalizeDigits(value: String): String = value.map { c ->
        when (c) {
            in '٠'..'٩' -> ('0'.code + (c - '٠')).toChar()
            else -> c
        }
    }.joinToString("")

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
