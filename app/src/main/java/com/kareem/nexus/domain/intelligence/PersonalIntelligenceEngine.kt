package com.kareem.nexus.domain.intelligence

import com.kareem.nexus.core.model.*
import java.security.MessageDigest
import kotlin.math.max

/**
 * NEXUS 2.0 structured interpretation layer.
 *
 * This engine is deliberately deterministic and fully local. It produces a stable structured
 * representation that can later be enriched by an on-device model without changing the UI or
 * persistence contracts. Cheap rules remain useful as a privacy-preserving first pass and a
 * fallback when model inference is unavailable.
 */
object PersonalIntelligenceEngine {
    private val requestTerms = listOf(
        "please send", "please reply", "please confirm", "kindly send", "kindly confirm",
        "can you", "could you", "would you", "send me", "send us", "reply", "confirm",
        "برجاء", "يرجى", "من فضلك", "ممكن", "ابعت", "ابعث", "ارسل", "أرسل", "رد", "أكد", "اكد",
    )
    private val paymentTerms = listOf(
        "payment", "invoice", "bill", "credit card", "card payment", "amount due", "pay",
        "دفع", "سداد", "فاتورة", "بطاقة", "بطاقتك", "ائتماني", "الحد الائتماني", "مستحق",
    )
    private val appointmentTerms = listOf(
        "appointment", "meeting", "reservation", "booking", "visit", "calendar", "tomorrow",
        "موعد", "اجتماع", "حجز", "زيارة", "كشف", "بكرة", "بكره", "غدا", "غداً",
    )
    private val deliveryTerms = listOf(
        "delivery", "shipment", "courier", "out for delivery", "arriving", "order #",
        "توصيل", "شحنة", "مندوب", "اوردر", "أوردر", "طلبك",
    )
    private val failureTerms = listOf(
        "failed", "couldn't", "cannot", "error", "something went wrong", "declined", "rejected",
        "فشل", "خطأ", "تعذر", "مشكلة", "رفض", "مرفوض", "لم يتم",
    )
    private val followTerms = listOf(
        "follow up", "follow-up", "waiting for", "pending", "remind", "reminder",
        "متابعة", "منتظر", "مستني", "تذكير", "فكرني",
    )
    private val resolvedTerms = listOf(
        "completed", "resolved", "paid successfully", "payment received", "delivered successfully",
        "no action required", "cancelled", "canceled", "تم", "تم الدفع", "تم السداد", "تم التسليم",
        "تم الالغاء", "تم الإلغاء",
    )
    private val highUrgencyTerms = listOf(
        "urgent", "asap", "immediately", "today", "deadline", "due today", "action required",
        "عاجل", "ضروري", "فورا", "فوراً", "اليوم", "النهاردة", "مطلوب إجراء",
    )
    private val noiseTerms = listOf(
        "rate your", "review your", "we'd love your thoughts", "tell us what you think", "leave a review",
        "added to their story", "sent a reel", "reacted to", "liked your", "new story", "promo code",
        "discount", "special offer", "sale", "verification code", "otp", "response ready", "tap to return",
        "قيم", "قيّم", "شاركنا رأيك", "رأيك", "عرض خاص", "خصم", "كود التحقق",
    )

    fun interpret(
        observation: Observation,
        now: Long = System.currentTimeMillis(),
    ): ObservationUnderstanding {
        val normalized = ContextIntelligence.normalize(observation.rawText)
        val source = ContextIntelligence.normalize(observation.source.orEmpty())
        val isSelf = source.contains("com.kareem.nexus")
        val isNoise = isSelf || containsAny(normalized, noiseTerms) || normalized.length < 4
        val resolved = containsAny(normalized, resolvedTerms)

        val kind = when {
            isNoise -> SignalKind.INFORMATION
            containsAny(normalized, failureTerms) -> SignalKind.FAILURE
            containsAny(normalized, paymentTerms) -> SignalKind.PAYMENT
            containsAny(normalized, appointmentTerms) -> SignalKind.APPOINTMENT
            containsAny(normalized, deliveryTerms) -> SignalKind.DELIVERY
            looksLikeRequest(normalized) -> SignalKind.REQUEST
            containsAny(normalized, followTerms) -> SignalKind.FOLLOW_UP
            else -> SignalKind.INFORMATION
        }

        val facts = extractFacts(observation.rawText, observation.source)
        val priority = priorityFor(kind, normalized, isNoise, resolved, observation.createdAt, now)
        val confidence = confidenceFor(kind, normalized, facts, isNoise)
        val title = titleFor(kind, facts, resolved)
        val summary = summaryFor(kind, observation.rawText, facts, resolved)
        val actions = if (isNoise || resolved) emptyList() else actionsFor(kind, observation, facts)

        return ObservationUnderstanding(
            observationId = observation.id,
            kind = kind,
            title = title,
            summary = summary,
            facts = facts,
            actions = actions,
            priority = priority,
            confidence = confidence,
            isNoise = isNoise,
            analyzedAt = now,
        )
    }

    fun topOfMind(
        observations: List<Observation>,
        existingActions: List<PreparedAction> = emptyList(),
        limit: Int = 5,
        now: Long = System.currentTimeMillis(),
    ): List<TopOfMindItem> {
        val terminalObservationIds = existingActions
            .filter { it.state in setOf(ActionState.COMPLETED, ActionState.REJECTED) }
            .map { it.id.removePrefix("action_signal_").removePrefix("action_commitment_") }
            .toSet()

        return observations.asSequence()
            .filter { it.type != ObservationType.APP_USAGE }
            .filter { it.createdAt >= now - 7L * 24 * 60 * 60 * 1000 }
            .filterNot { it.id in terminalObservationIds }
            .map { it to interpret(it, now) }
            .filter { (_, understanding) ->
                !understanding.isNoise &&
                    understanding.kind != SignalKind.INFORMATION &&
                    understanding.actions.isNotEmpty() &&
                    understanding.priority >= 0.45
            }
            .sortedWith(
                compareByDescending<Pair<Observation, ObservationUnderstanding>> { it.second.priority }
                    .thenByDescending { it.first.createdAt }
            )
            .distinctBy { (observation, understanding) -> dedupeKey(observation, understanding) }
            .take(limit)
            .map { (observation, understanding) ->
                TopOfMindItem(
                    id = "top_${observation.id}",
                    observationId = observation.id,
                    title = understanding.title,
                    summary = understanding.summary,
                    kind = understanding.kind,
                    priority = understanding.priority,
                    confidence = understanding.confidence,
                    actions = understanding.actions,
                    createdAt = observation.createdAt,
                )
            }
            .toList()
    }

    fun buildSituations(
        observations: List<Observation>,
        limit: Int = 8,
        now: Long = System.currentTimeMillis(),
    ): List<ContextSituation> {
        val interpreted = observations.asSequence()
            .filter { it.type != ObservationType.APP_USAGE }
            .filter { it.createdAt >= now - 30L * 24 * 60 * 60 * 1000 }
            .map { it to interpret(it, now) }
            .filterNot { it.second.isNoise }
            .toList()

        val buckets = linkedMapOf<String, MutableList<Pair<Observation, ObservationUnderstanding>>>()
        interpreted.forEach { pair ->
            val key = situationKey(pair.first, pair.second)
            if (key != null) buckets.getOrPut(key) { mutableListOf() }.add(pair)
        }

        return buckets.values.asSequence()
            .filter { it.size >= 2 }
            .map { rows ->
                val sorted = rows.sortedByDescending { it.first.createdAt }
                val understandings = sorted.map { it.second }
                val allFacts = understandings.flatMap { it.facts }.distinctBy { "${it.kind}:${it.normalizedValue}" }
                val top = understandings.maxBy { it.priority }
                val kind = situationKind(understandings)
                val title = situationTitle(allFacts, top, sorted.first().first.source)
                val summary = situationSummary(sorted, understandings)
                ContextSituation(
                    id = stableId("situation|${situationKey(sorted.first().first, top)}"),
                    title = title,
                    summary = summary,
                    kind = kind,
                    state = ResolutionState.OPEN,
                    observationIds = sorted.map { it.first.id },
                    facts = allFacts,
                    actions = top.actions.take(3),
                    priority = understandings.maxOf { it.priority },
                    confidence = understandings.map { it.confidence }.average().coerceIn(0.0, 1.0),
                    createdAt = sorted.minOf { it.first.createdAt },
                    lastUpdatedAt = sorted.maxOf { it.first.createdAt },
                )
            }
            .sortedWith(compareByDescending<ContextSituation> { it.priority }.thenByDescending { it.lastUpdatedAt })
            .take(limit)
            .toList()
    }

    fun extractFacts(text: String, source: String? = null): List<ExtractedFact> {
        val facts = mutableListOf<ExtractedFact>()
        val normalized = ContextIntelligence.normalize(text)

        Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE).findAll(text).forEach { match ->
            facts += ExtractedFact(FactKind.URL, match.value.trimEnd('.', ',', ')'), confidence = 0.99)
        }

        val amountRegex = Regex(
            "(?i)(?:EGP|LE|ج\\.?م|جنيه)?\\s*([0-9٠-٩][0-9٠-٩,.]{1,14})\\s*(?:EGP|LE|ج\\.?م|جنيه)?"
        )
        amountRegex.findAll(text).forEach { match ->
            val raw = match.groupValues.getOrNull(1).orEmpty()
            val normalizedNumber = normalizeDigits(raw).replace(",", "")
            if (normalizedNumber.toDoubleOrNull() != null) {
                facts += ExtractedFact(FactKind.AMOUNT, raw, normalizedNumber, 0.72)
                val currency = when {
                    match.value.contains("EGP", true) || match.value.contains("جنيه") || match.value.contains("ج.م") -> "EGP"
                    match.value.contains("LE", true) -> "EGP"
                    else -> null
                }
                if (currency != null) facts += ExtractedFact(FactKind.CURRENCY, currency, currency, 0.95)
            }
        }

        Regex("(?i)\\b(?:order|invoice|ref|reference|case|ticket)\\s*[#:]?\\s*([A-Z0-9-]{4,})").findAll(text).forEach { match ->
            facts += ExtractedFact(FactKind.ORDER, match.groupValues[1], match.groupValues[1].uppercase(), 0.85)
        }

        val organization = inferOrganization(text, source)
        if (organization != null) {
            facts += ExtractedFact(FactKind.ORGANIZATION, organization, ContextIntelligence.normalize(organization), 0.68)
        }

        Regex("(?i)\\b(?:today|tomorrow|tonight|next week|بكرة|بكره|اليوم|النهاردة|غدا|غداً)\\b").find(normalized)?.let {
            facts += ExtractedFact(FactKind.DATE, it.value, it.value, 0.7)
        }

        Regex("(?i)(?:at|الساعة|الساعه)\\s*([0-9٠-٩]{1,2}(?::[0-9٠-٩]{2})?\\s*(?:am|pm)?)").find(text)?.let {
            val value = it.groupValues[1]
            facts += ExtractedFact(FactKind.TIME, value, normalizeDigits(value), 0.82)
        }

        return facts.distinctBy { "${it.kind}:${it.normalizedValue}" }.take(12)
    }

    private fun actionsFor(
        kind: SignalKind,
        observation: Observation,
        facts: List<ExtractedFact>,
    ): List<ContextAction> {
        val actions = mutableListOf<ContextAction>()
        if (!observation.source.isNullOrBlank()) {
            actions += ContextAction(NexusActionKind.OPEN_SOURCE, "Open source", observation.source)
        }
        when (kind) {
            SignalKind.REQUEST -> actions += ContextAction(NexusActionKind.REPLY, "Open message", observation.source)
            SignalKind.PAYMENT -> {
                actions += ContextAction(NexusActionKind.REMIND, "Remind me")
                actions += ContextAction(NexusActionKind.MARK_RESOLVED, "Mark resolved")
            }
            SignalKind.APPOINTMENT -> {
                actions += ContextAction(NexusActionKind.ADD_TO_CALENDAR, "Add to calendar", factsValue(facts, FactKind.DATE))
                actions += ContextAction(NexusActionKind.REMIND, "Remind me")
            }
            SignalKind.DELIVERY -> actions += ContextAction(NexusActionKind.TRACK, "Track", factsValue(facts, FactKind.ORDER))
            SignalKind.FAILURE -> actions += ContextAction(NexusActionKind.RETRY, "Review failure")
            SignalKind.FOLLOW_UP, SignalKind.REMINDER -> actions += ContextAction(NexusActionKind.REMIND, "Remind me")
            SignalKind.INFORMATION -> Unit
        }
        if (kind != SignalKind.INFORMATION && actions.none { it.kind == NexusActionKind.MARK_RESOLVED }) {
            actions += ContextAction(NexusActionKind.MARK_RESOLVED, "Done")
        }
        return actions.distinctBy { it.kind }.take(3)
    }

    private fun titleFor(kind: SignalKind, facts: List<ExtractedFact>, resolved: Boolean): String {
        val organization = factsValue(facts, FactKind.ORGANIZATION)
        val base = when (kind) {
            SignalKind.REQUEST -> if (organization != null) "$organization is waiting for you" else "Someone may be waiting for you"
            SignalKind.PAYMENT -> if (organization != null) "$organization payment" else "Payment needs attention"
            SignalKind.APPOINTMENT -> "Upcoming commitment"
            SignalKind.DELIVERY -> "Order or delivery update"
            SignalKind.FAILURE -> "Something failed"
            SignalKind.FOLLOW_UP -> "Follow-up may be needed"
            SignalKind.REMINDER -> "Reminder"
            SignalKind.INFORMATION -> "Context update"
        }
        return if (resolved && kind != SignalKind.INFORMATION) "$base · resolved" else base
    }

    private fun summaryFor(
        kind: SignalKind,
        original: String,
        facts: List<ExtractedFact>,
        resolved: Boolean,
    ): String {
        if (resolved) return original.take(220)
        val amount = factsValue(facts, FactKind.AMOUNT)
        val currency = factsValue(facts, FactKind.CURRENCY)
        return when {
            kind == SignalKind.PAYMENT && amount != null -> buildString {
                append("Payment-related message")
                append(" involving ")
                if (currency != null) append("$currency ")
                append(amount)
                append(". ")
                append(original.take(160))
            }
            else -> original.take(220)
        }
    }

    private fun priorityFor(
        kind: SignalKind,
        text: String,
        isNoise: Boolean,
        resolved: Boolean,
        createdAt: Long,
        now: Long,
    ): Double {
        if (isNoise || resolved) return 0.0
        var score = when (kind) {
            SignalKind.FAILURE -> 0.82
            SignalKind.PAYMENT -> 0.80
            SignalKind.APPOINTMENT -> 0.76
            SignalKind.REQUEST -> 0.70
            SignalKind.DELIVERY -> 0.60
            SignalKind.FOLLOW_UP, SignalKind.REMINDER -> 0.58
            SignalKind.INFORMATION -> 0.15
        }
        if (containsAny(text, highUrgencyTerms)) score += 0.17
        val ageHours = ((now - createdAt).coerceAtLeast(0L) / 3_600_000.0)
        val freshness = when {
            ageHours <= 6 -> 0.06
            ageHours <= 24 -> 0.03
            ageHours >= 96 -> -0.12
            else -> 0.0
        }
        return (score + freshness).coerceIn(0.0, 1.0)
    }

    private fun confidenceFor(
        kind: SignalKind,
        text: String,
        facts: List<ExtractedFact>,
        isNoise: Boolean,
    ): Double {
        if (isNoise) return 0.95
        var value = if (kind == SignalKind.INFORMATION) 0.45 else 0.68
        if (facts.isNotEmpty()) value += 0.08
        if (kind == SignalKind.REQUEST && looksLikeRequest(text)) value += 0.08
        if (kind == SignalKind.PAYMENT && containsAny(text, paymentTerms)) value += 0.08
        if (kind == SignalKind.APPOINTMENT && containsAny(text, appointmentTerms)) value += 0.08
        return value.coerceIn(0.0, 0.96)
    }

    private fun situationKey(observation: Observation, understanding: ObservationUnderstanding): String? {
        val org = factsValue(understanding.facts, FactKind.ORGANIZATION)?.let(ContextIntelligence::normalize)
        if (!org.isNullOrBlank()) return "org:$org"

        val order = factsValue(understanding.facts, FactKind.ORDER)?.lowercase()
        if (!order.isNullOrBlank()) return "order:$order"

        val source = ContextIntelligence.normalize(observation.source.orEmpty())
        if (source.isNotBlank() && understanding.kind != SignalKind.INFORMATION) {
            return "source:$source:${understanding.kind.name.lowercase()}"
        }

        val concept = when {
            containsAny(ContextIntelligence.normalize(observation.rawText), listOf("github", "android", "kotlin", "compose", "build", "apk", "release", "nexus")) -> "development"
            containsAny(ContextIntelligence.normalize(observation.rawText), listOf("quotation", "vendor", "purchase order", "contractor", "عرض سعر", "مقاول")) -> "procurement"
            containsAny(ContextIntelligence.normalize(observation.rawText), listOf("doctor", "hospital", "scan", "lab", "دكتور", "مستشفي", "تحليل", "اشعة")) -> "health"
            else -> null
        }
        return concept?.let { "concept:$it" }
    }

    private fun situationKind(understandings: List<ObservationUnderstanding>): SituationKind = when {
        understandings.any { it.kind == SignalKind.PAYMENT || it.kind == SignalKind.DELIVERY } -> SituationKind.PURCHASE
        understandings.any { it.kind == SignalKind.APPOINTMENT } -> SituationKind.ERRAND
        understandings.any { it.facts.any { fact -> fact.kind == FactKind.PROJECT } } -> SituationKind.PROJECT
        else -> SituationKind.COMMUNICATION
    }

    private fun situationTitle(
        facts: List<ExtractedFact>,
        strongest: ObservationUnderstanding,
        source: String?,
    ): String {
        factsValue(facts, FactKind.ORGANIZATION)?.let { return it }
        return when (strongest.kind) {
            SignalKind.PAYMENT -> "Payment thread"
            SignalKind.APPOINTMENT -> "Appointment thread"
            SignalKind.REQUEST -> "Open conversation"
            SignalKind.DELIVERY -> "Order thread"
            SignalKind.FAILURE -> "Issue requiring attention"
            SignalKind.FOLLOW_UP, SignalKind.REMINDER -> "Follow-up thread"
            SignalKind.INFORMATION -> source?.substringAfterLast('.')?.replaceFirstChar { it.uppercase() } ?: "Connected context"
        }
    }

    private fun situationSummary(
        rows: List<Pair<Observation, ObservationUnderstanding>>,
        understandings: List<ObservationUnderstanding>,
    ): String {
        val openKinds = understandings.map { it.kind }.filter { it != SignalKind.INFORMATION }.distinct()
        val label = openKinds.joinToString(", ") { it.name.lowercase().replace('_', ' ') }
        return if (label.isBlank()) {
            "${rows.size} related signals were connected into one context."
        } else {
            "${rows.size} related signals include $label. NEXUS grouped them so you can review one situation instead of separate notifications."
        }
    }

    private fun inferOrganization(text: String, source: String?): String? {
        val prefix = text.substringBefore('—').substringBefore('-').substringBefore(':').trim()
        if (prefix.length in 2..36 && prefix.any(Char::isLetter) && prefix.split(' ').size <= 5) {
            val normalizedPrefix = ContextIntelligence.normalize(prefix)
            val generic = listOf("payment", "appointment", "reminder", "notification", "message", "android")
            if (generic.none { normalizedPrefix == it }) return prefix
        }
        val sourceTail = source?.substringAfterLast('.')?.trim().orEmpty()
        return sourceTail.takeIf { it.length in 2..24 && it !in setOf("android", "systemui") }
            ?.replace('_', ' ')
            ?.replaceFirstChar { it.uppercase() }
    }

    private fun looksLikeRequest(text: String): Boolean {
        if (containsAny(text, requestTerms)) return true
        return text.contains('?') && containsAny(text, listOf("can you", "could you", "would you", "ممكن", "تقدر", "ينفع"))
    }

    private fun containsAny(text: String, terms: List<String>): Boolean =
        terms.any { text.contains(ContextIntelligence.normalize(it)) }

    private fun factsValue(facts: List<ExtractedFact>, kind: FactKind): String? =
        facts.firstOrNull { it.kind == kind }?.value

    private fun dedupeKey(observation: Observation, understanding: ObservationUnderstanding): String {
        val organization = factsValue(understanding.facts, FactKind.ORGANIZATION)?.let(ContextIntelligence::normalize).orEmpty()
        val order = factsValue(understanding.facts, FactKind.ORDER)?.lowercase().orEmpty()
        return listOf(understanding.kind.name, organization, order, observation.source.orEmpty()).joinToString("|")
    }

    private fun stableId(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .take(12)
        .joinToString("") { "%02x".format(it) }

    private fun normalizeDigits(value: String): String = value.map { c ->
        when (c) {
            in '٠'..'٩' -> ('0'.code + (c - '٠')).toChar()
            else -> c
        }
    }.joinToString("")
}
