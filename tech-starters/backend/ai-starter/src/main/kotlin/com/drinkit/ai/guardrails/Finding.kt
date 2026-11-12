package com.drinkit.ai.guardrails

import com.drinkit.documentation.tech.starter.TechStarterTool

/** Names the rule a call broke, never the text that broke it: a finding goes to the logs and the traces. */
@TechStarterTool
data class Finding(val group: String, val rule: String, val threatLevel: ThreatLevel)

@TechStarterTool
enum class ThreatLevel { NONE, LOW, MEDIUM, HIGH, CRITICAL }
