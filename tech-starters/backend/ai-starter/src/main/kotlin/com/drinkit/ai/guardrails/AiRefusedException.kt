package com.drinkit.ai.guardrails

import com.drinkit.documentation.tech.starter.TechStarterTool

/** A call the guardrails refused: no other model is asked. Its message never holds the text of the prompt. */
@TechStarterTool
sealed class AiRefusedException(message: String) : RuntimeException(message)

@TechStarterTool
class GuardrailBlockedException(val findings: List<Finding>) :
    AiRefusedException(
        "A guardrail blocked the call: ${findings.joinToString { "${it.group}/${it.rule} ${it.threatLevel}" }}",
    )
