package com.drinkit.messaging.events

import com.drinkit.messaging.PlatformEvent

// Why SSE logic here? it's more simple to share event directly in the module exposing the platformEvent tools
// Alternative: create a dedicated module like sse-messaging
data class SendSseEvent(
    val eventName: String,
    val sessionId: String,
    val payload: Any,
): PlatformEvent<SendSseEvent>