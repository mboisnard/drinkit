package com.drinkit.sse.infra

import com.drinkit.messaging.SpyPlatformEventPublisher
import io.kotest.matchers.shouldBe
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
    fun `queues the event of a session under the hash of its id, since the id is a secret`() {
        serverEvents.sendToSession("session-of-a-visitor", "price-changed", mapOf("bottleId" to "7"))

        val queued = publisher.findLastSentEvent().shouldBeInstanceOf<SendServerEvent>()
        queued.recipient shouldBe Recipient.session("session-of-a-visitor")
        queued.toString() shouldNotContain "session-of-a-visitor"
    }
}
