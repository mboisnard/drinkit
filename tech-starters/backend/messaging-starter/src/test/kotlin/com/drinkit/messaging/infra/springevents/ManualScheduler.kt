package com.drinkit.messaging.infra.springevents

import java.time.Duration

internal class ManualScheduler {

    private val pending = ArrayDeque<() -> Unit>()
    val delays = mutableListOf<Duration>()

    fun schedule(delay: Duration, task: () -> Unit) {
        delays += delay
        pending += task
    }

    fun pendingCount(): Int = pending.size

    // Until nothing is left: a failed delivery schedules its next attempt
    fun runAll() {
        while (pending.isNotEmpty()) {
            pending.removeFirst().invoke()
        }
    }
}
