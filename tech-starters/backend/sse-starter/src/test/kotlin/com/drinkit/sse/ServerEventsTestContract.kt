package com.drinkit.sse

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal abstract class ServerEventsTestContract {

    protected val serverEvents: ServerEvents by lazy { fetchServerEvents() }

    abstract fun fetchServerEvents(): ServerEvents

    abstract fun eventNamesSentTo(userId: String): List<String>

    abstract fun eventNamesSentToSession(sessionId: String): List<String>

    @Test
    fun `sends an event to the user it names only`() {
        // When
        serverEvents.send("ada", "cellar-updated", mapOf("cellarId" to "42"))

        // Then
        eventNamesSentTo("ada") shouldBe listOf("cellar-updated")
        eventNamesSentTo("grace").shouldBeEmpty()
    }

    @Test
    fun `sends an event to the session it names only`() {
        // When
        serverEvents.sendToSession("session-1", "price-changed", mapOf("bottleId" to "7"))

        // Then
        eventNamesSentToSession("session-1") shouldBe listOf("price-changed")
        eventNamesSentToSession("session-2").shouldBeEmpty()
    }

    @Test
    fun `refuses a blank event name`() {
        // When & Then
        shouldThrow<IllegalArgumentException> { serverEvents.send("ada", " ", "{}") }
    }

    @Test
    fun `refuses an event name of several lines, which the stream cannot carry`() {
        // When & Then
        shouldThrow<IllegalArgumentException> { serverEvents.sendToSession("session-1", "price\nchanged", "{}") }
    }
}
