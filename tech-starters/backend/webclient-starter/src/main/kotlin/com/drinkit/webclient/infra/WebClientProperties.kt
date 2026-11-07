package com.drinkit.webclient.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "starters.webclient")
internal data class WebClientProperties(val enabled: Boolean = true, val retry: Retry = Retry()) {

    data class Retry(val maxRetries: Long = 3, val delay: Duration = DEFAULT_DELAY)

    private companion object {
        val DEFAULT_DELAY: Duration = Duration.ofMillis(100)
    }
}
