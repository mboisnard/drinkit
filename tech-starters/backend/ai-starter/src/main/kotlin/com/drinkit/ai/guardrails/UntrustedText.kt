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
class UntrustedText private constructor(val content: String) {

    fun delimited(): String = "<$TAG>$content</$TAG>"

    // A string template keeps the text delimited
    override fun toString(): String = delimited()

    companion object {

        const val TAG = "untrusted_input"

        private val KEPT_CONTROLS = setOf('\n', '\r', '\t').map { it.code }
        private val INVISIBLE_TYPES = setOf(
            Character.CONTROL,
            Character.FORMAT,
            Character.PRIVATE_USE,
            Character.SURROGATE,
            Character.UNASSIGNED,
        ).map { it.toInt() }

        fun of(raw: String): UntrustedText = UntrustedText(
            raw.normalizeUnicode()
                .removeInvisibleCharacters()
                .escapeTags(),
        )

        // NFKC turns a fullwidth ＜ into the < that the escaping then sees
        private fun String.normalizeUnicode(): String = Normalizer.normalize(this, Normalizer.Form.NFKC)

        // By code point: an emoji is two chars, each a surrogate on its own
        private fun String.removeInvisibleCharacters(): String = buildString {
            this@removeInvisibleCharacters.codePoints()
                .filter { it in KEPT_CONTROLS || Character.getType(it) !in INVISIBLE_TYPES }
                .forEach(::appendCodePoint)
        }

        // The ampersand first, or the entities of the next replacements would be escaped twice
        private fun String.escapeTags(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
}
