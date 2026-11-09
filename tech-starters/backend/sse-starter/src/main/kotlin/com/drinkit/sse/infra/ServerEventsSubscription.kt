package com.drinkit.sse.infra

import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.time.Duration

internal class ServerEventsSubscription(private val streams: InMemoryStreams, private val timeout: Duration) {

    fun subscribe(request: ServerRequest): ServerResponse {
        // 204 tells EventSource to stop asking, as no event can reach a visitor without a session
        val recipient = Recipient.of(request) ?: return ServerResponse.noContent().build()

        return ServerResponse.sse({ stream -> streams.open(recipient, stream) }, timeout)
    }
}
