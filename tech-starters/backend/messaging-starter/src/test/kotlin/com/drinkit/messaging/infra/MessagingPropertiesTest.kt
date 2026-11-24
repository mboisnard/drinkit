package com.drinkit.messaging.infra

import com.drinkit.messaging.infra.MessagingProperties.RetryProperties
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import java.time.Duration

internal class MessagingPropertiesTest {

    @Test
    fun `gives each following attempt twice the previous wait, up to one hour`() {
        // Given
        val waits = RetryProperties(delay = Duration.ofMinutes(10)).backOff().start()

        // When
        val minutes = List(5) { Duration.ofMillis(waits.nextBackOff()).toMinutes() }

        // Then
        minutes shouldContainExactly listOf(10L, 20L, 40L, 60L, 60L)
    }

    @Test
    fun `gives each following attempt the same wait when the exponential backoff is switched off`() {
        // Given
        val waits = RetryProperties(delay = Duration.ofSeconds(30), exponentialBackoff = false).backOff().start()

        // When
        val seconds = List(3) { Duration.ofMillis(waits.nextBackOff()).toSeconds() }

        // Then
        seconds shouldContainExactly listOf(30L, 30L, 30L)
    }

    @Test
    fun `refuses a delay that is not positive, or above one hour with the exponential backoff`() {
        // When & Then
        shouldThrow<IllegalArgumentException> { RetryProperties(delay = Duration.ZERO) }
        shouldThrow<IllegalArgumentException> { RetryProperties(delay = Duration.ofHours(2)) }
    }
}
