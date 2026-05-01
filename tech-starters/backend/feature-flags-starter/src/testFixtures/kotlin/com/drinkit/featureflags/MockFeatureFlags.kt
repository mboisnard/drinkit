package com.drinkit.featureflags

class MockFeatureFlags : FeatureFlags {

    private val flags = mutableMapOf<String, Boolean>()
    private val evaluations = mutableListOf<Evaluation>()

    data class Evaluation(val flag: String, val context: FeatureFlagContext?)

    fun configure(flag: String, enabled: Boolean) {
        flags[flag] = enabled
    }

    fun reset() {
        flags.clear()
        evaluations.clear()
    }

    fun enabledFlags(): Set<String> = flags.filterValues { it }.keys

    fun disabledFlags(): Set<String> = flags.filterValues { !it }.keys

    fun allFlags(): Map<String, Boolean> = flags.toMap()

    fun evaluationCount(): Int = evaluations.size

    fun wasEvaluated(flag: String): Boolean = evaluations.any { it.flag == flag }

    fun evaluationsOf(flag: String): List<Evaluation> = evaluations.filter { it.flag == flag }

    fun lastEvaluation(): Evaluation? = evaluations.lastOrNull()

    override fun isEnabled(flag: String): Boolean {
        evaluations += Evaluation(flag, context = null)
        return flags[flag] ?: false
    }

    override fun isEnabled(flag: String, context: FeatureFlagContext): Boolean {
        evaluations += Evaluation(flag, context)
        return flags[flag] ?: false
    }
}
