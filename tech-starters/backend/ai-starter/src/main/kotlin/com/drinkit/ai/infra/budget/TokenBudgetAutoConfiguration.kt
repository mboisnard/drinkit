package com.drinkit.ai.infra.budget

import com.drinkit.rate.limit.RateLimiter
import com.drinkit.security.CurrentUser
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/** Needs rate-limit-starter, which counts the tokens, and security-starter, which tells whose tokens they are. */
@AutoConfiguration(
    afterName = [
        "com.drinkit.rate.limit.infra.RateLimitAutoConfiguration",
        "com.drinkit.security.infra.login.UserAccountsAutoConfiguration",
    ],
)
@ConditionalOnClass(RateLimiter::class, CurrentUser::class)
@ConditionalOnBean(RateLimiter::class, CurrentUser::class)
@ConditionalOnBooleanProperty("starters.ai.guardrails.token-budget.enabled", matchIfMissing = true)
@EnableConfigurationProperties(TokenBudgetProperties::class)
internal class TokenBudgetAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["aiTokenBudget"])
    fun aiTokenBudget(rateLimiter: RateLimiter, currentUser: CurrentUser, properties: TokenBudgetProperties) =
        ChatClientBuilderCustomizer { builder ->
            builder.defaultAdvisors(TokenBudgetAdvisor(rateLimiter, currentUser, properties.tokensPerHour))
        }
}
