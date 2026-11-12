package com.drinkit.ai.infra.budget

import com.drinkit.ai.AiModels
import com.drinkit.ai.guardrails.TokenBudgetExceededException
import com.drinkit.ai.infra.AiAutoConfiguration
import com.drinkit.ai.infra.FakeModelServer
import com.drinkit.rate.limit.RateLimiter
import com.drinkit.rate.limit.inMemoryRateLimiter
import com.drinkit.security.CurrentUser
import com.drinkit.security.InMemoryCurrentUser
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.UserAccount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

internal class TokenBudgetAutoConfigurationTest {

    private val currentUser = InMemoryCurrentUser(
        InMemoryUserAccounts("jeanne@example.com" to UserAccount("jeanne", setOf("ROLE_USER"))),
    ).apply { signIn("jeanne") }

    private val server = FakeModelServer().apply { answers("qwen3-vl-4b", ANSWER) }

    private val runner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(AiAutoConfiguration::class.java, TokenBudgetAutoConfiguration::class.java),
        )
        .withBean(CurrentUser::class.java, { currentUser })
        .withPropertyValues(
            "starters.ai.endpoints.localai.base-url=${server.url}/v1",
            "starters.ai.endpoints.localai.api-key-required=false",
            "starters.ai.endpoints.localai.max-retries=0",
            "starters.ai.models.qwen3-vl-4b.endpoint=localai",
            "starters.ai.guardrails.token-budget.tokens-per-hour=1000",
        )

    @Test
    fun `counts the tokens of each model of the catalog against the user's budget`() {
        runner.withBean(RateLimiter::class.java, { inMemoryRateLimiter() }).run { context ->
            // Given
            val models = context.getBean<AiModels>()
            models.ask()

            // When & Then
            shouldThrow<TokenBudgetExceededException> { models.ask() }
        }
    }

    @Test
    fun `has no token budget without a rate limiter`() {
        runner.run { context ->
            // Given
            val models = context.getBean<AiModels>()
            models.ask()

            // When & Then
            models.ask() shouldBe ANSWER
        }
    }

    @AfterEach
    fun stopServer() = server.close()

    private fun AiModels.ask() = chatClient("qwen3-vl-4b").prompt().user("Which region is Pommard in?").call().content()

    private companion object {
        const val ANSWER = "Burgundy"
    }
}
