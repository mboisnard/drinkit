package com.drinkit.ai.infra.guardrails.before

import com.drinkit.ai.guardrails.BeforeCallGuardrail
import com.drinkit.ai.guardrails.GuardrailRules
import com.drinkit.ai.guardrails.ThreatLevel.CRITICAL
import com.drinkit.ai.guardrails.ThreatLevel.HIGH
import com.drinkit.ai.guardrails.ThreatLevel.MEDIUM
import com.drinkit.ai.guardrails.UntrustedText
import com.drinkit.ai.guardrails.regex
import org.springframework.ai.chat.client.ChatClientRequest

/** OWASP: [LLM01:2025 Prompt Injection](https://genai.owasp.org/llmrisk/llm01-prompt-injection/) */
internal class InjectionGuardrail : BeforeCallGuardrail {

    override val name = "injection"

    private val rules = GuardrailRules(
        name,
        regex(
            "instruction-override",
            HIGH,
            Regex(
                """(?i)\b(ignore[rz]?|disregard|forget|override|bypass|oublie[rz]?)\b[^.\n]{0,40}""" +
                    """\b(instructions?|prompts?|rules?|directives?|guidelines?|consignes?|règles?)\b""",
            ),
        ),
        regex(
            "fake-system",
            HIGH,
            Regex("""(?im)(^|<${UntrustedText.TAG}>)\W*(system|new instructions?|nouvelles? consignes?)\s*:"""),
        ),
        regex(
            "system-prompt-request",
            CRITICAL,
            Regex(
                """(?i)\b(reveal|show|print|repeat|output|display|what is|affiche|donne|révèle|répète)\b""" +
                    """[^.\n]{0,40}(system prompt|(initial|original|internal) instructions|prompt système|""" +
                    """instructions (internes|initiales))""",
            ),
        ),
        regex(
            "jailbreak",
            HIGH,
            Regex(
                """(?i)\b(dan mode|jailbreak|developer mode|no restrictions|""" +
                    """without any (rules|restrictions|limits)|sans aucune (limite|restriction|règle))""",
            ),
        ),
        // A sommelier is a role too: role play alone only warns
        regex("role-play", MEDIUM, Regex("""(?i)\b(pretend to be|joue le rôle)""")),
    )

    override fun check(request: ChatClientRequest) = rules.check(decoded(request.prompt().userMessage.text.orEmpty()))

    // An instruction spelled in HTML entities, escaped once more by UntrustedText, reads the same once decoded
    private fun decoded(text: String): String = ENTITY.replace(text.replace("&amp;", "&")) { entity ->
        val (hex, digits) = entity.destructured
        runCatching { Character.toString(digits.toInt(if (hex.isEmpty()) DECIMAL else HEXADECIMAL)) }
            .getOrDefault(entity.value)
    }

    private companion object {
        const val DECIMAL = 10
        const val HEXADECIMAL = 16
        val ENTITY = Regex("""&#([xX]?)([0-9a-fA-F]{1,6});""")
    }
}
