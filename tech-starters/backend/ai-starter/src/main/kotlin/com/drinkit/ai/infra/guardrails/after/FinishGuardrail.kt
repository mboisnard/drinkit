package com.drinkit.ai.infra.guardrails.after

import com.drinkit.ai.guardrails.AfterCallGuardrail
import com.drinkit.ai.guardrails.Finding
import com.drinkit.ai.guardrails.GuardrailRule
import com.drinkit.ai.guardrails.GuardrailRules
import com.drinkit.ai.guardrails.ThreatLevel.HIGH
import com.drinkit.ai.guardrails.ThreatLevel.LOW
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.metadata.FinishReasonCategory.FILTERED
import org.springframework.ai.chat.metadata.FinishReasonCategory.OTHER
import org.springframework.ai.chat.metadata.FinishReasonCategory.TRUNCATED
import org.springframework.ai.chat.metadata.FinishReasonCategory.categorize

/**
 * Reads how the provider ended the answer, through the categories Spring AI gives each provider's finish reason. A
 * refusal stays apart from a filter: Anthropic ends with `refusal`, and OpenAI puts it in the `refusal` property of a
 * normally ended answer.
 *
 * OWASP: [LLM05:2025 Improper Output Handling](https://genai.owasp.org/llmrisk/llm052025-improper-output-handling/)
 */
internal class FinishGuardrail : AfterCallGuardrail {

    override val name = "finish"

    private val rules = GuardrailRules(
        name,
        GuardrailRule("content-filter", HIGH) { it != REFUSAL && categorize(it) == FILTERED },
        GuardrailRule("refusal", HIGH) { it == REFUSAL },
        GuardrailRule("cut-short", LOW) { categorize(it) == TRUNCATED },
        GuardrailRule("unknown-end", LOW) { categorize(it) == OTHER && it != PAUSE_TURN },
    )

    override fun check(request: ChatClientRequest, response: ChatClientResponse): List<Finding> {
        val results = response.chatResponse()?.results.orEmpty()
        val ends = results.mapNotNull { it.metadata.finishReason?.lowercase() } +
            results.filter { !(it.output.metadata[REFUSAL] as? String).isNullOrBlank() }.map { REFUSAL }

        return ends.flatMap(rules::check).distinct()
    }

    private companion object {
        const val REFUSAL = "refusal"

        // Anthropic pauses a long turn of its server tools, which Spring AI does not categorize
        const val PAUSE_TURN = "pause_turn"
    }
}
