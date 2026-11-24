package com.drinkit.spring.infra

import com.drinkit.test.ControlledClock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Clock
import java.time.ZoneId

internal class ClockAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ClockAutoConfiguration::class.java))

    @Test
    fun `gives the system clock of the default zone`() {
        runner.run { context ->
            // When
            val clock = context.getBean(Clock::class.java)

            // Then
            clock.zone shouldBe ZoneId.systemDefault()
        }
    }

    @Test
    fun `backs off when the application declares its own clock`() {
        // Given
        val ownClock = ControlledClock()
        val withOwnClock = runner.withBean(Clock::class.java, { ownClock })

        withOwnClock.run { context ->
            // When & Then
            context.getBean(Clock::class.java) shouldBeSameInstanceAs ownClock
        }
    }

    @Test
    fun `declares no clock when disabled`() {
        // Given
        val disabled = runner.withPropertyValues("starters.spring.enabled=false")

        disabled.run { context ->
            // When & Then
            context.getBeansOfType(Clock::class.java) shouldBe emptyMap()
        }
    }
}
