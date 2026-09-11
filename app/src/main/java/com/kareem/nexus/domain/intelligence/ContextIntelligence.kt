package com.kareem.nexus.domain.intelligence

import com.kareem.nexus.core.model.*
import java.util.Locale

object ContextIntelligence {
    private val appointmentTerms = listOf(
        "appointment", "meeting", "visit", "booking", "reservation", "tomorrow",
        "موعد", "كشف", "زيارة", "حجز", "اجتماع", "بكره", "بكرة", "غدا", "غداً",
    )
    private val deliveryTerms = listOf(
        "delivery", "shipment", "courier", "arriving", "out for delivery",
        "توصيل", "شحنة", "اوردر", "أوردر", "مندوب",
    )
    private val paymentTerms = listOf(
        "payment due", "invoice", "bill due", "amount due", "pay before",
        "دفع", "فاتورة", "مستحق", "المبلغ", "سداد",
    )
    private val followTerms = listOf(
        "follow up", "follow-up", "waiting for", "pending your", "remind", "reminder",
        "متابعة", "مستني", "منتظر", "تذكير", "فكرني",
    )
    private val urgentTerms = listOf(
        "urgent", "asap", "immediately", "deadline", "due today",
        "عاجل", "ضروري", "فورا", "فوراً", "النهاردة", "اليوم",
    )
    private val errorTerms = listOf(
        "failed", "couldn't", "cannot", "error", "something went wrong", "action required",
        "فشل", "خطأ", "مشكلة", "مش قادر", "تعذر",
    )
    private val explicitRequestTerms = listOf(
        "please send", "please reply", "please confirm", "kindly send", "kindly confirm",
        "can you send", "could you send", "send us", "send me", "reply to", "confirm your",
        "برجاء ارسال", "برجاء إرسال", "يرجى ارسال", "يرجى إرسال", "ابعت", "ارسل", "أرسل",
        "من فضلك", "محتاج منك", "مطلوب منك", "برجاء الرد", "يرجى الرد", "رد علينا", "رد علي", "رد على",
    )
    private val noiseTerms = listOf(
        "rate your order", "rate your experience", "review your order", "review your recent order",
        "we'd love your thoughts", "we would love your thoughts", "tell us what you think", "leave a review",
        "added to their story", "sent a reel", "reacted to", "liked your", "new story", "new reel",
        "sale", "discount", "special offer", "limited offer", "promo code", "coupon",
        "verification code", "otp", "one time password", "response ready", "tap to return",
        "image is ready", "installation guide", "your image is ready to review",
        "قيم طلبك", "قيّم طلبك", "شاركنا رأيك", "ما رأيك", "عرض خاص", "خصم", "كود التحقق",
    )
    private val passiveStatusTerms = listOf(
        "delivered successfully", "payment received", "payment successful", "invoice paid", "already paid",
        "order delivered", "no action required", "no reply needed", "meeting cancelled", "appointment cancelled",
        "تم الدفع", "تم السداد", "تم التسليم", "تم الالغاء", "تم إلغاء الموعد", "مش محتاج رد",
    )

    fun buildAttention(
        observations: List<Observation>,
        limit: Int = 8,
        now: Long = System.currentTimeMillis(),
    ): List<AttentionItem> = observations.asSequence()
        .filter { it.type in setOf(ObservationType.NOTIFICATION, ObservationType.MANUAL, ObservationType.SHARED_TEXT) }
        .filter { it.createdAt >= now - 7L * 24 * 60 * 60 * 1000 }
        .mapNotNull(::toAttention)
        .sortedWith(compareByDescending<AttentionItem> { levelRank(it.level) }.thenByDescending { it.createdAt })
        .distinctBy { it.id }
        .take(limit)
        .toList()

    fun buildSituations(observations: List<Observation>, limit: Int = 6): List<Situation> {
        val groups = linkedMapOf<String, MutableList<Observation>>()
        observations
            .filterNot { it.type == ObservationType.APP_USAGE || it.type == ObservationType.IMAGE }
            .filterNot { shouldSuppress(it.rawText, it.source) }
            .take(200)
            .forEach { observation ->
                val key = situationKey(observation)
                if (key != "general") groups.getOrPut(key) { mutableListOf() }.add(observation)
            }

        return groups.filterValues { it.size >= 2 }.map { (key, rows) ->
            val kind = situationKind(key, rows)
            Situation(
                id = "situation_" + key.hashCode(),
                title = situationTitle(key, kind),
                summary = situationSummary(key, rows.size),
                kind = kind,
                observationIds = rows.map { it.id },
                lastUpdatedAt = rows.maxOf { it.createdAt },
            )
        }.sortedByDescending { it.lastUpdatedAt }.take(limit)
    }

    fun buildDailyBrief(
        observations: List<Observation>,
        interests: List<Interest>,
        attention: List<AttentionItem>,
        now: Long = System.currentTimeMillis(),
    ): DailyBrief {
        val dayAgo = now - 24L * 60L * 60L * 1000L
        val newSignals = observations.count {
            it.createdAt >= dayAgo && it.type != ObservationType.APP_USAGE && !shouldSuppress(it.rawText, it.source)
        }
        val topTheme = interests.maxByOrNull { it.affinity }?.label
        val headline = when {
            attention.any { it.level == AttentionLevel.URGENT } -> "Something may need a prompt review"
            attention.size == 1 -> "1 thing may need your attention"
            attention.isNotEmpty() -> "${attention.size} things may need your attention"
            newSignals >= 8 -> "NEXUS connected a busy day"
            newSignals > 0 -> "Your context changed today"
            else -> "Nothing important is demanding attention"
        }
        val summary = when {
            attention.isNotEmpty() && topTheme != null ->
                "$topTheme is your strongest recent theme. ${attention.size} actionable signal${if (attention.size == 1) "" else "s"} survived NEXUS noise filtering."
            attention.isNotEmpty() ->
                "${attention.size} signal${if (attention.size == 1) "" else "s"} look actionable after filtering promotions and passive updates."
            topTheme != null -> "$topTheme is currently your strongest recurring theme."
            else -> "NEXUS will stay quiet until it has something useful to surface."
        }
        return DailyBrief(headline, summary, attention.size, newSignals, topTheme)
    }

    fun buildInsights(
        observations: List<Observation>,
        interests: List<Interest>,
        situations: List<Situation>,
    ): List<IntelligenceInsight> {
        val clean = observations.filterNot { shouldSuppress(it.rawText, it.source) }
        val items = mutableListOf<IntelligenceInsight>()

        situations.take(3).forEach { situation ->
            items += IntelligenceInsight(
                id = "situation_${situation.id}",
                title = situation.title,
                summary = situation.summary,
                evidence = "${situation.observationIds.size} related signals",
                score = 1.0,
            )
        }

        interests.take(4).forEachIndexed { index, interest ->
            items += IntelligenceInsight(
                id = "interest_${interest.id}",
                title = when (index) {
                    0 -> "Dominant theme: ${interest.label}"
                    1 -> "Strong secondary theme: ${interest.label}"
                    else -> "Recurring theme: ${interest.label}"
                },
                summary = "This topic keeps recurring across recent saved context.",
                evidence = "Relative prominence ${(interest.affinity * 100).toInt()}%",
                score = interest.affinity,
            )
        }

        val actionable = clean.count { suggestedActionFor(it.rawText, it.source) != null }
        if (actionable > 0) {
            items += IntelligenceInsight(
                id = "actionable_context",
                title = "$actionable actionable signal${if (actionable == 1) "" else "s"} detected",
                summary = "NEXUS found requests, commitments or failures that may deserve a decision.",
                evidence = "Promotions and passive social notifications excluded",
                score = .95,
            )
        }

        return items.distinctBy { it.id }.sortedByDescending { it.score }.take(8)
    }

    fun suggestedActionFor(text: String, source: String? = null): Pair<String, String>? {
        val t = normalize(text)
        if (shouldSuppress(text, source) || isResolved(t)) return null
        return when {
            containsAny(t, errorTerms) -> "Check what failed" to text.take(220)
            containsAny(t, paymentTerms) -> "Review payment or invoice" to text.take(220)
            containsAny(t, deliveryTerms) && !containsAny(t, listOf("delivered", "تم التسليم")) -> "Check delivery or order" to text.take(220)
            containsAny(t, appointmentTerms) -> "Review upcoming commitment" to text.take(220)
            isExplicitRequest(t) -> "Respond to this request" to text.take(220)
            containsAny(t, followTerms) -> "Follow up on this" to text.take(220)
            else -> null
        }
    }

    fun shouldSuppress(text: String, source: String? = null): Boolean {
        val t = normalize(text)
        val s = normalize(source.orEmpty())
        if (s.contains("com.kareem.nexus")) return true
        if (containsAny(t, noiseTerms)) return true
        if (containsAny(t, passiveStatusTerms)) return true
        if (t.length < 4) return true
        return false
    }

    private fun toAttention(o: Observation): AttentionItem? {
        val t = normalize(o.rawText)
        if (shouldSuppress(o.rawText, o.source) || isResolved(t)) return null
        val kind = when {
            containsAny(t, errorTerms) -> AttentionKind.ERROR
            containsAny(t, paymentTerms) -> AttentionKind.PAYMENT
            containsAny(t, deliveryTerms) && !containsAny(t, listOf("delivered", "تم التسليم")) -> AttentionKind.DELIVERY
            containsAny(t, appointmentTerms) -> AttentionKind.APPOINTMENT
            isExplicitRequest(t) -> AttentionKind.REQUEST
            containsAny(t, followTerms) -> AttentionKind.FOLLOW_UP
            else -> return null
        }
        val level = when {
            containsAny(t, urgentTerms) -> AttentionLevel.URGENT
            kind in setOf(AttentionKind.PAYMENT, AttentionKind.APPOINTMENT, AttentionKind.ERROR) -> AttentionLevel.HIGH
            else -> AttentionLevel.MEDIUM
        }
        val title = when (kind) {
            AttentionKind.ERROR -> "Something failed"
            AttentionKind.PAYMENT -> "Payment may need attention"
            AttentionKind.DELIVERY -> "Delivery or order update"
            AttentionKind.APPOINTMENT -> "Upcoming commitment"
            AttentionKind.REQUEST -> "Someone may be waiting for you"
            AttentionKind.FOLLOW_UP -> "Possible follow-up"
            AttentionKind.REMINDER -> "Reminder"
            AttentionKind.GENERAL -> "Needs attention"
        }
        return AttentionItem(o.id, title, o.rawText.take(240), o.source, kind, level, o.createdAt)
    }

    private fun isExplicitRequest(text: String): Boolean {
        if (containsAny(text, explicitRequestTerms)) return true
        val questionWithAction = text.contains('?') && containsAny(
            text,
            listOf("can you", "could you", "would you", "ممكن", "تقدر", "ينفع"),
        )
        return questionWithAction
    }

    private fun situationKey(o: Observation): String {
        val t = normalize(o.rawText)
        return when {
            containsAny(t, paymentTerms) -> "payment"
            containsAny(t, deliveryTerms) -> "delivery"
            containsAny(t, appointmentTerms) -> "appointment"
            containsAny(t, listOf("github", "android", "kotlin", "compose", "termux", "apk", "build", "release", "ci")) -> "development"
            containsAny(t, listOf("doctor", "hospital", "lab", "scan", "medicine", "دكتور", "مستشفي", "تحليل", "اشعه", "دواء")) -> "health"
            containsAny(t, listOf("flight", "hotel", "booking", "travel", "trip", "رحله", "طيران", "فندق")) -> "travel"
            containsAny(t, listOf("quotation", "purchase order", "vendor", "contractor", "pr ", "po ", "مقاول", "امر اسناد", "عرض سعر")) -> "procurement"
            else -> "general"
        }
    }

    private fun situationKind(key: String, rows: List<Observation>): SituationKind = when (key) {
        "development", "procurement" -> SituationKind.PROJECT
        "health" -> SituationKind.HEALTH
        "travel" -> SituationKind.TRAVEL
        "delivery", "payment" -> SituationKind.PURCHASE
        "appointment" -> SituationKind.ERRAND
        else -> if (rows.any { it.type == ObservationType.NOTIFICATION }) SituationKind.COMMUNICATION else SituationKind.OTHER
    }

    private fun situationTitle(key: String, kind: SituationKind): String = when (key) {
        "development" -> "Development activity"
        "procurement" -> "Project and procurement thread"
        "health" -> "Health-related context"
        "travel" -> "Travel-related context"
        "delivery" -> "Orders and deliveries"
        "payment" -> "Payments and invoices"
        "appointment" -> "Appointments and commitments"
        else -> if (kind == SituationKind.COMMUNICATION) "Communication thread" else key.replaceFirstChar { it.uppercase() }
    }

    private fun situationSummary(key: String, count: Int): String = when (key) {
        "development" -> "$count development signals appear to belong to the same active work context."
        "procurement" -> "$count procurement/project signals may belong to the same work thread."
        "health" -> "$count health-related signals are connected in recent context."
        "appointment" -> "$count signals mention appointments, meetings or upcoming commitments."
        "delivery" -> "$count order or delivery signals are related."
        "payment" -> "$count payment or invoice signals are related."
        else -> "$count related signals share meaningful context."
    }

    fun normalize(value: String): String = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
        .replace("ـ", "")
        .replace(Regex("[أإآٱ]"), "ا")
        .replace('ى', 'ي')
        .map { if (it in '٠'..'٩') ('0'.code + (it - '٠')).toChar() else it }
        .joinToString("")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun containsAny(text: String, terms: List<String>): Boolean = terms.any { term ->
        val normalized = normalize(term)
        if (normalized.contains(' ')) text.contains(normalized)
        else Regex("(?<![\\p{L}\\p{N}])(?:ال)?${Regex.escape(normalized)}(?![\\p{L}\\p{N}])").containsMatchIn(text)
    }

    private fun isResolved(text: String): Boolean = containsAny(text, passiveStatusTerms)

    fun unresolved(observations: List<Observation>, actions: List<PreparedAction>): List<Observation> {
        val suppressed = actions
            .filter { it.state in setOf(ActionState.COMPLETED, ActionState.REJECTED, ActionState.DRAFT) }
            .map { it.id.removePrefix("action_signal_").removePrefix("action_commitment_") }
            .toSet()
        return observations.filterNot { it.id in suppressed || shouldSuppress(it.rawText, it.source) }
    }

    private fun levelRank(level: AttentionLevel) = when (level) {
        AttentionLevel.LOW -> 0
        AttentionLevel.MEDIUM -> 1
        AttentionLevel.HIGH -> 2
        AttentionLevel.URGENT -> 3
    }
}
