package com.drinkit.idempotency.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "starters.idempotency")
internal data class IdempotencyProperties(
    val enabled: Boolean = true,
    val retention: Duration = DEFAULT_RETENTION,
    val lease: Duration = DEFAULT_LEASE,
) {
    private companion object {
        val DEFAULT_RETENTION: Duration = Duration.ofHours(24)
        val DEFAULT_LEASE: Duration = Duration.ofMinutes(5)
    }
}
