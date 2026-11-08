package com.drinkit.featuremanagement.infra.openfeature

import com.drinkit.featuremanagement.Assignment
import com.drinkit.featuremanagement.FlagSubject
import com.drinkit.featuremanagement.InMemoryFeatureFlags
import dev.openfeature.sdk.FeatureProvider
import dev.openfeature.sdk.NoOpProvider
import dev.openfeature.sdk.OpenFeatureAPI
import dev.openfeature.sdk.providers.memory.Flag
import dev.openfeature.sdk.providers.memory.InMemoryProvider
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class OpenFeatureExperimentsTest {

    private val ada = FlagSubject("ada")
    private val gates = InMemoryFeatureFlags()

    @Test
    fun `keeps the user out of the experiment, with control, while its gate is off`() {
        // Given
        gates.configure("checkout", enabled = false)

        // When
        val assignment = experimentsDrawing("B").assign("checkout", ada)

        // Then
        assignment shouldBe Assignment.outOf("checkout", ada)
    }

    @Test
    fun `puts the user in the variant the provider draws once the gate is on`() {
        // Given
        gates.configure("checkout", enabled = true)

        // When
        val assignment = experimentsDrawing("B").assign("checkout", ada)

        // Then
        assignment shouldBe Assignment("checkout", ada, "B", inExperiment = true)
    }

    @Test
    fun `puts the user in the experiment when the provider draws control, so that control is exposed too`() {
        // Given
        gates.configure("checkout", enabled = true)

        // When
        val assignment = experimentsDrawing("control").assign("checkout", ada)

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

    private fun experimentsDrawing(variant: String) = experiments(
        InMemoryProvider(
            mapOf("checkout" to Flag.builder<String>().variant(variant, variant).defaultVariant(variant).build()),
        ),
    )

    private fun experiments(variants: FeatureProvider): OpenFeatureExperiments {
        val api = OpenFeatureAPI.createIsolated().apply { setProviderAndWait(variants) }
        return OpenFeatureExperiments(gates, api.client) { ada }
    }
}
