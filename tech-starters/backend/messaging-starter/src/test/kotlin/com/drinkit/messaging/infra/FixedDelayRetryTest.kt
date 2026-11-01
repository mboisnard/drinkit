package com.drinkit.messaging.infra

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import java.time.Duration

internal class FixedDelayRetryTest {

    @Test
    fun `gives each following attempt the same delay`() {
        val firstAttempt = FixedDelayRetry(delay = Duration.ofSeconds(30))
        val attempts = generateSequence<Retry>(firstAttempt) { it.next() }.take(3).toList()

        attempts.map { it.attempt to it.waitBeforeNext } shouldContainExactly
            listOf(1 to Duration.ofSeconds(30), 2 to Duration.ofSeconds(30), 3 to Duration.ofSeconds(30))
    }

    @Test
    fun `refuses an invalid retry`() {
        shouldThrow<IllegalArgumentException> { FixedDelayRetry(delay = Duration.ZERO) }
        shouldThrow<IllegalArgumentException> { FixedDelayRetry(delay = Duration.ofSeconds(30), attempt = 0) }
    }
}
