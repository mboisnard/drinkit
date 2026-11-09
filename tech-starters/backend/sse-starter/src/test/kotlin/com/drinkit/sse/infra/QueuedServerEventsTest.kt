package com.drinkit.sse.infra

import com.drinkit.messaging.SpyPlatformEventPublisher
import com.drinkit.sse.ServerEvents
import com.drinkit.sse.ServerEventsTestContract
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

internal class QueuedServerEventsTest : ServerEventsTestContract() {

    private val publisher = SpyPlatformEventPublisher()

    override fun fetchServerEvents(): ServerEvents = QueuedServerEvents(publisher, JsonMapper())

    override fun eventNamesSentTo(userId: String) = eventNamesSentTo(Recipient.user(userId))

    override fun eventNamesSentToSession(sessionId: String) = eventNamesSentTo(Recipient.session(sessionId))

    @Test
    fun `queues the event of a user with its payload as JSON`() {
        // When
        serverEvents.send("ada", "cellar-updated", mapOf("cellarId" to "42"))

        // Then
        publisher.findLastSentEvent() shouldBe
            SendServerEvent(Recipient.user("ada"), "cellar-updated", """{"cellarId":"42"}""")
    }

    @Test
    fun `queues the event of a session under the hash of its id, since the id is a secret`() {
        // When
        serverEvents.sendToSession("session-of-a-visitor", "price-changed", mapOf("bottleId" to "7"))

        // Then
        val event = publisher.findLastSentEvent().toString()
        // The SHA-256 of "session-of-a-visitor", as shasum -a 256 prints it
        event shouldContain "session:4988fb40d859b6e9995b22af4810501d4a3e790906fa860e4e547f3b7a424a52"
        event shouldNotContain "session-of-a-visitor"
    }

    private fun eventNamesSentTo(recipient: Recipient): List<String> {
        val event = publisher.findLastSentEvent() as SendServerEvent?

        return listOfNotNull(event?.takeIf { it.recipient == recipient }?.eventName)
    }
}
