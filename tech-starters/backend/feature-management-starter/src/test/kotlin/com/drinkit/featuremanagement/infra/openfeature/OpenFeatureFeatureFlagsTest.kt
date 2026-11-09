package com.drinkit.featuremanagement.infra.openfeature

import com.drinkit.featuremanagement.FeatureFlags
import com.drinkit.featuremanagement.FeatureFlagsTestContract
import com.drinkit.featuremanagement.FlagSubject
import dev.openfeature.sdk.OpenFeatureAPI
import dev.openfeature.sdk.providers.memory.Flag
import dev.openfeature.sdk.providers.memory.InMemoryProvider

internal class OpenFeatureFeatureFlagsTest : FeatureFlagsTestContract() {

    private val flags = InMemoryProvider(emptyMap())

    override fun fetchFeatureFlags(): FeatureFlags {
        val api = OpenFeatureAPI.createIsolated().apply { setProviderAndWait(flags) }
        return OpenFeatureFeatureFlags(api.client) { FlagSubject("current-user") }
    }

    override fun givenFlag(flag: String, enabled: Boolean) {
        val variant = if (enabled) "on" else "off"
        flags.updateFlag(
            flag,
            Flag.builder<Boolean>().variant("on", true).variant("off", false).defaultVariant(variant).build(),
        )
    }
}
