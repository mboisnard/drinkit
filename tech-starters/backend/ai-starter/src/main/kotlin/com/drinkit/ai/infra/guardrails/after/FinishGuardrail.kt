package com.drinkit.ai.infra.guardrails.after

import com.drinkit.ai.guardrails.AfterCallGuardrail
import com.drinkit.ai.guardrails.Finding
import com.drinkit.ai.guardrails.GuardrailRule
import com.drinkit.ai.guardrails.GuardrailRules
import com.drinkit.ai.guardrails.ThreatLevel.HIGH
import com.drinkit.ai.guardrails.ThreatLevel.LOW
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse

/**
 * Reads how the provider ended the answer: OpenAI in upper case, Anthropic in lower case. OpenAI also puts a refusal
 * in the `refusal` property of a normally ended answer.
 *
 * OWASP: [LLM05:2025 Improper Output Handling](https://genai.owasp.org/llmrisk/llm052025-improper-output-handling/)
 */
internal class FinishGuardrail : AfterCallGuardrail {

    override val name = "finish"

    private val rules = GuardrailRules(
        name,
        GuardrailRule("content-filter", HIGH) { it == CONTENT_FILTER },
        GuardrailRule("refusal", HIGH) { it == REFUSAL },
        GuardrailRule("cut-short", LOW) { it in CUT_SHORT },
        GuardrailRule("unknown-end", LOW) { it !in KNOWN_ENDS },
    )

    override fun check(request: ChatClientRequest, response: ChatClientResponse): List<Finding> {
        val results = response.chatResponse()?.results.orEmpty()
        val ends = results.mapNotNull { it.metadata.finishReason?.lowercase() } +
            results.filter { !(it.output.metadata[REFUSAL] as? String).isNullOrBlank() }.map { REFUSAL }

        return ends.flatMap(rules::check).distinct()
    }

    private companion object {
        val NORMAL_ENDS =
            setOf("", "stop", "end_turn", "stop_sequence", "tool_calls", "tool_use", "function_call", "pause_turn")
        val CUT_SHORT = setOf("length", "max_tokens", "model_context_window_exceeded")
        const val CONTENT_FILTER = "content_filter"
        const val REFUSAL = "refusal"
        val KNOWN_ENDS = NORMAL_ENDS + CUT_SHORT + CONTENT_FILTER + REFUSAL
    }
}
