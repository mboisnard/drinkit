package com.drinkit.ai.infra.guardrails

/** The cases of `guardrails/prompt-injections.csv`, by kind: `attack` or `benign`. */
internal object EvaluationSet {

    fun read(): Map<String, List<String>> {
        val lines = javaClass.getResource("/guardrails/prompt-injections.csv")!!.readText().lines()

        return lines.filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.substringBefore(',') to it.substringAfter(',').removeSurrounding("\"") }
            .groupBy({ it.first }, { it.second })
    }
}
