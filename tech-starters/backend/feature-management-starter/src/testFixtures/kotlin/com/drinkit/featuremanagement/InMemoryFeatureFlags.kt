package com.drinkit.featuremanagement

class InMemoryFeatureFlags(private val currentSubject: FlagSubject = FlagSubject("current-user")) : FeatureFlags {

    private val flags = mutableMapOf<String, Boolean>()
    private val evaluations = mutableListOf<Pair<String, FlagSubject>>()

    fun configure(flag: String, enabled: Boolean) {
        flags[flag] = enabled
    }

    fun subjectsOf(flag: String): List<FlagSubject> = evaluations.filter { it.first == flag }.map { it.second }

    fun reset() {
        flags.clear()
        evaluations.clear()
    }

    override fun isEnabled(flag: String, default: Boolean) = isEnabled(flag, currentSubject, default)

    override fun isEnabled(flag: String, subject: FlagSubject, default: Boolean): Boolean {
        evaluations += flag to subject
        return flags.getOrDefault(flag, default)
    }
}
