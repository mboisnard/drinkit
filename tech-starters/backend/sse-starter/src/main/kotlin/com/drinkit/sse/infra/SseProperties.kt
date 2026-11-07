package com.drinkit.sse.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "starters.sse")
internal data class SseProperties(
    val enabled: Boolean = true,
    val timeout: Duration = DEFAULT_TIMEOUT,
    val heartbeat: Duration = DEFAULT_HEARTBEAT,
    val maxStreams: Int = DEFAULT_MAX_STREAMS,
) {
    init {
        require(maxStreams >= 1) { "starters.sse.max-streams must be at least 1" }
        require(heartbeat.isPositive && heartbeat < timeout) {
            "starters.sse.heartbeat must be positive and shorter than starters.sse.timeout"
        }
    }

    companion object {
        private val DEFAULT_TIMEOUT = Duration.ofMinutes(3)
        private val DEFAULT_HEARTBEAT = Duration.ofSeconds(30)
        private const val DEFAULT_MAX_STREAMS = 5
    }
}
