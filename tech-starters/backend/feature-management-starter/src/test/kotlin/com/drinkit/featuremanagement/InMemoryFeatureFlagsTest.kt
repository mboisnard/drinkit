package com.drinkit.featuremanagement

internal class InMemoryFeatureFlagsTest : FeatureFlagsTestContract() {

    private val featureFlags = InMemoryFeatureFlags()

    override fun fetchFeatureFlags(): FeatureFlags = featureFlags

    override fun givenFlag(flag: String, enabled: Boolean) = featureFlags.configure(flag, enabled)
}
