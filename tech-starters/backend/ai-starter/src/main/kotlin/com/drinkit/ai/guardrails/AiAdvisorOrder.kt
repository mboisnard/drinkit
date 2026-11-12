package com.drinkit.ai.guardrails

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.springframework.core.Ordered

/** Where the advisors of the starter sit, so that an advisor of the application can be placed around them. */
@TechStarterTool
object AiAdvisorOrder {

    const val GUARDRAILS = Ordered.HIGHEST_PRECEDENCE + 100

    /** Inside the guardrails: a call they block costs no token. */
    const val TOKEN_BUDGET = GUARDRAILS + 50
}
