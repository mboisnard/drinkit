package com.drinkit.sse.infra

import com.drinkit.messaging.PlatformEventPublisher
import com.drinkit.sse.ServerEvents
import tools.jackson.databind.json.JsonMapper

internal class QueuedServerEvents(private val publisher: PlatformEventPublisher, private val jsonMapper: JsonMapper) :
    ServerEvents {

    override fun send(userId: String, eventName: String, payload: Any) {
        publish(Recipient.user(userId), eventName, payload)
    }

    override fun sendToSession(sessionId: String, eventName: String, payload: Any) {
        publish(Recipient.session(sessionId), eventName, payload)
    }

    private fun publish(recipient: Recipient, eventName: String, payload: Any) {
        ServerEvents.requireEventName(eventName)

        val json = jsonMapper.writeValueAsString(payload)

        publisher.publish(SendServerEvent(recipient, eventName, json))
    }
}
