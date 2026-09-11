package com.kareem.nexus.domain.intelligence

import com.kareem.nexus.core.model.*
import java.util.Locale

object ContextIntelligence {
    private val requestTerms = listOf("please","kindly","send","reply","confirm","review","need","required","يرجى","برجاء","ابعت","ارسل","أكد","اكد","محتاج","مطلوب","رد علي", "رد على")
    private val appointmentTerms = listOf("appointment","meeting","visit","booking","reservation","tomorrow","موعد","كشف","زيارة","حجز","اجتماع","بكره","بكرة","غدا","غداً")
    private val deliveryTerms = listOf("delivery","shipment","courier","arriving","توصيل","شحنة","اوردر","أوردر","مندوب")
    private val paymentTerms = listOf("payment","invoice","payment due","bill","دفع","فاتورة","مستحق","المبلغ","سداد")
    private val followTerms = listOf("follow up","follow-up","waiting","pending","remind","reminder","متابعة","مستني","منتظر","تذكير","فكرني")
    private val urgentTerms = listOf("urgent","asap","immediately","today","deadline","عاجل","ضروري","فورا","فوراً","النهاردة","اليوم")
    private val errorTerms = listOf("failed","couldn't","cannot","error","problem","something went wrong","فشل","خطأ","مشكلة","مش قادر","تعذر")

    fun buildAttention(observations: List<Observation>, limit: Int = 8, now: Long = System.currentTimeMillis()): List<AttentionItem> =
        observations.asSequence()
            .filter { it.type in setOf(ObservationType.NOTIFICATION, ObservationType.MANUAL, ObservationType.SHARED_TEXT) }
            .filter { it.createdAt >= now - 7L * 24 * 60 * 60 * 1000 }
            .mapNotNull(::toAttention)
            .sortedWith(compareByDescending<AttentionItem> { levelRank(it.level) }.thenByDescending { it.createdAt })
            .distinctBy { it.id }
            .take(limit).toList()

    fun buildSituations(observations: List<Observation>, limit: Int = 6): List<Situation> {
        val groups = linkedMapOf<String, MutableList<Observation>>()
        observations.filterNot { it.type == ObservationType.APP_USAGE || it.type == ObservationType.IMAGE }.take(200).forEach { observation ->
            val key = situationKey(observation)
            if (key == "general") return@forEach
            groups.getOrPut(key) { mutableListOf() }.add(observation)
        }
        return groups.filterValues { it.size >= 2 }.map { entry ->
            val key = entry.key
            val rows = entry.value
            val kind = situationKind(key, rows)
            Situation(
                id = "situation_" + key.hashCode(),
                title = situationTitle(key, kind),
                summary = rows.size.toString() + " signals share a topic. Review the evidence to confirm the connection.",
                kind = kind,
                observationIds = rows.map { it.id },
                lastUpdatedAt = rows.maxOf { it.createdAt },
            )
        }.sortedByDescending { it.lastUpdatedAt }.take(limit)
    }

    fun buildDailyBrief(observations: List<Observation>, interests: List<Interest>, attention: List<AttentionItem>, now: Long = System.currentTimeMillis()): DailyBrief {
        val dayAgo = now - 24L * 60L * 60L * 1000L
        val newSignals = observations.count { it.createdAt >= dayAgo && it.type != ObservationType.APP_USAGE }
        val topTheme = interests.maxByOrNull { it.affinity }?.label
        val headline = when {
            attention.any { it.level == AttentionLevel.URGENT } -> "A signal may need a prompt review"
            attention.isNotEmpty() -> attention.size.toString() + " things may need your attention"
            newSignals >= 8 -> "NEXUS learned a lot from your day"
            newSignals > 0 -> "Your context changed today"
            else -> "Nothing important is demanding attention"
        }
        val summary = when {
            attention.isNotEmpty() && topTheme != null -> "Your strongest current theme is " + topTheme + ", with " + attention.size + " signals to review worth reviewing."
            attention.isNotEmpty() -> attention.size.toString() + " signals to review look actionable."
            topTheme != null -> topTheme + " is currently your strongest recurring theme."
            else -> "NEXUS will stay quiet until it has something useful to surface."
        }
        return DailyBrief(headline, summary, attention.size, newSignals, topTheme)
    }

    fun buildInsights(observations: List<Observation>, interests: List<Interest>, situations: List<Situation>): List<IntelligenceInsight> {
        val items = mutableListOf<IntelligenceInsight>()
        interests.take(3).forEachIndexed { index, interest ->
            items += IntelligenceInsight(
                id = "interest_" + interest.id,
                title = (if (index == 0) "Dominant theme: " else "Recurring theme: ") + interest.label,
                summary = "This keeps recurring across your recent context.",
                evidence = "Relative prominence " + (interest.affinity * 100).toInt() + "% · local pattern estimate",
                score = interest.affinity,
            )
        }
        if (situations.isNotEmpty()) items += IntelligenceInsight(
            id = "connected_context",
            title = situations.size.toString() + " connected situations detected",
            summary = "These observations share topic keywords; the connections are suggestions.",
            evidence = situations.sumOf { it.observationIds.size }.toString() + " linked signals",
            score = 0.9,
        )
        val usage = observations.count { it.type == ObservationType.APP_USAGE }
        if (usage > 0) items += IntelligenceInsight(
            id = "usage_context",
            title = "Behavior is now part of your context",
            summary = "App usage is compressed into behavioral context instead of flooding Memory.",
            evidence = usage.toString() + " active app snapshots",
            score = 0.7,
        )
        val notifications = observations.count { it.type == ObservationType.NOTIFICATION }
        if (notifications > 0) items += IntelligenceInsight(
            id = "notification_context",
            title = "Notifications are being interpreted",
            summary = "Requests, appointments, deliveries, payments and follow-ups can become attention items.",
            evidence = notifications.toString() + " notification signals available",
            score = 0.8,
        )
        return items.distinctBy { it.id }.sortedByDescending { it.score }.take(8)
    }

    fun suggestedActionFor(text: String): Pair<String, String>? {
        val t = normalize(text)
        if (isResolved(t)) return null
        return when {
            containsAny(t, errorTerms) -> "Check what failed" to text.take(180)
            containsAny(t, paymentTerms) -> "Review payment or invoice" to text.take(180)
            containsAny(t, deliveryTerms) -> "Check delivery or order" to text.take(180)
            containsAny(t, appointmentTerms) -> "Review upcoming commitment" to text.take(180)
            containsAny(t, requestTerms) -> "Respond to this request" to text.take(180)
            containsAny(t, followTerms) -> "Follow up on this" to text.take(180)
            else -> null
        }
    }

    private fun toAttention(o: Observation): AttentionItem? {
        val t = normalize(o.rawText)
        if (isResolved(t)) return null
        val kind = when {
            containsAny(t, errorTerms) -> AttentionKind.ERROR
            containsAny(t, paymentTerms) -> AttentionKind.PAYMENT
            containsAny(t, deliveryTerms) -> AttentionKind.DELIVERY
            containsAny(t, appointmentTerms) -> AttentionKind.APPOINTMENT
            containsAny(t, requestTerms) -> AttentionKind.REQUEST
            containsAny(t, followTerms) -> AttentionKind.FOLLOW_UP
            else -> return null
        }
        val level = when {
            containsAny(t, urgentTerms) -> AttentionLevel.URGENT
            kind == AttentionKind.PAYMENT || kind == AttentionKind.APPOINTMENT || kind == AttentionKind.ERROR -> AttentionLevel.HIGH
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
        return AttentionItem(o.id, title, o.rawText.take(220), o.source, kind, level, o.createdAt)
    }

    private fun situationKey(o: Observation): String {

        val t = normalize(o.rawText)
        return when {
            containsAny(t, paymentTerms) -> "payment"
            containsAny(t, deliveryTerms) -> "delivery"
            containsAny(t, appointmentTerms) -> "appointment"
            containsAny(t, listOf("github","android","kotlin","compose","termux","apk","build")) -> "development"
            containsAny(t, listOf("doctor","hospital","lab","scan","medicine","دكتور","مستشفى","تحليل","اشعة","أشعة")) -> "health"
            containsAny(t, listOf("flight","hotel","booking","travel","trip","رحلة","طيران","فندق")) -> "travel"
            else -> "general"
        }
    }

    private fun situationKind(key: String, rows: List<Observation>): SituationKind = when (key) {
        "development" -> SituationKind.PROJECT
        "health" -> SituationKind.HEALTH
        "travel" -> SituationKind.TRAVEL
        "delivery", "payment" -> SituationKind.PURCHASE
        "appointment" -> SituationKind.ERRAND
        else -> if (rows.any { it.type == ObservationType.NOTIFICATION }) SituationKind.COMMUNICATION else SituationKind.OTHER
    }

    private fun situationTitle(key: String, kind: SituationKind): String = when (key) {
        "development" -> "Development activity"
        "health" -> "Health-related context"
        "travel" -> "Travel-related context"
        "delivery" -> "Orders and deliveries"
        "payment" -> "Payments and invoices"
        "appointment" -> "Appointments and commitments"
        else -> if (kind == SituationKind.COMMUNICATION) "Communication thread" else key.replaceFirstChar { it.uppercase() }
    }

    fun normalize(value: String): String = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
        .replace("ـ", "")
        .replace(Regex("[أإآٱ]"), "ا")
        .replace('ى', 'ي')
        .map { if (it in '٠'..'٩') ('0'.code + (it - '٠')).toChar() else it }
        .joinToString("")
        .replace(Regex("\\s+"), " ").trim()

    private fun containsAny(text: String, terms: List<String>): Boolean = terms.any { term ->
        val normalized = Regex.escape(normalize(term))
        Regex("(?<![\\p{L}\\p{N}])(?:ال)?$normalized(?![\\p{L}\\p{N}])").containsMatchIn(text)
    }

    private fun isResolved(text: String): Boolean = containsAny(text, listOf(
        "payment received", "payment successful", "invoice paid", "already paid", "order delivered",
        "no action required", "no reply needed", "meeting cancelled", "appointment cancelled",
        "تم الدفع", "تم السداد", "تم التسليم", "تم الالغاء", "تم إلغاء الموعد", "مش محتاج رد"
    ))

    fun unresolved(observations: List<Observation>, actions: List<PreparedAction>): List<Observation> {
        val suppressed = actions.filter { it.state in setOf(ActionState.COMPLETED, ActionState.REJECTED, ActionState.DRAFT) }
            .map { it.id.removePrefix("action_signal_").removePrefix("action_commitment_") }.toSet()
        return observations.filterNot { it.id in suppressed }
    }

    private fun levelRank(level: AttentionLevel) = when (level) {
        AttentionLevel.LOW -> 0
        AttentionLevel.MEDIUM -> 1
        AttentionLevel.HIGH -> 2
        AttentionLevel.URGENT -> 3
    }
}

