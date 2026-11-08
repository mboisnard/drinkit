package com.drinkit.featuremanagement

class InMemoryExperiments(private val currentSubject: FlagSubject = FlagSubject("current-user")) : Experiments {

    private val variants = mutableMapOf<String, String>()
    private val exposures = mutableListOf<Assignment>()

    fun assignVariant(experiment: String, variant: String) {
        variants[experiment] = variant
    }

    fun exposuresOf(experiment: String): List<Assignment> = exposures.filter { it.experiment == experiment }

    fun reset() {
        variants.clear()
        exposures.clear()
    }

    override fun assign(experiment: String) = assign(experiment, currentSubject)

    override fun assign(experiment: String, subject: FlagSubject): Assignment =
        variants[experiment]?.let { Assignment(experiment, subject, it, inExperiment = true) }
            ?: Assignment.outOf(experiment, subject)

    override fun trackExposure(assignment: Assignment) {
        if (assignment.inExperiment) exposures += assignment
    }
}
