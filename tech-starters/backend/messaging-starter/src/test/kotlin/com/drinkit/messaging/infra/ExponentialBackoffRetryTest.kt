package com.drinkit.messaging.infra

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import java.time.Duration

internal class ExponentialBackoffRetryTest {

    @Test
    fun `gives each following attempt twice the previous wait, up to one hour`() {
        val firstAttempt = ExponentialBackoffRetry(initialDelay = Duration.ofMinutes(10))
        val attempts = generateSequence<Retry>(firstAttempt) { it.next() }.take(5).toList()

        attempts.map { it.attempt to it.waitBeforeNext.toMinutes() } shouldContainExactly
            listOf(1 to 10L, 2 to 20L, 3 to 40L, 4 to 60L, 5 to 60L)
    }

    @Test
    fun `refuses an invalid retry`() {
        shouldThrow<IllegalArgumentException> { ExponentialBackoffRetry(initialDelay = Duration.ZERO) }
        shouldThrow<IllegalArgumentException> { ExponentialBackoffRetry(initialDelay = Duration.ofHours(2)) }
        shouldThrow<IllegalArgumentException> {
            ExponentialBackoffRetry(initialDelay = Duration.ofMinutes(10), attempt = 0)
        }
    }
}
