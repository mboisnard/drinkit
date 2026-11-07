package com.drinkit.sse.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "starters.sse")
internal data class SseProperties(
    val enabled: Boolean = true,
    val timeout: Duration = DEFAULT_TIMEOUT,
    val heartbeat: Duration = DEFAULT_HEARTBEAT,
    val maxStreamsPerUser: Int = DEFAULT_MAX_STREAMS_PER_USER,
) {
    init {
        require(maxStreamsPerUser >= 1) { "starters.sse.max-streams-per-user must be at least 1" }
    }

    companion object {
        private val DEFAULT_TIMEOUT = Duration.ofMinutes(3)
        private val DEFAULT_HEARTBEAT = Duration.ofSeconds(30)
        private const val DEFAULT_MAX_STREAMS_PER_USER = 5
    }
}
