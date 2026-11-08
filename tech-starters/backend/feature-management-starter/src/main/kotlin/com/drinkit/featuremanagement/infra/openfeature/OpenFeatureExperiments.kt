package com.drinkit.featuremanagement.infra.openfeature

import com.drinkit.featuremanagement.Assignment
import com.drinkit.featuremanagement.Experiments
import com.drinkit.featuremanagement.FeatureFlags
import com.drinkit.featuremanagement.FlagSubject
import dev.openfeature.sdk.Client
import dev.openfeature.sdk.Reason

private val NOT_DRAWN = setOf(Reason.DEFAULT.name, Reason.DISABLED.name, Reason.ERROR.name)

internal class OpenFeatureExperiments(
    private val gates: FeatureFlags,
    private val variants: Client,
    private val currentSubject: () -> FlagSubject,
) : Experiments {

    override fun assign(experiment: String) = assign(experiment, currentSubject())

    override fun assign(experiment: String, subject: FlagSubject): Assignment {
        val variant = drawnVariant(experiment, subject)
            ?: return Assignment.outOf(experiment, subject)

        return Assignment(experiment, subject, variant, inExperiment = true)
    }

    override fun trackExposure(assignment: Assignment) = Unit

    private fun drawnVariant(experiment: String, subject: FlagSubject): String? {
        if (gates.isDisabled(experiment, subject)) return null

        val details = variants.getStringDetails(experiment, Assignment.CONTROL, subject.toContext())
        return details.value.takeIf {
            details.errorCode == null && details.reason !in NOT_DRAWN
        }
    }
}
