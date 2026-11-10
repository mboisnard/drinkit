package com.drinkit.rate.limit

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import java.time.Duration

internal class RateLimitRuleTest {

    private val perHour = RateLimit(5, Duration.ofHours(1))

    @Test
    fun `a limit needs a positive count and period`() {
        // When & Then
        shouldThrow<IllegalArgumentException> { RateLimit(0, Duration.ofHours(1)) }
        shouldThrow<IllegalArgumentException> { RateLimit(5, Duration.ZERO) }
    }

    @Test
    fun `a rule needs at least one limit`() {
        // When & Then
        shouldThrow<IllegalArgumentException> { RateLimitRule("signin", emptyList()) }
    }

    @Test
    fun `two limits of a rule cannot share a period`() {
        // When & Then
        shouldThrow<IllegalArgumentException> {
            RateLimitRule("signin", listOf(perHour, RateLimit(10, Duration.ofHours(1))))
        }
    }

    @Test
    fun `a rule needs a name, which counts it apart from the others`() {
        // When & Then
        shouldThrow<IllegalArgumentException> { RateLimitRule(" ", listOf(perHour)) }
    }
}
