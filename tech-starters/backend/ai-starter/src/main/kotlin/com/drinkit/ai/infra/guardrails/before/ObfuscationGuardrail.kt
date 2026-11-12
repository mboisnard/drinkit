package com.drinkit.ai.infra.guardrails.before

import com.drinkit.ai.guardrails.BeforeCallGuardrail
import com.drinkit.ai.guardrails.GuardrailRule
import com.drinkit.ai.guardrails.GuardrailRules
import com.drinkit.ai.guardrails.ThreatLevel.CRITICAL
import com.drinkit.ai.guardrails.ThreatLevel.HIGH
import com.drinkit.ai.guardrails.ThreatLevel.MEDIUM
import com.drinkit.ai.guardrails.UntrustedText
import com.drinkit.ai.guardrails.regex
import com.drinkit.ai.infra.guardrails.patterns.MARKDOWN_IMAGE
import org.springframework.ai.chat.client.ChatClientRequest
import java.util.Base64

/** OWASP: [LLM01:2025 Prompt Injection](https://genai.owasp.org/llmrisk/llm01-prompt-injection/) */
internal class ObfuscationGuardrail : BeforeCallGuardrail {

    override val name = "obfuscation"

    private val rules = GuardrailRules(
        name,
        GuardrailRule("base64", HIGH) { text -> BASE64.findAll(text).any { hidesText(it.value) } },
        regex("hex", HIGH, Regex("""(\\x[0-9a-fA-F]{2}){4,}""")),
        regex("unicode-escape", HIGH, Regex("""(\\u[0-9a-fA-F]{4}){4,}""")),
        regex("html-entities", MEDIUM, Regex("""(&(amp;)?#x?[0-9a-fA-F]+;){4,}""")),
        regex("invisible-characters", HIGH, Regex("""[\u200B-\u200F\u2028-\u202F\u2060\uFEFF\u00AD]{2,}""")),
        GuardrailRule("tag-escape", CRITICAL) { text -> FAKE_TAG.containsMatchIn(text.replace(OWN_TAGS, "")) },
        regex("rot13", HIGH, Regex("""(?i)\brot-?13\b|caesar\s*cipher""")),
        regex("markdown-image", HIGH, MARKDOWN_IMAGE),
    )

    override fun check(request: ChatClientRequest) = rules.check(request.prompt().userMessage.text.orEmpty())

    // A long word is valid Base64 by chance: only a candidate that decodes to readable text hides an instruction
    private fun hidesText(candidate: String): Boolean {
        val decoded = runCatching { Base64.getDecoder().decode(candidate) }.getOrNull() ?: return false
        return decoded.toString(Charsets.UTF_8).none { it == '\uFFFD' || (it.isISOControl() && it !in "\t\n\r") }
    }

    private companion object {
        val BASE64 = Regex("""(?<![\w+/=])[A-Za-z0-9+/]{20,}={0,2}(?![\w+/=])""")
        val OWN_TAGS = Regex("""</?${UntrustedText.TAG}>""")
        val FAKE_TAG = Regex(
            """(?i)(&(amp;)?lt;|<)/?\s*""" +
                """(system|${UntrustedText.TAG}|user|assistant|instructions?|prompt|tool|function)""" +
                """\s*/?(&(amp;)?gt;|>)""",
        )
    }
}
