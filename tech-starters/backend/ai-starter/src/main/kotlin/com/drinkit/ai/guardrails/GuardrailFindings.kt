package com.drinkit.ai.guardrails

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Collects what the guardrails found on a call, blocking or not, so that the code can act on a threat they only
 * observed. `CompatibleModels` puts it in each `ModelAnswer`. Another call passes one as an advisor parameter:
 * `prompt().advisors { it.param(GuardrailFindings.KEY, findings) }`.
 */
@TechStarterTool
class GuardrailFindings {

    private val found = CopyOnWriteArrayList<Finding>()

    val all: List<Finding> get() = found.toList()

    internal fun add(findings: List<Finding>) {
        found += findings
    }

    companion object {
        const val KEY = "ai.guardrails.findings"
    }
}
