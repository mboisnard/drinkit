package com.drinkit.ai.infra.guardrails

import io.micrometer.common.KeyValue
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationFilter
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext

/**
 * Puts the verdict of the guardrails on the observation of their advisor: the rules broken, never the text.
 * OpenTelemetry defines no attribute for guardrails, so these live under `gen_ai`, in a namespace of the starter.
 */
internal class GuardrailsObservationFilter : ObservationFilter {

    override fun map(context: Observation.Context): Observation.Context {
        val verdict = (context as? AdvisorObservationContext)
            ?.takeIf { it.advisorName == GuardrailsAdvisor.NAME }
            ?.let { it.chatClientRequest.context()[GuardrailsAdvisor.VERDICT] as? GuardrailsVerdict }
            ?: return context

        val maxLevel = verdict.findings.maxOfOrNull { it.threatLevel }
        val rules = verdict.findings.joinToString(",") { "${it.group}/${it.rule}" }
        context.addLowCardinalityKeyValue(KeyValue.of(MAX_THREAT_LEVEL, "${maxLevel ?: "NONE"}"))
        context.addLowCardinalityKeyValue(KeyValue.of(BLOCKED, "${verdict.blocked}"))
        context.addHighCardinalityKeyValue(KeyValue.of(RULES, rules))

        return context
    }

    private companion object {
        const val PREFIX = "gen_ai.ai_starter.guardrails"
        const val MAX_THREAT_LEVEL = "$PREFIX.max_threat_level"
        const val BLOCKED = "$PREFIX.blocked"
        const val RULES = "$PREFIX.rules"
    }
}
