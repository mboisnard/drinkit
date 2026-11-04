package com.drinkit.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

internal class ControlledClockTest {

    private val delegate = ControlledClock().fix(NOW)

    @Test
    fun `a clock fixed now stays at that instant while its delegate moves`() {
        // Given
        val clock = ControlledClock(delegate).fix()

        // When
        delegate.add(Duration.ofHours(1))

        // Then
        clock.instant() shouldBe NOW
    }

    @Test
    fun `adding a duration moves a fixed clock by that duration`() {
        // Given
        val clock = ControlledClock().fix(NOW)

        // When
        clock.add(Duration.ofMinutes(16))

        // Then
        clock.instant() shouldBe Instant.parse("2026-11-03T10:31:30Z")
    }

    @Test
    fun `adding a duration to a clock that is not fixed fails`() {
        shouldThrow<IllegalArgumentException> { ControlledClock().add(Duration.ofMinutes(16)) }
    }

    @Test
    fun `a reset clock follows its delegate again`() {
        // Given
        val clock = ControlledClock(delegate).fix(Instant.parse("2020-01-01T00:00:00Z"))

        // When
        clock.reset()

        // Then
        clock.instant() shouldBe NOW
    }

    @Test
    fun `a clock derived with another zone is in that zone`() {
        ControlledClock().withZone(PARIS).zone shouldBe PARIS
    }

    @Test
    fun `a clock derived with another zone follows the time of the original`() {
        // Given
        val clock = ControlledClock()
        val parisClock = clock.withZone(PARIS)

        // When
        clock.fix(NOW).add(Duration.ofMinutes(16))

        // Then
        parisClock.instant() shouldBe Instant.parse("2026-11-03T10:31:30Z")
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-11-03T10:15:30Z")
        val PARIS: ZoneId = ZoneId.of("Europe/Paris")
    }
}
