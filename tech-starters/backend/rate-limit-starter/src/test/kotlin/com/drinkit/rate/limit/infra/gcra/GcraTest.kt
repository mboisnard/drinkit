package com.drinkit.rate.limit.infra.gcra

import com.drinkit.rate.limit.RateLimit
import com.drinkit.rate.limit.RateLimitRule
import com.drinkit.rate.limit.infra.gcra.Outcome.Allowed
import com.drinkit.rate.limit.infra.gcra.Outcome.Refused
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

internal class GcraTest {

    private val hour = Duration.ofHours(1)

    private val fivePerHour = RateLimitRule("signin", listOf(RateLimit(5, hour)))

    private val key = Key()

    @Test
    fun `a first call is allowed and books the next arrival one interval later`() {
        // When
        val outcome = key.call(fivePerHour, at("10:00"))

        // Then
        outcome shouldBe Allowed(mapOf(hour to at("10:12")))
    }

    @Test
    fun `a whole limit of calls is allowed at once, the last one exactly at the limit`() {
        // Given
        repeat(4) { key.call(fivePerHour, at("10:00")) }

        // When
        val fifth = key.call(fivePerHour, at("10:00"))

        // Then
        fifth shouldBe Allowed(mapOf(hour to at("11:00")))
    }

    @Test
    fun `the call past the limit is refused until the next slot frees up`() {
        // Given
        repeat(5) { key.call(fivePerHour, at("10:00")) }

        // When
        val sixth = key.call(fivePerHour, at("10:05"))

        // Then
        sixth shouldBe Refused(Duration.ofMinutes(7))
    }

    @Test
    fun `a call exactly at its retry instant is allowed`() {
        // Given
        repeat(5) { key.call(fivePerHour, at("10:00")) }

        // When
        val atRetry = key.call(fivePerHour, at("10:12"))

        // Then
        atRetry shouldBe Allowed(mapOf(hour to at("11:12")))
    }

    @Test
    fun `slots free up one by one, one interval apart, after a burst`() {
        // Given
        repeat(5) { key.call(fivePerHour, at("10:00")) }
        key.call(fivePerHour, at("10:12"))

        // When
        val secondAtTheSameTime = key.call(fivePerHour, at("10:12"))

        // Then
        secondAtTheSameTime shouldBe Refused(Duration.ofMinutes(12))
    }

    @Test
    fun `a key idle for a whole period gets its whole limit back`() {
        // Given
        repeat(5) { key.call(fivePerHour, at("10:00")) }

        // When
        val outcomes = List(5) { key.call(fivePerHour, at("11:00")) }

        // Then
        outcomes.last() shouldBe Allowed(mapOf(hour to at("12:00")))
    }

    @Test
    fun `a refused call does not push the next arrival further`() {
        // Given
        repeat(5) { key.call(fivePerHour, at("10:00")) }
        key.call(fivePerHour, at("10:05"))

        // When
        val atFirstRetry = key.call(fivePerHour, at("10:12"))

        // Then
        atFirstRetry shouldBe Allowed(mapOf(hour to at("11:12")))
    }

    @Test
    fun `a call costing several slots needs as many free slots`() {
        // Given
        repeat(3) { key.call(fivePerHour, at("10:00")) }

        // When
        val costingThree = key.call(fivePerHour, at("10:00"), cost = 3)
        val costingTwo = key.call(fivePerHour, at("10:00"), cost = 2)

        // Then
        costingThree shouldBe Refused(Duration.ofMinutes(12))
        costingTwo shouldBe Allowed(mapOf(hour to at("11:00")))
    }

    @Test
    fun `at most twice the limit minus one calls pass within one period`() {
        // Given
        val burst = List(5) { at("10:00") }
        val oneEveryMinute = (1..59L).map { at("10:00").plus(Duration.ofMinutes(it)) }

        // When
        val allowed = (burst + oneEveryMinute).count { key.call(fivePerHour, it) is Allowed }

        // Then
        allowed shouldBe 9
    }

    @Test
    fun `a clock behind the stored arrival still counts the call`() {
        // Given
        key.call(fivePerHour, at("10:00"))

        // When
        val fromALateClock = key.call(fivePerHour, at("09:58"))

        // Then
        fromALateClock shouldBe Allowed(mapOf(hour to at("10:24")))
    }

    @Test
    fun `a rule is refused with the longest delay among its refused limits`() {
        // Given
        val rule = RateLimitRule("signin", listOf(RateLimit(1, Duration.ofMinutes(10)), RateLimit(2, hour)))
        key.call(rule, at("10:00"))
        key.call(rule, at("10:10"))

        // When
        val refusedByBoth = key.call(rule, at("10:15"))

        // Then
        refusedByBoth shouldBe Refused(Duration.ofMinutes(15))
    }

    private class Key {
        private var theoreticalArrivals = emptyMap<Duration, Instant>()

        fun call(rule: RateLimitRule, now: Instant, cost: Long = 1): Outcome =
            Gcra.decide(rule, theoreticalArrivals, now, cost)
                .also { if (it is Allowed) theoreticalArrivals = it.theoreticalArrivals }
    }

    private fun at(time: String): Instant = Instant.parse("2026-11-09T$time:00Z")
}
