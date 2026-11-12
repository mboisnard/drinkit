package com.drinkit.ai.infra.budget

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("starters.ai.guardrails.token-budget")
internal data class TokenBudgetProperties(val enabled: Boolean = true, val tokensPerHour: Long = 100_000) {
    init {
        require(tokensPerHour > 0) { "A token budget needs a positive count of tokens per hour, not $tokensPerHour" }
    }
}
