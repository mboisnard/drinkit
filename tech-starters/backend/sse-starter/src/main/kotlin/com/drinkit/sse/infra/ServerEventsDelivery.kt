package com.drinkit.sse.infra

import com.drinkit.messaging.PlatformEventHandler

internal class ServerEventsDelivery(private val streams: InMemoryStreams) {

    // Each instance delivers to the streams it holds, and a stream that missed an event has no use of it later
    @PlatformEventHandler("sse.delivery.queue", oneQueuePerInstance = true, maxAttempts = 1)
    fun deliver(event: SendServerEvent) {
        streams.send(event.recipient, event.eventName, event.json)
    }
}
