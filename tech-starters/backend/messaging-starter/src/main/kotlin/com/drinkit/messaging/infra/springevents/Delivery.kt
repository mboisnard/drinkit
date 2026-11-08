package com.drinkit.messaging.infra.springevents

import com.drinkit.idempotency.CurrentIdempotencyKey
import com.drinkit.messaging.PlatformEvent
import java.util.UUID

/**
 * An event on its way to its handlers, with the key that tells an idempotent one a replay from a new event: derived
 * from the key of the request or the event being handled, so the same when they are replayed, else its own.
 */
internal data class Delivery(val event: PlatformEvent<*>, val idempotencyKey: String) {

    companion object {
        fun of(event: PlatformEvent<*>) =
            Delivery(event, CurrentIdempotencyKey.derive(event.javaClass.name) ?: UUID.randomUUID().toString())
    }
}
