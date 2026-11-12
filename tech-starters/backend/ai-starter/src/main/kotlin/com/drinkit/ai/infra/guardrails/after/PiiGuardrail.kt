package com.drinkit.ai.infra.guardrails.after

import com.drinkit.ai.guardrails.AfterCallGuardrail
import com.drinkit.ai.guardrails.Finding
import com.drinkit.ai.infra.guardrails.patterns.PersonalData
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse

/**
 * OWASP: [LLM02:2025 Sensitive Information Disclosure](https://genai.owasp.org/llmrisk/llm022025-sensitive-information-disclosure/)
 */
internal class PiiGuardrail : AfterCallGuardrail {

    override val name = "pii"

    override fun check(request: ChatClientRequest, response: ChatClientResponse): List<Finding> {
        val answer = answerOf(response)

        return PersonalData.entries.filter { it.isIn(answer) }.map { Finding(name, it.rule, it.threatLevel) }
    }
}
