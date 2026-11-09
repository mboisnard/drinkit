package com.drinkit.messaging.infra.springevents

import com.drinkit.idempotency.CurrentIdempotencyKey
import com.drinkit.idempotency.inMemoryIdempotency
import com.drinkit.messaging.PlatformEvent
import com.drinkit.messaging.PlatformEventHandler
import com.drinkit.messaging.infra.ExponentialBackoffRetry
import com.drinkit.test.ControlledClock
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.logging.LogLevel
import org.springframework.boot.logging.LoggingSystem
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.PayloadApplicationEvent
import java.time.Duration
import java.time.Instant

@ExtendWith(OutputCaptureExtension::class)
internal class PlatformEventHandlerListenerTest {

    private val scheduler = ManualScheduler()
    private val received = mutableListOf<PlatformEvent<*>>()

    @Test
    fun `a handler gets the events of its type on the scheduler, never on the publishing thread`() {
        val listener = listenerOf { received += it }

        // When
        listener.onApplicationEvent(springEventOf(SomethingHappened(1)))

        // Then
        received.shouldBeEmpty()
        scheduler.runAll()
        received shouldContainExactly listOf(SomethingHappened(1))
    }

    @Test
    fun `ignores a Spring event of another type`() {
        val listener = listenerOf { received += it }

        // When
        listener.onApplicationEvent(springEventOf(SomethingElseHappened(1)))

        // Then
        scheduler.pendingCount() shouldBe 0
    }

    @Test
    fun `runs a failed handler again after the waits of the retry, up to its maxAttempts`() {
        var attempts = 0
        val listener = listenerOf(maxAttempts = 3) {
            attempts++
            error("Attempt $attempts failed")
        }

        // When
        listener.onApplicationEvent(springEventOf(SomethingHappened(1)))
        scheduler.runAll()

        // Then
        attempts shouldBe 3
        scheduler.delays shouldContainExactly listOf(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(2))
    }

    @Test
    fun `stops once the handler succeeds`() {
        var attempts = 0
        val listener = listenerOf(maxAttempts = 5) {
            attempts++
            check(attempts > 1) { "First attempt failed" }
        }

        // When
        listener.onApplicationEvent(springEventOf(SomethingHappened(1)))
        scheduler.runAll()

        // Then
        attempts shouldBe 2
    }

    @Test
    fun `waits the handler's delay before the first attempt only`() {
        val listener = listenerOf(maxAttempts = 2, delay = Duration.ofSeconds(30)) { error("Failed") }

        // When
        listener.onApplicationEvent(springEventOf(SomethingHappened(1)))
        scheduler.runAll()

        // Then
        scheduler.delays shouldContainExactly listOf(Duration.ofSeconds(30), Duration.ofSeconds(1))
    }

    @Test
    fun `logs the class of the failure of a handler that gives up`(output: CapturedOutput) {
        val listener = listenerOf(maxAttempts = 2) { error("Cannot handle $it") }

        // When
        listener.onApplicationEvent(springEventOf(SomethingHappened(1)))
        scheduler.runAll()

        // Then
        output.out shouldContain "Handler something.handler.queue gave up after 2 attempts, the last one failed with " +
            "IllegalStateException"
    }

    @Test
    fun `logs the stack trace of each failed attempt at debug`(output: CapturedOutput) {
        val listener = listenerOf(maxAttempts = 1) { error("Cannot handle $it") }
        val logging = LoggingSystem.get(javaClass.classLoader)
        logging.setLogLevel(PlatformEventHandlerListener::class.java.name, LogLevel.DEBUG)

        // When
        try {
            listener.onApplicationEvent(springEventOf(SomethingHappened(1)))
            scheduler.runAll()
        } finally {
            logging.setLogLevel(PlatformEventHandlerListener::class.java.name, null)
        }

        // Then
        output.out shouldContain "Attempt 1 of handler something.handler.queue failed\njava.lang.IllegalStateException"
        output.out shouldContain "\tat com.drinkit.messaging"
    }

    @Nested
    inner class `Replaying an asynchronous event` {

        private val clock = ControlledClock().fix(Instant.parse("2026-11-08T10:00:00Z"))
        private val idempotency = inMemoryIdempotency(clock)
        private val delivery = Delivery(SomethingHappened(1), idempotencyKey = "delivery-key")
        private val handled = mutableListOf<PlatformEvent<*>>()

        @Test
        fun `an idempotent handler runs once per key, however many deliveries`() {
            // Given
            val listener = idempotentListener { handled += it }

            // When
            listener.onApplicationEvent(springEventOf(delivery))
            listener.onApplicationEvent(springEventOf(delivery))
            scheduler.runAll()

            // Then
            handled shouldContainExactly listOf(SomethingHappened(1))
        }

        @Test
        fun `an idempotent handler that failed runs again on its next attempt`() {
            // Given
            var failing = true
            val listener = idempotentListener {
                check(!failing) { "Handler down" }
                handled += it
            }
            listener.onApplicationEvent(springEventOf(delivery))
            scheduler.runNext()

            // When
            failing = false
            scheduler.runAll()

            // Then
            handled shouldContainExactly listOf(SomethingHappened(1))
        }

        @Test
        fun `a delivery finding its key in flight on another instance runs once it is free, never twice`() {
            // Given
            val otherScheduler = ManualScheduler()
            val otherInstance = idempotentListener(otherScheduler) { handled += it }
            val listener = idempotentListener {
                otherInstance.onApplicationEvent(springEventOf(delivery))
                otherScheduler.runNext()
                handled += it
            }

            // When
            listener.onApplicationEvent(springEventOf(delivery))
            scheduler.runAll()

            // Then
            handled shouldContainExactly listOf(SomethingHappened(1))
            otherScheduler.pendingCount() shouldBe 1
            otherScheduler.runAll()
            handled shouldContainExactly listOf(SomethingHappened(1))
        }

        @Test
        fun `an idempotent handler keyed by its idempotencyKey expression runs once per value read from the event`() {
            // Given
            val listener = idempotentListener(idempotencyKey = "#event.id") { handled += it }

            // When
            listener.onApplicationEvent(springEventOf(Delivery(SomethingHappened(1), idempotencyKey = "first")))
            listener.onApplicationEvent(springEventOf(Delivery(SomethingHappened(1), idempotencyKey = "second")))
            scheduler.runAll()

            // Then
            handled shouldContainExactly listOf(SomethingHappened(1))
        }

        @Test
        fun `an event published by a handler carries a key derived from the handler's key`() {
            // Given
            val keys = mutableListOf<String?>()
            val listener = idempotentListener { keys += CurrentIdempotencyKey.derive("NextEvent") }

            // When
            listener.onApplicationEvent(springEventOf(delivery))
            listener.onApplicationEvent(springEventOf(delivery.copy(idempotencyKey = "other-key")))
            scheduler.runAll()

            // Then
            keys.shouldNotContainDuplicates()
            keys.forEach { it shouldNotBe null }
        }

        private fun idempotentListener(
            on: ManualScheduler = scheduler,
            idempotencyKey: String = "",
            handle: (PlatformEvent<*>) -> Unit,
        ) = PlatformEventHandlerListener(
            handler = PlatformEventHandler(
                name = "idempotent.handler.queue",
                idempotent = true,
                idempotencyKey = idempotencyKey,
            ),
            eventType = SomethingHappened::class.java,
            callHandler = handle,
            firstAttempt = ExponentialBackoffRetry(initialDelay = Duration.ofSeconds(1)),
            idempotency = idempotency,
            schedule = on::schedule,
        )
    }

    private fun listenerOf(maxAttempts: Int = 3, delay: Duration = Duration.ZERO, handle: (PlatformEvent<*>) -> Unit) =
        PlatformEventHandlerListener(
            handler = PlatformEventHandler(
                name = "something.handler.queue",
                maxAttempts = maxAttempts,
                delay = delay.toMillis(),
            ),
            eventType = SomethingHappened::class.java,
            callHandler = handle,
            firstAttempt = ExponentialBackoffRetry(initialDelay = Duration.ofSeconds(1)),
            schedule = scheduler::schedule,
        )

    private fun springEventOf(event: PlatformEvent<*>) = springEventOf(Delivery.of(event))

    private fun springEventOf(delivery: Delivery) = PayloadApplicationEvent(this, delivery)
}
