package com.drinkit.sse.infra

import com.drinkit.messaging.SpyPlatformEventPublisher
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

internal class QueuedServerEventsTest {

    private val publisher = SpyPlatformEventPublisher()
    private val serverEvents = QueuedServerEvents(publisher, JsonMapper())

    @Test
    fun `queues the event of a user with its payload as JSON`() {
        serverEvents.send("ada", "cellar-updated", mapOf("cellarId" to "42"))

        publisher.findLastSentEvent() shouldBe
            SendServerEvent(Recipient.user("ada"), "cellar-updated", """{"cellarId":"42"}""")
    }

    @Test
    fun `refuses an event name the stream cannot carry`() {
        shouldThrow<IllegalArgumentException> { serverEvents.send("ada", "cellar\nupdated", "{}") }
    }

    @Test
    fun `queues the event of a session under the hash of its id, since the id is a secret`() {
        serverEvents.sendToSession("session-of-a-visitor", "price-changed", mapOf("bottleId" to "7"))

        val queued = publisher.findLastSentEvent().shouldBeInstanceOf<SendServerEvent>().toString()
        // The SHA-256 of "session-of-a-visitor", as shasum -a 256 prints it
        queued shouldContain "session:4988fb40d859b6e9995b22af4810501d4a3e790906fa860e4e547f3b7a424a52"
        queued shouldNotContain "session-of-a-visitor"
    }
}
