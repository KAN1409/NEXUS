package com.kareem.nexus.domain.intelligence

import com.kareem.nexus.core.model.OpenLoop
import com.kareem.nexus.core.model.OpenLoopKind
import com.kareem.nexus.core.model.OpenLoopState

data class HomeSurface(
    val surfaced: List<OpenLoop>,
    val needsYou: List<OpenLoop>,
    val waitingOn: List<OpenLoop>,
    val upcoming: List<OpenLoop>,
)

/**
 * Pure Home surfacing policy.
 *
 * Persistence keeps every open loop. Home intentionally collapses duplicate evidence from the
 * same real-world situation, but it must not collapse unrelated topics just because they came
 * from the same organization/app. Keeping this logic pure makes it independently testable and
 * removes presentation timing from product correctness.
 */
object HomeSurfacePolicy {
    fun build(
        loops: List<OpenLoop>,
        limitPerSection: Int = 5,
    ): HomeSurface {
        val active = loops
            .asSequence()
            .filter { it.state in setOf(OpenLoopState.OPEN, OpenLoopState.WAITING) }
            .toList()

        val surfaced = active
            .groupBy(::surfaceKey)
            .values
            .mapNotNull { group ->
                group.maxWithOrNull(
                    compareBy<OpenLoop> { it.priority }
                        .thenBy { it.updatedAt }
                )
            }
            .sortedWith(
                compareByDescending<OpenLoop> { it.priority }
                    .thenBy { it.dueAt ?: Long.MAX_VALUE }
                    .thenByDescending { it.updatedAt }
            )

        val needsYou = surfaced.filter {
            it.kind !in setOf(OpenLoopKind.WAITING_ON, OpenLoopKind.DELIVERY, OpenLoopKind.UPCOMING)
        }.take(limitPerSection)

        val waitingOn = surfaced.filter {
            it.kind in setOf(OpenLoopKind.WAITING_ON, OpenLoopKind.DELIVERY)
        }.take(limitPerSection)

        val upcoming = surfaced
            .filter { it.kind == OpenLoopKind.UPCOMING }
            .sortedBy { it.dueAt ?: Long.MAX_VALUE }
            .take(limitPerSection)

        return HomeSurface(
            surfaced = surfaced,
            needsYou = needsYou,
            waitingOn = waitingOn,
            upcoming = upcoming,
        )
    }

    private fun surfaceKey(loop: OpenLoop): String {
        val situation = loop.situationId ?: "standalone:${loop.id}"
        val party = normalize(loop.party.orEmpty())
        return "$situation|$party|${topic(loop)}"
    }

    private fun topic(loop: OpenLoop): String {
        val text = normalize("${loop.title} ${loop.detail}")
        return when {
            containsAny(text, "fraud", "suspicious", "security", "unauthorized", "احتيال", "مشبوه", "غير مصرح") -> "security"
            containsAny(text, "transfer", "bank transfer", "تحويل", "حوالة") -> "transfer"
            containsAny(text, "invoice", "bill", "فاتورة") -> "invoice"
            containsAny(text, "card", "credit card", "بطاقة", "كارت") -> "card-payment"
            loop.kind == OpenLoopKind.PAYMENT -> "payment"
            loop.kind == OpenLoopKind.FAILURE -> "failure"
            loop.kind == OpenLoopKind.DELIVERY -> "delivery"
            loop.kind == OpenLoopKind.UPCOMING -> "upcoming"
            loop.kind == OpenLoopKind.NEEDS_REPLY -> "reply"
            loop.kind == OpenLoopKind.WAITING_ON -> "waiting"
            loop.kind == OpenLoopKind.FOLLOW_UP -> "follow-up"
            else -> loop.kind.name.lowercase()
        }
    }

    private fun containsAny(text: String, vararg terms: String): Boolean =
        terms.any { text.contains(normalize(it)) }

    private fun normalize(value: String): String = ContextIntelligence.normalize(value)
}
