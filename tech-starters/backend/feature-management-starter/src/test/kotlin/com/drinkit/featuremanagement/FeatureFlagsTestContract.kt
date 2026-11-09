package com.drinkit.featuremanagement

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal abstract class FeatureFlagsTestContract {

    private val ada = FlagSubject("ada")
    private val featureFlags: FeatureFlags by lazy { fetchFeatureFlags() }

    abstract fun fetchFeatureFlags(): FeatureFlags

    abstract fun givenFlag(flag: String, enabled: Boolean)

    @Test
    fun `tells that a flag switched on is enabled`() {
        // Given
        givenFlag("dark-mode", enabled = true)

        // When
        val enabled = featureFlags.isEnabled("dark-mode", ada)

        // Then
        enabled shouldBe true
    }

    @Test
    fun `tells that a flag switched off is disabled, whatever the default`() {
        // Given
        givenFlag("dark-mode", enabled = false)

        // When
        val enabled = featureFlags.isEnabled("dark-mode", ada, default = true)

        // Then
        enabled shouldBe false
    }

    @Test
    fun `falls back to the default for a flag it does not know`() {
        // When
        val enabled = featureFlags.isEnabled("unknown", ada, default = true)

        // Then
        enabled shouldBe true
    }
}
