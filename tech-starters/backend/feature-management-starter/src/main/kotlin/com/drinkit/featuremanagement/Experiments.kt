package com.drinkit.featuremanagement

import com.drinkit.documentation.tech.starter.TechStarterTool

/** An experiment runs behind the flag of the same key, its gate. */
@TechStarterTool
interface Experiments {

    fun assign(experiment: String): Assignment

    fun assign(experiment: String, subject: FlagSubject): Assignment

    /** Where the variant reaches the user, at the same point for every variant, `control` included. */
    fun trackExposure(assignment: Assignment)
}

@TechStarterTool
data class Assignment(
    val experiment: String,
    val subject: FlagSubject,
    val variant: String,
    val inExperiment: Boolean,
) {

    companion object {
        const val CONTROL = "control"

        fun outOf(experiment: String, subject: FlagSubject) =
            Assignment(experiment, subject, CONTROL, inExperiment = false)
    }
}
