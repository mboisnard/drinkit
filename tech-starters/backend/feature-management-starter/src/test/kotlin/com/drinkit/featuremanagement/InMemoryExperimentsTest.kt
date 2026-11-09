package com.drinkit.featuremanagement

internal class InMemoryExperimentsTest : ExperimentsTestContract() {

    private val experiments = InMemoryExperiments(currentSubject)

    override fun fetchExperiments(): Experiments = experiments

    override fun givenDrawnVariant(experiment: String, variant: String) = experiments.assignVariant(experiment, variant)
}
