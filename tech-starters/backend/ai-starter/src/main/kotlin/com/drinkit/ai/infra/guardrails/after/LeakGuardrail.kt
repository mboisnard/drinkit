package com.drinkit.ai.infra.guardrails.after

import com.drinkit.ai.guardrails.AfterCallGuardrail
import com.drinkit.ai.guardrails.Finding
import com.drinkit.ai.guardrails.ThreatLevel.CRITICAL
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import kotlin.math.sqrt

/**
 * Compares the words of the answer with those of the system prompt. Only the words the system prompt holds count, so
 * an answer that wraps the system prompt in text of its own is caught as well.
 *
 * OWASP: [LLM07:2025 System Prompt Leakage](https://genai.owasp.org/llmrisk/llm072025-system-prompt-leakage/)
 */
internal class LeakGuardrail : AfterCallGuardrail {

    override val name = "leak"

    override fun check(request: ChatClientRequest, response: ChatClientResponse): List<Finding> {
        val system = request.prompt().systemMessages.joinToString("\n") { it.text.orEmpty() }
        if (system.length < MIN_SYSTEM_PROMPT) return emptyList()

        val leaks = similarity(system, answerOf(response)) >= THRESHOLD
        return if (leaks) listOf(Finding(name, "system-prompt", CRITICAL)) else emptyList()
    }

    private fun similarity(system: String, answer: String): Double {
        val expected = wordsOf(system)
        val repeated = wordsOf(answer).filterKeys { it in expected }

        val dot = expected.entries.sumOf { (word, count) -> count * (repeated[word] ?: 0) }.toDouble()
        val norms = norm(expected) * norm(repeated)
        return if (norms == 0.0) 0.0 else dot / norms
    }

    private fun wordsOf(text: String): Map<String, Int> =
        WORD.findAll(text.lowercase()).map { it.value }.groupingBy { it }.eachCount()

    private fun norm(words: Map<String, Int>): Double = sqrt(words.values.sumOf { it * it }.toDouble())

    private companion object {
        const val THRESHOLD = 0.85
        const val MIN_SYSTEM_PROMPT = 20
        val WORD = Regex("""\p{L}{3,}""")
    }
}
