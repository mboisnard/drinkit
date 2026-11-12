package com.drinkit.ai.guardrails

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.text.Normalizer

/**
 * Text that comes from a user, a label or a web page, put in a prompt as data. The guardrails tell the model that
 * what its delimiters hold is never an instruction.
 *
 * OWASP: [LLM01:2025 Prompt Injection](https://genai.owasp.org/llmrisk/llm01-prompt-injection/)
 */
@TechStarterTool
class UntrustedText private constructor(private val text: String) {

    override fun toString(): String = "<$TAG>$text</$TAG>"

    companion object {

        const val TAG = "untrusted_input"

        private val KEPT_CONTROLS = setOf('\n', '\r', '\t').map { it.code }
        private val HIDDEN_TYPES = setOf(
            Character.CONTROL,
            Character.FORMAT,
            Character.PRIVATE_USE,
            Character.SURROGATE,
            Character.UNASSIGNED,
        ).map { it.toInt() }

        fun of(raw: String): UntrustedText {
            // By code point: an emoji is two chars, each a surrogate on its own
            val visible = buildString {
                Normalizer.normalize(raw, Normalizer.Form.NFKC).codePoints()
                    .filter { it in KEPT_CONTROLS || Character.getType(it) !in HIDDEN_TYPES }
                    .forEach(::appendCodePoint)
            }

            return UntrustedText(visible.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))
        }
    }
}
