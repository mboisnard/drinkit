package com.drinkit.featuremanagement

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal abstract class ExperimentsTestContract {

    protected val currentSubject = FlagSubject("current-user")
    private val ada = FlagSubject("ada")
    private val experiments: Experiments by lazy { fetchExperiments() }

    /** Evaluates for [currentSubject] when no subject is given. */
    abstract fun fetchExperiments(): Experiments

    abstract fun givenDrawnVariant(experiment: String, variant: String)

    @Test
    fun `puts the subject in the variant drawn for the experiment`() {
        // Given
        givenDrawnVariant("checkout", "B")

        // When
        val assignment = experiments.assign("checkout", ada)

        // Then
        assignment shouldBe Assignment("checkout", ada, "B", inExperiment = true)
    }

    @Test
    fun `keeps the subject out of the experiment, with control, while no variant is drawn`() {
        // When
        val assignment = experiments.assign("checkout", ada)

        // Then
        assignment shouldBe Assignment.outOf("checkout", ada)
    }

    @Test
    fun `assigns the current subject when none is given`() {
        // Given
        givenDrawnVariant("checkout", "B")

        // When
        val assignment = experiments.assign("checkout")

        // Then
        assignment.subject shouldBe currentSubject
    }
}
