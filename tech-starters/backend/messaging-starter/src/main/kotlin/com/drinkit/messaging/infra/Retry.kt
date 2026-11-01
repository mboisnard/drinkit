package com.drinkit.messaging.infra

import java.time.Duration
import kotlin.math.pow

internal sealed interface Retry {
    val attempt: Int
    val waitBeforeNext: Duration

    fun next(): Retry
}

internal data class FixedDelayRetry(private val delay: Duration, override val attempt: Int = 1) : Retry {

    init {
        require(delay.isPositive) { "The retry delay must be positive" }
        require(attempt >= 1) { "Attempts count from 1" }
    }

    override val waitBeforeNext: Duration get() = delay

    override fun next(): Retry = copy(attempt = attempt + 1)
}

internal data class ExponentialBackoffRetry(private val initialDelay: Duration, override val attempt: Int = 1) : Retry {

    init {
        require(initialDelay.isPositive) { "The retry delay must be positive" }
        require(initialDelay <= MAX_DELAY) { "The retry delay must not exceed the longest wait, $MAX_DELAY" }
        require(attempt >= 1) { "Attempts count from 1" }
    }

    override val waitBeforeNext: Duration
        get() = Duration.ofMillis(
            minOf(initialDelay.toMillis() * MULTIPLIER.pow(attempt - 1), MAX_DELAY.toMillis().toDouble()).toLong(),
        )

    override fun next(): Retry = copy(attempt = attempt + 1)

    companion object {
        private const val MULTIPLIER = 2.0
        private val MAX_DELAY = Duration.ofHours(1)
    }
}
