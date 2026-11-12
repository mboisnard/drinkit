package com.drinkit.ai.infra.guardrails.after

import com.drinkit.ai.guardrails.AfterCallGuardrail
import com.drinkit.ai.guardrails.GuardrailRules
import com.drinkit.ai.guardrails.ThreatLevel.CRITICAL
import com.drinkit.ai.guardrails.ThreatLevel.HIGH
import com.drinkit.ai.guardrails.ThreatLevel.MEDIUM
import com.drinkit.ai.guardrails.regex
import com.drinkit.ai.infra.guardrails.patterns.MARKDOWN_IMAGE
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse

/** OWASP: [LLM05:2025 Improper Output Handling](https://genai.owasp.org/llmrisk/llm052025-improper-output-handling/) */
internal class OutputGuardrail : AfterCallGuardrail {

    override val name = "output"

    private val rules = GuardrailRules(
        name,
        regex(
            "script",
            CRITICAL,
            Regex("""(?i)<script[\s>]|\bon\w+\s*=\s*["']|(?:href|src|action)\s*=\s*["']?\s*javascript:"""),
        ),
        regex("sql", CRITICAL, Regex("""(?i);\s*(?:DROP|ALTER|DELETE|INSERT|UPDATE)\s|UNION\s+(?:ALL\s+)?SELECT\s""")),
        regex("shell", HIGH, Regex("""(?i)\brm\s+-rf\b|\b(?:curl|wget)\s+\S+[^|\n]*\|\s*(?:sh|bash)\b""")),
        regex("markdown-image", HIGH, MARKDOWN_IMAGE),
        regex("secret-url", MEDIUM, Regex("""(?i)https?://\S*[?&](?:password|token|secret|api_?key)=[^\s&]+""")),
    )

    override fun check(request: ChatClientRequest, response: ChatClientResponse) = rules.check(answerOf(response))
}
