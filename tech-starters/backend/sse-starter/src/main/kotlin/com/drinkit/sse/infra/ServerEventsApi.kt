package com.drinkit.sse.infra

import com.drinkit.messaging.PlatformEvent
import com.drinkit.messaging.PlatformEventHandler
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.time.Duration

internal data class SendServerEvent(val recipient: Recipient, val eventName: String, val json: String) :
    PlatformEvent<SendServerEvent>

internal class ServerEventsApi(private val streams: InMemoryStreams, private val timeout: Duration) {

    fun subscribe(request: ServerRequest): ServerResponse {
        // 204 tells EventSource to stop asking, as no event can reach a visitor without a session
        val recipient = Recipient.of(request) ?: return ServerResponse.noContent().build()

        return ServerResponse.sse({ stream -> streams.open(recipient, stream) }, timeout)
    }

    // Each instance delivers to the streams it holds, and a stream that missed an event has no use of it later
    @PlatformEventHandler("sse.delivery.queue", oneQueuePerInstance = true, maxAttempts = 1)
    fun deliver(event: SendServerEvent) {
        streams.send(event.recipient, event.eventName, event.json)
    }
}
