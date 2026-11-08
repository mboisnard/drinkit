package com.drinkit.featuremanagement.infra.flipt

import com.drinkit.featuremanagement.infra.FeatureManagementProperties
import dev.openfeature.contrib.providers.flipt.FliptProvider
import dev.openfeature.contrib.providers.flipt.FliptProviderConfig
import dev.openfeature.sdk.FeatureProvider
import io.flipt.api.FliptClient

internal fun fliptProvider(flipt: FeatureManagementProperties.Flipt): FeatureProvider {
    check(flipt.url.isNotBlank()) { "starters.feature-management.flipt.url is required" }

    // The provider sends a namespace only: Flipt v2 reads the environment from this header
    val headers = if (flipt.environment.isBlank()) emptyMap() else mapOf("X-Flipt-Environment" to flipt.environment)
    val config = FliptProviderConfig.builder()
        .fliptClientBuilder(FliptClient.builder().url(flipt.url).headers(headers))
        .namespace(flipt.namespace)
        .build()

    return FliptProvider(config)
}
