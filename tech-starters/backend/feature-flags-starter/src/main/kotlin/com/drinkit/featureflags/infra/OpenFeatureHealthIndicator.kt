package com.drinkit.featureflags.infra

import dev.openfeature.sdk.Client
import dev.openfeature.sdk.ProviderState
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.stereotype.Component

@Component
@ConditionalOnClass(HealthIndicator::class)
internal class OpenFeatureHealthIndicator(
    private val client: Client,
) : HealthIndicator {

    override fun health(): Health {
        val state = client.providerState
        val healthBuilder = Health.Builder()
            .withDetail("provider", client.metadata.domain)
            .withDetail("state", state.name)

        return when (state) {
            ProviderState.READY, ProviderState.STALE -> healthBuilder.up().build()
            ProviderState.ERROR, ProviderState.FATAL -> healthBuilder.down().build()
            else -> healthBuilder.unknown().build()
        }
    }
}
