package com.drinkit.messaging.infra.springevents

import com.drinkit.messaging.PlatformEvent
import com.drinkit.messaging.PlatformEventHandler
import com.drinkit.messaging.infra.ExponentialBackoffRetry
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.PayloadApplicationEvent
import java.time.Duration

@ExtendWith(OutputCaptureExtension::class)
internal class PlatformEventHandlerListenerTest {

    private val scheduler = ManualScheduler()
    private val received = mutableListOf<PlatformEvent<*>>()

    @Test
    fun `calls the handler with a Spring event of its type, on the scheduler`() {
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

    private fun springEventOf(event: PlatformEvent<*>) = PayloadApplicationEvent(this, event)
}
