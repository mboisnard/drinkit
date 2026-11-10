package com.drinkit.rate.limit.infra

import com.drinkit.rate.limit.RateLimit
import com.drinkit.rate.limit.RateLimitDecision.Allowed
import com.drinkit.rate.limit.RateLimitDecision.Refused
import com.drinkit.rate.limit.RateLimitRule
import com.drinkit.rate.limit.RateLimiter
import com.drinkit.test.ControlledClock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

internal abstract class RateLimiterTestContract {

    protected val clock = ControlledClock().fix(Instant.parse("2026-11-09T10:00:00Z"))

    abstract fun rateLimiter(): RateLimiter

    private val twoPerHour = RateLimitRule("signin", listOf(RateLimit(2, Duration.ofHours(1))))

    @Test
    fun `a key over its limit is refused until a slot frees up, while another key is not`() {
        // Given
        val limiter = rateLimiter()
        repeat(2) { limiter.consume(twoPerHour, "alice@drinkit.com") }

        // When
        val alice = limiter.consume(twoPerHour, "alice@drinkit.com")
        val bob = limiter.consume(twoPerHour, "bob@drinkit.com")

        // Then
        alice shouldBe Refused("signin", Duration.ofMinutes(30))
        bob shouldBe Allowed
    }

    @Test
    fun `a refused key is allowed again once its retry delay has passed`() {
        // Given
        val limiter = rateLimiter()
        repeat(2) { limiter.consume(twoPerHour, "alice@drinkit.com") }
        val refused = limiter.consume(twoPerHour, "alice@drinkit.com").shouldBeInstanceOf<Refused>()

        // When
        clock.add(refused.retryAfter)

        // Then
        limiter.consume(twoPerHour, "alice@drinkit.com") shouldBe Allowed
    }

    @Test
    fun `the strictest limit of a rule decides`() {
        // Given
        val rule = RateLimitRule(
            "signin",
            listOf(RateLimit(2, Duration.ofMinutes(10)), RateLimit(10, Duration.ofDays(1))),
        )
        val limiter = rateLimiter()
        repeat(2) { limiter.consume(rule, "alice@drinkit.com") }

        // When
        val third = limiter.consume(rule, "alice@drinkit.com")

        // Then
        third shouldBe Refused("signin", Duration.ofMinutes(5))
    }

    @Test
    fun `a refused rule charges none of its limits`() {
        // Given
        val rule = RateLimitRule(
            "signin",
            listOf(RateLimit(1, Duration.ofMinutes(10)), RateLimit(3, Duration.ofHours(1))),
        )
        val limiter = rateLimiter()
        limiter.consume(rule, "alice@drinkit.com")
        repeat(2) { limiter.consume(rule, "alice@drinkit.com") }
        clock.add(Duration.ofMinutes(10))

        // When
        val afterTheShortPeriod = limiter.consume(rule, "alice@drinkit.com")

        // Then
        afterTheShortPeriod shouldBe Allowed
    }

    @Test
    fun `the same key counts apart in two rules`() {
        // Given
        val otherRule = RateLimitRule("assistant", twoPerHour.limits)
        val limiter = rateLimiter()
        repeat(2) { limiter.consume(twoPerHour, "alice@drinkit.com") }

        // When
        val inTheOtherRule = limiter.consume(otherRule, "alice@drinkit.com")

        // Then
        inTheOtherRule shouldBe Allowed
    }

    @Test
    fun `a cost must be at least one and at most the smallest limit`() {
        // Given
        val limiter = rateLimiter()

        // When & Then
        shouldThrow<IllegalArgumentException> { limiter.consume(twoPerHour, "alice@drinkit.com", cost = 0) }
        shouldThrow<IllegalArgumentException> { limiter.consume(twoPerHour, "alice@drinkit.com", cost = 3) }
    }
}
