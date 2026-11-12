package com.drinkit.ai.guardrails

import com.drinkit.documentation.tech.starter.TechStarterTool

/** The rules of a group, as a table: each one names what it detects and how serious it is. */
@TechStarterTool
class GuardrailRules(private val group: String, private vararg val rules: GuardrailRule) {

    fun check(text: String): List<Finding> =
        rules.filter { it.matches(text) }.map { Finding(group, it.name, it.threatLevel) }
}

@TechStarterTool
class GuardrailRule(val name: String, val threatLevel: ThreatLevel, val matches: (String) -> Boolean)

@TechStarterTool
fun regex(name: String, threatLevel: ThreatLevel, regex: Regex): GuardrailRule =
    GuardrailRule(name, threatLevel) { regex.containsMatchIn(it) }
