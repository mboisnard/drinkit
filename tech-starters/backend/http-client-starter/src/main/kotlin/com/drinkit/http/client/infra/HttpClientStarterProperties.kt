package com.drinkit.http.client.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.retry.RetryPolicySettings

@ConfigurationProperties(prefix = "starters.http-client")
internal data class HttpClientStarterProperties(val enabled: Boolean = true, val retry: Retry = Retry()) {

    data class Retry(
        val policy: RetryPolicySettings = RetryPolicySettings(),
        /** The groups whose API honours the `Idempotency-Key` header. */
        val idempotentGroups: Set<String> = emptySet(),
    )
}
