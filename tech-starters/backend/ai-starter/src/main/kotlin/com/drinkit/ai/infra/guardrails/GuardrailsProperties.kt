package com.drinkit.ai.infra.guardrails

import com.drinkit.ai.guardrails.Finding
import com.drinkit.ai.guardrails.ThreatLevel
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("starters.ai.guardrails")
internal data class GuardrailsProperties(val enabled: Boolean = true, val groups: Map<String, Group> = emptyMap()) {

    fun groupNamed(name: String): Group = groups[name] ?: Group(blockAt = BLOCKING_GROUPS[name] ?: ThreatLevel.NONE)

    fun blocks(finding: Finding): Boolean {
        val blockAt = groupNamed(finding.group).blockAt
        return blockAt != ThreatLevel.NONE && finding.threatLevel >= blockAt
    }

    /** [blockAt] `NONE` only observes: the findings of the group are logged and traced, the call goes on. */
    data class Group(
        val enabled: Boolean = true,
        val blockAt: ThreatLevel = ThreatLevel.NONE,
        val disabledRules: Set<String> = emptySet(),
    )

    private companion object {
        val BLOCKING_GROUPS = mapOf(
            "injection" to ThreatLevel.HIGH,
            "obfuscation" to ThreatLevel.HIGH,
            "secret" to ThreatLevel.HIGH,
            "leak" to ThreatLevel.HIGH,
        )
    }
}
