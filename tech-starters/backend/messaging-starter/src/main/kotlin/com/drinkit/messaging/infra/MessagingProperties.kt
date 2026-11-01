package com.drinkit.messaging.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "starters.messaging")
internal data class MessagingProperties(val enabled: Boolean = true, val retry: Retry = Retry()) {

    data class Retry(val delay: Duration = DEFAULT_RETRY_DELAY, val exponentialBackoff: Boolean = true)

    companion object {
        private val DEFAULT_RETRY_DELAY = Duration.ofSeconds(30)
    }
}
