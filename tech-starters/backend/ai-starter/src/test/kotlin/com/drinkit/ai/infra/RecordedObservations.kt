package com.drinkit.ai.infra

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler

/** Keeps the name and the key values of each observation stopped, to check what a trace would hold. */
internal class RecordedObservations : ObservationHandler<Observation.Context> {

    private val started = mutableSetOf<Observation.Context>()
    val stopped = mutableListOf<Stopped>()

    override fun supportsContext(context: Observation.Context) = true

    override fun onStart(context: Observation.Context) {
        started += context
    }

    override fun onStop(context: Observation.Context) {
        val keys = (context.lowCardinalityKeyValues + context.highCardinalityKeyValues).map { it.key to it.value }
        stopped += Stopped(context.name.orEmpty(), context in started, keys)
    }

    data class Stopped(val name: String, val started: Boolean, val keys: List<Pair<String, String>>)
}
