package com.drinkit.sse

class SpyServerEvents : ServerEvents {

    private var sentEvents: List<SentServerEvent> = emptyList()

    override fun send(userId: String, eventName: String, payload: Any) {
        ServerEvents.requireEventName(eventName)
        sentEvents += SentServerEvent(userId = userId, sessionId = null, eventName, payload)
    }

    override fun sendToSession(sessionId: String, eventName: String, payload: Any) {
        ServerEvents.requireEventName(eventName)
        sentEvents += SentServerEvent(userId = null, sessionId = sessionId, eventName, payload)
    }

    fun sentTo(userId: String): List<SentServerEvent> = sentEvents.filter { it.userId == userId }

    fun sentToSession(sessionId: String): List<SentServerEvent> = sentEvents.filter { it.sessionId == sessionId }
}

data class SentServerEvent(val userId: String?, val sessionId: String?, val eventName: String, val payload: Any)
