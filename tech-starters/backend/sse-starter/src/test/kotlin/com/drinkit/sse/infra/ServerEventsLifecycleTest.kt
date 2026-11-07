package com.drinkit.sse.infra

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.server.context.WebServerGracefulShutdownLifecycle
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

private val ADA = Recipient.user("ada")
private val GRACE = Recipient.user("grace")

internal class ServerEventsLifecycleTest {

    private val streams = InMemoryStreams(maxStreamsPerRecipient = 5)
    private val lifecycle = ServerEventsLifecycle(streams, heartbeat = Duration.ofMillis(10))

    @AfterEach
    fun stop() = lifecycle.stop()

    @Test
    fun `pings the open streams at each heartbeat`() {
        val tab = RecordingSseBuilder().also { streams.open(ADA, it) }

        lifecycle.start()

        runBlocking { eventually(2.seconds) { tab.sent shouldHaveAtLeastSize 3 } }
    }

    @Test
    fun `keeps pinging after a stream fails in a way the starter did not expect`() {
        val completed = IllegalStateException("The response is already complete")
        streams.open(ADA, RecordingSseBuilder(failureAfterOpening = completed, completeFailure = completed))
        val tab = RecordingSseBuilder().also { streams.open(GRACE, it) }

        lifecycle.start()

        runBlocking { eventually(2.seconds) { tab.sent shouldHaveAtLeastSize 3 } }
    }

    @Test
    fun `completes every open stream when the application stops`() {
        val tab = RecordingSseBuilder().also { streams.open(ADA, it) }
        lifecycle.start()

        lifecycle.stop()

        tab.completed shouldBe true
    }

    @Test
    fun `stops before the web server waits for the requests in progress`() {
        lifecycle.phase shouldBeGreaterThan WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE
    }
}
