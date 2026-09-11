package com.kareem.nexus.domain.intelligence

import com.kareem.nexus.core.model.*
import java.util.Locale

object ContextIntelligence {
    private val requestTerms = listOf("please","kindly","send","reply","confirm","review","need","required","يرجى","برجاء","ابعت","ارسل","أكد","اكد","محتاج","مطلوب","رد")
    private val appointmentTerms = listOf("appointment","meeting","visit","booking","reservation","tomorrow","موعد","كشف","زيارة","حجز","اجتماع","بكره","بكرة","غدا","غداً")
    private val deliveryTerms = listOf("delivery","shipment","order","courier","arriving","delivered","توصيل","شحنة","طلب","اوردر","أوردر","مندوب")
    private val paymentTerms = listOf("payment","invoice","due","bill","paid","amount","egp","usd","دفع","فاتورة","مستحق","المبلغ","جنيه")
    private val followTerms = listOf("follow up","follow-up","waiting","pending","remind","reminder","متابعة","مستني","منتظر","تذكير","فكرني")
    private val urgentTerms = listOf("urgent","asap","immediately","today","deadline","عاجل","ضروري","فورا","فوراً","النهاردة","اليوم")
    private val errorTerms = listOf("failed","couldn't","cannot","error","problem","something went wrong","فشل","خطأ","مشكلة","مش قادر","تعذر")

    fun buildAttention(observations: List<Observation>, limit: Int = 8): List<AttentionItem> =
        observations.asSequence()
            .filter { it.type == ObservationType.NOTIFICATION || it.type == ObservationType.MANUAL }
            .mapNotNull(::toAttention)
            .sortedWith(compareByDescending<AttentionItem> { levelRank(it.level) }.thenByDescending { it.createdAt })
            .distinctBy { it.title.lowercase(Locale.ROOT) + "|" + it.source.orEmpty() }
            .take(limit).toList()

    fun buildSituations(observations: List<Observation>, limit: Int = 6): List<Situation> {
        val groups = linkedMapOf<String, MutableList<Observation>>()
        observations.take(80).forEach { observation ->
            val key = situationKey(observation)
            groups.getOrPut(key) { mutableListOf() }.add(observation)
        }
        return groups.filterValues { it.size >= 2 }.map { entry ->
            val key = entry.key
            val rows = entry.value
            val kind = situationKind(key, rows)
            Situation(
                id = "situation_" + key.hashCode(),
                title = situationTitle(key, kind),
                summary = rows.size.toString() + " related signals connected across your recent context.",
                kind = kind,
                observationIds = rows.map { it.id },
                lastUpdatedAt = rows.maxOf { it.createdAt },
            )
        }.sortedByDescending { it.lastUpdatedAt }.take(limit)
    }

    fun buildDailyBrief(observations: List<Observation>, interests: List<Interest>, attention: List<AttentionItem>): DailyBrief {
        val dayAgo = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        val newSignals = observations.count { it.createdAt >= dayAgo }
        val topTheme = interests.maxByOrNull { it.affinity }?.label
        val headline = when {
            attention.any { it.level == AttentionLevel.URGENT } -> "You have something urgent to handle"
            attention.isNotEmpty() -> attention.size.toString() + " things may need your attention"
            newSignals >= 8 -> "NEXUS learned a lot from your day"
            newSignals > 0 -> "Your context changed today"
            else -> "Nothing important is demanding attention"
        }
        val summary = when {
            attention.isNotEmpty() && topTheme != null -> "Your strongest current theme is " + topTheme + ", with " + attention.size + " unresolved signals worth reviewing."
            attention.isNotEmpty() -> attention.size.toString() + " unresolved signals look actionable."
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
                title = (if (index == 0) "Dominant theme: " else "Growing theme: ") + interest.label,
                summary = "This keeps recurring across your recent context.",
                evidence = "Confidence " + (interest.confidence * 100).toInt() + "%",
                score = interest.affinity,
            )
        }
        if (situations.isNotEmpty()) items += IntelligenceInsight(
            id = "connected_context",
            title = situations.size.toString() + " connected situations detected",
            summary = "NEXUS linked separate observations that appear to belong to the same real-world context.",
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
        val src = o.source.orEmpty().substringAfterLast('.').lowercase(Locale.ROOT)
        val t = normalize(o.rawText)
        return when {
            containsAny(t, paymentTerms) -> "payment"
            containsAny(t, deliveryTerms) -> "delivery"
            containsAny(t, appointmentTerms) -> "appointment"
            containsAny(t, listOf("github","android","kotlin","compose","termux","apk","build")) -> "development"
            containsAny(t, listOf("doctor","hospital","lab","scan","medicine","دكتور","مستشفى","تحليل","اشعة","أشعة")) -> "health"
            containsAny(t, listOf("flight","hotel","booking","travel","trip","رحلة","طيران","فندق")) -> "travel"
            else -> src.ifBlank { "general" }
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

    private fun normalize(value: String) = value.lowercase(Locale.ROOT).replace('ـ', ' ')
    private fun containsAny(text: String, terms: List<String>) = terms.any(text::contains)
    private fun levelRank(level: AttentionLevel) = when (level) {
        AttentionLevel.LOW -> 0
        AttentionLevel.MEDIUM -> 1
        AttentionLevel.HIGH -> 2
        AttentionLevel.URGENT -> 3
    }
}
