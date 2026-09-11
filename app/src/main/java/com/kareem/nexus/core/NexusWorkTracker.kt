package com.kareem.nexus.core

import java.util.concurrent.atomic.AtomicInteger

/** Lightweight process-local accounting for user-triggered background intelligence work. */
object NexusWorkTracker {
    private val pending = AtomicInteger(0)

    fun begin() {
        pending.incrementAndGet()
    }

    fun end() {
        while (true) {
            val current = pending.get()
            if (current <= 0) return
            if (pending.compareAndSet(current, current - 1)) return
        }
    }

    fun isIdle(): Boolean = pending.get() == 0

    fun pendingCount(): Int = pending.get()
}
