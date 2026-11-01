package com.drinkit.messaging

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

internal class PlatformEventHandlerTest {

    @Test
    fun `refuses a handler with no attempt`() {
        val failure = shouldThrow<IllegalArgumentException> {
            PlatformEventHandler(name = "something.queue", maxAttempts = 0).validate()
        }

        failure.message!! shouldContain "Handler something.queue: maxAttempts must be at least 1"
    }

    @Test
    fun `refuses a negative delay`() {
        val failure = shouldThrow<IllegalArgumentException> {
            PlatformEventHandler(name = "something.queue", delay = -1).validate()
        }

        failure.message!! shouldContain "Handler something.queue: delay must not be negative"
    }

    @Test
    fun `refuses a delay with a queue per instance`() {
        val failure = shouldThrow<IllegalArgumentException> {
            PlatformEventHandler(name = "something.queue", delay = 1, oneQueuePerInstance = true).validate()
        }

        failure.message!! shouldContain "Handler something.queue: a delay does not combine with oneQueuePerInstance"
    }
}
