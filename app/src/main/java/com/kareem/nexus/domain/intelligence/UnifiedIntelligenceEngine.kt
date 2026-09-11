package com.kareem.nexus.domain.intelligence

import com.kareem.nexus.core.model.*
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Value-first product interpretation on top of the cheap deterministic classifier. */
object UnifiedIntelligenceEngine {
    private val waitingTerms = listOf(
        "waiting for", "awaiting", "pending from", "still waiting", "waiting on",
        "منتظر", "مستني", "في انتظار", "لسه مستني", "بانتظار", "معلق عند",
    )

    fun deriveOpenLoop(
        observation: Observation,
        understanding: ObservationUnderstanding,
        situationId: String? = null,
        now: Long = System.currentTimeMillis(),
    ): OpenLoop? {
        if (understanding.isNoise || understanding.kind == SignalKind.INFORMATION || understanding.actions.isEmpty()) return null

        val normalized = ContextIntelligence.normalize(observation.rawText)
        val waiting = containsAny(normalized, waitingTerms)
        val party = understanding.facts.firstOrNull { it.kind in setOf(FactKind.ORGANIZATION, FactKind.PERSON) }?.value
        val amount = understanding.facts.firstOrNull { it.kind == FactKind.AMOUNT }?.value
        val currency = understanding.facts.firstOrNull { it.kind == FactKind.CURRENCY }?.value
        val dueAt = dueAt(observation.rawText, now)
        val kind = when (understanding.kind) {
            SignalKind.REQUEST -> if (waiting) OpenLoopKind.WAITING_ON else OpenLoopKind.NEEDS_REPLY
            SignalKind.PAYMENT -> OpenLoopKind.PAYMENT
            SignalKind.APPOINTMENT, SignalKind.REMINDER -> OpenLoopKind.UPCOMING
            SignalKind.DELIVERY -> OpenLoopKind.DELIVERY
            SignalKind.FAILURE -> OpenLoopKind.FAILURE
            SignalKind.FOLLOW_UP -> if (waiting) OpenLoopKind.WAITING_ON else OpenLoopKind.FOLLOW_UP
            SignalKind.INFORMATION -> return null
        }

        val title = when (kind) {
            OpenLoopKind.NEEDS_REPLY -> if (party != null) "$party needs a reply" else "Reply needed"
            OpenLoopKind.NEEDS_ACTION -> party?.let { "Action needed for $it" } ?: "Action needed"
            OpenLoopKind.WAITING_ON -> party?.let { "Waiting on $it" } ?: "Waiting on a response"
            OpenLoopKind.UPCOMING -> party?.let { "Upcoming with $it" } ?: "Upcoming commitment"
            OpenLoopKind.PAYMENT -> buildString {
                append(party?.let { "$it payment" } ?: "Payment needs attention")
                if (amount != null) {
                    append(" · ")
                    if (currency != null) append("$currency ")
                    append(amount)
                }
            }
            OpenLoopKind.DELIVERY -> party?.let { "$it delivery" } ?: "Delivery or order update"
            OpenLoopKind.FAILURE -> party?.let { "$it needs review" } ?: "Something failed"
            OpenLoopKind.FOLLOW_UP -> party?.let { "Follow up with $it" } ?: "Follow-up needed"
        }

        val hasLaunchableSource = observation.source
            ?.takeIf { it != "NEXUS" && it.contains('.') }
            ?.isNotBlank() == true
        val sourceActions = understanding.actions
            .filterNot { action ->
                action.kind in setOf(NexusActionKind.OPEN_SOURCE, NexusActionKind.REPLY, NexusActionKind.RETRY) && !hasLaunchableSource
            }
            .sortedBy { action ->
                when {
                    kind == OpenLoopKind.NEEDS_REPLY && action.kind == NexusActionKind.REPLY -> 0
                    action.kind == NexusActionKind.OPEN_SOURCE -> 1
                    else -> 2
                }
            }

        val enrichedActions = buildList {
            addAll(sourceActions)
            if (sourceActions.none { it.kind == NexusActionKind.REMIND } && kind != OpenLoopKind.DELIVERY) {
                add(ContextAction(NexusActionKind.REMIND, "Remind later"))
            }
            if (sourceActions.none { it.kind == NexusActionKind.MARK_RESOLVED }) {
                add(ContextAction(NexusActionKind.MARK_RESOLVED, "Done"))
            }
        }.distinctBy { it.kind }.take(4)

        return OpenLoop(
            id = "loop_${observation.id}",
            observationId = observation.id,
            situationId = situationId,
            kind = kind,
            title = title,
            detail = observation.rawText.trim().take(280),
            party = party,
            source = observation.source,
            state = if (kind == OpenLoopKind.WAITING_ON) OpenLoopState.WAITING else OpenLoopState.OPEN,
            priority = understanding.priority,
            dueAt = dueAt,
            snoozedUntil = null,
            actions = enrichedActions,
            createdAt = observation.createdAt,
            updatedAt = now,
        )
    }

    fun buildSituationBrief(
        situation: ContextSituation,
        loops: List<OpenLoop>,
        observations: List<Observation>,
        now: Long = System.currentTimeMillis(),
    ): SituationBrief {
        val relatedLoops = loops.filter {
            it.situationId == situation.id && it.state !in setOf(OpenLoopState.RESOLVED, OpenLoopState.DISMISSED)
        }
        val evidence = observations.filter { it.id in situation.observationIds }.sortedByDescending { it.createdAt }
        val latest = evidence.firstOrNull()
        val topLoop = relatedLoops.maxByOrNull { it.priority }
        val changedRecently = latest?.let { now - it.createdAt <= 24L * 60L * 60L * 1000L } == true

        val currentState = topLoop?.title ?: latest?.rawText?.take(180) ?: situation.summary
        val whatChanged = when {
            latest == null -> "No source evidence is available."
            changedRecently && evidence.size > 1 -> "${evidence.size} connected signals; the latest arrived ${relativeAge(latest.createdAt, now)}."
            changedRecently -> "New evidence arrived ${relativeAge(latest.createdAt, now)}."
            else -> "Last changed ${relativeAge(latest.createdAt, now)}."
        }
        val nextStep = topLoop?.let { loop ->
            loop.actions.firstOrNull { it.kind !in setOf(NexusActionKind.MARK_RESOLVED, NexusActionKind.REMIND) }?.label
                ?: if (loop.kind == OpenLoopKind.WAITING_ON) "Keep waiting or mark resolved" else "Review and resolve"
        }

        return SituationBrief(
            situationId = situation.id,
            title = situation.title,
            currentState = currentState,
            whatChanged = whatChanged,
            nextStep = nextStep,
            openLoopCount = relatedLoops.size,
            evidenceCount = evidence.size,
            priority = maxOf(situation.priority, relatedLoops.maxOfOrNull { it.priority } ?: 0.0),
            lastUpdatedAt = situation.lastUpdatedAt,
        )
    }

    fun memoryImportance(understanding: ObservationUnderstanding): Double = when {
        understanding.isNoise -> 0.05
        understanding.kind in setOf(SignalKind.PAYMENT, SignalKind.FAILURE, SignalKind.APPOINTMENT) -> 0.9
        understanding.kind in setOf(SignalKind.REQUEST, SignalKind.FOLLOW_UP) -> 0.8
        understanding.facts.isNotEmpty() -> 0.65
        else -> 0.35
    }

    fun searchableText(observation: Observation, understanding: ObservationUnderstanding): String = buildString {
        append(observation.rawText)
        append(' ')
        append(observation.source.orEmpty())
        understanding.facts.forEach { fact ->
            append(' ')
            append(fact.value)
            append(' ')
            append(fact.normalizedValue)
        }
        append(' ')
        append(understanding.kind.name.replace('_', ' '))
    }

    fun dueAt(text: String, now: Long = System.currentTimeMillis()): Long? {
        val normalized = ContextIntelligence.normalize(text)
        val zone = ZoneId.systemDefault()
        val current = Instant.ofEpochMilli(now).atZone(zone)
        fun at(dayOffset: Long, time: LocalTime): Long {
            var target = current.toLocalDate().plusDays(dayOffset).atTime(time).atZone(zone)
            if (!target.isAfter(current)) target = target.plusDays(1)
            return target.toInstant().toEpochMilli()
        }

        return when {
            containsAny(normalized, listOf("tonight", "الليله", "الليلة", "المساء", "بالليل")) -> at(0, LocalTime.of(19, 0))
            containsAny(normalized, listOf("tomorrow", "بكرة", "بكره", "غدا", "غداً")) -> at(1, LocalTime.of(9, 0))
            containsAny(normalized, listOf("today", "اليوم", "النهاردة", "النهارده")) -> at(0, LocalTime.of(18, 0))
            containsAny(normalized, listOf("next week", "الاسبوع الجاي", "الأسبوع الجاي")) -> at(7, LocalTime.of(9, 0))
            else -> null
        }
    }

    private fun relativeAge(timestamp: Long, now: Long): String {
        val minutes = ChronoUnit.MINUTES.between(Instant.ofEpochMilli(timestamp), Instant.ofEpochMilli(now)).coerceAtLeast(0)
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 24 * 60 -> "${minutes / 60}h ago"
            else -> "${minutes / (24 * 60)}d ago"
        }
    }

    private fun containsAny(text: String, terms: List<String>): Boolean =
        terms.any { text.contains(ContextIntelligence.normalize(it)) }
}
