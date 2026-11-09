package com.drinkit.featuremanagement.infra.openfeature

import com.drinkit.featuremanagement.Assignment
import com.drinkit.featuremanagement.Experiments
import com.drinkit.featuremanagement.ExperimentsTestContract
import com.drinkit.featuremanagement.FlagSubject
import com.drinkit.featuremanagement.InMemoryFeatureFlags
import dev.openfeature.sdk.FeatureProvider
import dev.openfeature.sdk.NoOpProvider
import dev.openfeature.sdk.OpenFeatureAPI
import dev.openfeature.sdk.providers.memory.Flag
import dev.openfeature.sdk.providers.memory.InMemoryProvider
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class OpenFeatureExperimentsTest : ExperimentsTestContract() {

    private val ada = FlagSubject("ada")
    private val gates = InMemoryFeatureFlags()
    private val variants = InMemoryProvider(emptyMap())

    override fun fetchExperiments(): Experiments = experiments(variants)

    override fun givenDrawnVariant(experiment: String, variant: String) {
        gates.configure(experiment, enabled = true)
        variants.updateFlag(experiment, drawing(variant))
    }

    @Test
    fun `keeps the user out of the experiment, with control, while its gate is off`() {
        // Given
        givenDrawnVariant("checkout", "B")
        gates.configure("checkout", enabled = false)

        // When
        val assignment = experiments(variants).assign("checkout", ada)

        // Then
        assignment shouldBe Assignment.outOf("checkout", ada)
    }

    @Test
    fun `puts the user in the experiment when the provider draws control, so that control is exposed too`() {
        // Given
        givenDrawnVariant("checkout", "control")

        // When
        val assignment = experiments(variants).assign("checkout", ada)

        // Then
        assignment shouldBe Assignment("checkout", ada, "control", inExperiment = true)
    }

    @Test
    fun `keeps the user out of the experiment when no provider draws a variant`() {
        // Given
        gates.configure("checkout", enabled = true)

        // When
        val assignment = experiments(NoOpProvider()).assign("checkout", ada)

        // Then
        assignment shouldBe Assignment.outOf("checkout", ada)
    }

    private fun drawing(variant: String) =
        Flag.builder<String>().variant(variant, variant).defaultVariant(variant).build()

    private fun experiments(provider: FeatureProvider): OpenFeatureExperiments {
        val api = OpenFeatureAPI.createIsolated().apply { setProviderAndWait(provider) }
        return OpenFeatureExperiments(gates, api.client) { currentSubject }
    }
}
