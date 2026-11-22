package com.drinkit.sse.infra

import com.drinkit.messaging.PlatformEvent

internal data class SendServerEvent(
    val recipient: Recipient,
    val id: String,
    val eventName: String,
    val json: String,
) : PlatformEvent<SendServerEvent>
