package com.drinkit.ai.guardrails

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.time.Duration

/**
 * A call that a guardrail or the token budget refused: no other model is asked. Its message never holds the text of
 * the prompt.
 */
@TechStarterTool
sealed class AiRefusedException(message: String) : RuntimeException(message)

@TechStarterTool
class GuardrailBlockedException(val findings: List<Finding>) :
    AiRefusedException(
        "A guardrail blocked the call: ${findings.joinToString { "${it.group}/${it.rule} ${it.threatLevel}" }}",
    )

@TechStarterTool
class TokenBudgetExceededException(val retryAfter: Duration) :
    AiRefusedException("The token budget of the user is spent, for $retryAfter")
