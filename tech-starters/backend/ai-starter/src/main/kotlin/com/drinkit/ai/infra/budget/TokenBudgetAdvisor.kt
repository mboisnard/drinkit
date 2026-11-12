package com.drinkit.ai.infra.budget

import com.drinkit.ai.guardrails.AiAdvisorOrder
import com.drinkit.ai.guardrails.TokenBudgetExceededException
import com.drinkit.rate.limit.RateLimit
import com.drinkit.rate.limit.RateLimitDecision
import com.drinkit.rate.limit.RateLimitRule
import com.drinkit.rate.limit.RateLimiter
import com.drinkit.security.CurrentUser
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain
import java.time.Duration

/** [LLM10:2025 Unbounded Consumption](https://genai.owasp.org/llmrisk/llm102025-unbounded-consumption/) */
internal class TokenBudgetAdvisor(
    private val rateLimiter: RateLimiter,
    private val currentUser: CurrentUser,
    private val tokensPerHour: Long,
) : CallAdvisor {

    private val rule = RateLimitRule("ai-tokens", listOf(RateLimit(tokensPerHour, Duration.ofHours(1))))

    override fun getName() = NAME

    override fun getOrder() = AiAdvisorOrder.TOKEN_BUDGET

    override fun adviseCall(request: ChatClientRequest, chain: CallAdvisorChain): ChatClientResponse {
        val user = currentUser.orNull()?.id ?: return chain.nextCall(request)

        val estimate = estimate(request)
        val decision = rateLimiter.consume(rule, user, estimate)
        if (decision is RateLimitDecision.Refused) throw TokenBudgetExceededException(decision.retryAfter)

        val response = chain.nextCall(request)

        // A refusal here is ignored: the answer is already paid for, the next call waits for the budget to come back
        val beyond = usedTokens(response) - estimate
        if (beyond > 0) rateLimiter.consume(rule, user, beyond.coerceAtMost(tokensPerHour))

        return response
    }

    private fun estimate(request: ChatClientRequest): Long {
        val prompt = request.prompt()
        val promptTokens = prompt.instructions.sumOf { it.text.orEmpty().length } / CHARACTERS_PER_TOKEN
        val answerTokens = prompt.options?.maxTokens ?: UNCAPPED_ANSWER_TOKENS

        return (promptTokens + answerTokens).toLong().coerceIn(1, tokensPerHour)
    }

    private fun usedTokens(response: ChatClientResponse): Long =
        response.chatResponse()?.metadata?.usage?.totalTokens?.toLong() ?: 0

    companion object {
        const val NAME = "TokenBudget"

        private const val CHARACTERS_PER_TOKEN = 4
        private const val UNCAPPED_ANSWER_TOKENS = 1024
    }
}
