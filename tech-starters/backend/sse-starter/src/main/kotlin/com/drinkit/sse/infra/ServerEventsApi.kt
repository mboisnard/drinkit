package com.drinkit.sse.infra

import com.drinkit.messaging.PlatformEventHandler
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.time.Duration

internal class ServerEventsApi(private val streams: InMemoryStreams, private val timeout: Duration) {

    fun subscribe(request: ServerRequest): ServerResponse {
        val recipient = Recipient.of(request)
            ?: return ServerResponse.noContent().build()

        return ServerResponse.sse({ stream -> streams.open(recipient, stream) }, timeout)
    }

    // Each instance delivers to the streams it holds, and a stream that missed an event has no use of it later
    @PlatformEventHandler("sse.delivery.queue", oneQueuePerInstance = true, maxAttempts = 1)
    fun deliver(event: SendServerEvent) {
        streams.send(event.recipient, event.eventName, event.json)
    }
}
