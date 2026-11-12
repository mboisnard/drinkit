package com.drinkit.ai.infra.guardrails.before

import com.drinkit.ai.guardrails.BeforeCallGuardrail
import com.drinkit.ai.guardrails.GuardrailRules
import com.drinkit.ai.guardrails.ThreatLevel.CRITICAL
import com.drinkit.ai.guardrails.regex
import org.springframework.ai.chat.client.ChatClientRequest

/** OWASP: [LLM07:2025 System Prompt Leakage](https://genai.owasp.org/llmrisk/llm072025-system-prompt-leakage/) */
internal class SecretGuardrail : BeforeCallGuardrail {

    override val name = "secret"

    private val rules = GuardrailRules(
        name,
        regex(
            "api-key",
            CRITICAL,
            Regex("""\b(sk-[\w-]{20,}|AKIA[0-9A-Z]{16}|gh[pousr]_\w{36}|AIza[\w-]{35}|xox[abprs]-[\w-]{10,})"""),
        ),
        regex("bearer-token", CRITICAL, Regex("""(?i)\bbearer\s+[\w.~+/-]{20,}""")),
        regex(
            "password",
            CRITICAL,
            Regex("""(?i)\b(password|passwd|pwd|mot de passe)\b[^.\n]{0,30}?(:|=|\bis\b|\best\b)\s*\S{6,}"""),
        ),
    )

    override fun check(request: ChatClientRequest) =
        request.prompt().systemMessages.flatMap { rules.check(it.text.orEmpty()) }
}
