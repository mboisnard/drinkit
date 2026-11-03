package com.drinkit.sse

import com.drinkit.messaging.events.SendSseEvent
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

internal class SseApiTest {

    private val emitters = InMemoryEmitters()

    @Test
    fun `sends an event without the session id`() {
        val emitter = RecordingEmitter()
        emitters.register("session-1", "cellar-updated", emitter)

        // When
        SseApi(emitters).sendMessageToEventStream(SendSseEvent("cellar-updated", "session-1", "payload"))

        // Then
        emitter.sent shouldHaveSize 1
        emitter.sent.single() shouldContain "event:cellar-updated"
        emitter.sent.single() shouldNotContain "session-1"
    }

    private class RecordingEmitter : SseEmitter() {
        val sent = mutableListOf<String>()

        override fun send(builder: SseEventBuilder) {
            sent += builder.build().joinToString("") { it.data.toString() }
        }
    }
}
