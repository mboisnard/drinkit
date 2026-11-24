package com.drinkit.messaging.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.backoff.BackOff
import org.springframework.util.backoff.ExponentialBackOff
import org.springframework.util.backoff.FixedBackOff
import java.time.Duration

@ConfigurationProperties(prefix = "starters.messaging")
internal data class MessagingProperties(val enabled: Boolean = true, val retry: RetryProperties = RetryProperties()) {

    data class RetryProperties(val delay: Duration = DEFAULT_RETRY_DELAY, val exponentialBackoff: Boolean = true) {

        init {
            require(delay.isPositive) { "The retry delay must be positive" }
            require(!exponentialBackoff || delay <= MAX_DELAY) {
                "The retry delay must not exceed the longest wait, $MAX_DELAY"
            }
        }

        fun backOff(): BackOff = if (exponentialBackoff) {
            ExponentialBackOff(delay.toMillis(), MULTIPLIER).apply { maxInterval = MAX_DELAY.toMillis() }
        } else {
            FixedBackOff(delay.toMillis())
        }
    }

    private companion object {
        val DEFAULT_RETRY_DELAY: Duration = Duration.ofSeconds(30)
        const val MULTIPLIER = 2.0
        val MAX_DELAY: Duration = Duration.ofHours(1)
    }
}
