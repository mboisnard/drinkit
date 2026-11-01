package com.drinkit.messaging.infra.springevents

import com.drinkit.messaging.PlatformEventHandler
import com.drinkit.messaging.PlatformEventPublisher
import com.drinkit.messaging.infra.MessagingAutoConfiguration
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

internal class PlatformEventHandlerProcessorTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MessagingAutoConfiguration::class.java))

    @Test
    fun `a published event reaches its handler, off the publishing thread`() {
        runner.withBean(RecordingHandler::class.java)
            .run { context ->
                // When
                context.getBean<PlatformEventPublisher>().publish(SomethingHappened(1))

                // Then
                val handler = context.getBean<RecordingHandler>()
                handler.received.get(5, TimeUnit.SECONDS) shouldBe SomethingHappened(1)
                handler.thread.get(5, TimeUnit.SECONDS) shouldNotBe Thread.currentThread()
            }
    }

    @Test
    fun `refuses a handler whose parameter is not a platform event`() {
        runner.withBean(NotAnEventHandler::class.java)
            .run { context ->
                context.startupFailure.shouldNotBeNull().stackTraceToString() shouldContain
                    "Handler not.an.event.queue: its only parameter must be a platform event"
            }
    }

    @Test
    fun `refuses two handlers with the same name`() {
        runner.withBean(RecordingHandler::class.java)
            .withBean(SameNameHandler::class.java)
            .run { context ->
                context.startupFailure.shouldNotBeNull().stackTraceToString() shouldContain
                    "Two handlers are named recording.handler.queue"
            }
    }

    @Test
    fun `refuses a handler that breaks the rules of its annotation`() {
        runner.withBean(NoAttemptHandler::class.java)
            .run { context ->
                context.startupFailure.shouldNotBeNull().stackTraceToString() shouldContain
                    "Handler no.attempt.queue: maxAttempts must be at least 1"
            }
    }

    internal class RecordingHandler {
        val received = CompletableFuture<SomethingHappened>()
        val thread = CompletableFuture<Thread>()

        @PlatformEventHandler(name = "recording.handler.queue")
        fun handle(event: SomethingHappened) {
            thread.complete(Thread.currentThread())
            received.complete(event)
        }
    }

    internal class SameNameHandler {

        @PlatformEventHandler(name = "recording.handler.queue")
        fun handle(event: SomethingElseHappened) = event.id
    }

    internal class NoAttemptHandler {

        @PlatformEventHandler(name = "no.attempt.queue", maxAttempts = 0)
        fun handle(event: SomethingHappened) = event.id
    }

    internal class NotAnEventHandler {

        @PlatformEventHandler(name = "not.an.event.queue")
        fun handle(notAnEvent: String) = notAnEvent.length
    }
}
