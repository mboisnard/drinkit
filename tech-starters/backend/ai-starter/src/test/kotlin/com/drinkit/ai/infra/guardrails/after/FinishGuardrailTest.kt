package com.drinkit.ai.infra.guardrails.after

import com.drinkit.ai.guardrails.Finding
import com.drinkit.ai.guardrails.ThreatLevel
import com.drinkit.ai.guardrails.ThreatLevel.HIGH
import com.drinkit.ai.infra.guardrails.requestWith
import com.drinkit.ai.infra.guardrails.responseWith
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

internal class FinishGuardrailTest {

    private val guardrail = FinishGuardrail()

    @ParameterizedTest
    @CsvSource(
        "CONTENT_FILTER, content-filter, HIGH",
        "refusal, refusal, HIGH",
        "LENGTH, cut-short, LOW",
        "max_tokens, cut-short, LOW",
        "model_context_window_exceeded, cut-short, LOW",
        "SOMETHING_NEW, unknown-end, LOW",
    )
    fun `flags an answer that did not end normally`(finishReason: String, rule: String, threatLevel: ThreatLevel) {
        // When
        val findings = guardrail.check(requestWith(), responseWith("…", finishReason))

        // Then
        findings shouldBe listOf(Finding("finish", rule, threatLevel))
    }

    @Test
    fun `flags the refusal an OpenAI model gives with a normal end`() {
        // Given
        val refused = responseWith("", "STOP", properties = mapOf("refusal" to "I can't help with that."))

        // When
        val findings = guardrail.check(requestWith(), refused)

        // Then
        findings shouldBe listOf(Finding("finish", "refusal", HIGH))
    }

    @ParameterizedTest
    @ValueSource(strings = ["STOP", "TOOL_CALLS", "end_turn", "stop_sequence", "tool_use", "pause_turn", ""])
    fun `lets a normal end through`(finishReason: String) {
        // Given
        val answer = responseWith("Pommard is in Burgundy.", finishReason, properties = mapOf("refusal" to ""))

        // When
        val findings = guardrail.check(requestWith(), answer)

        // Then
        findings.shouldBeEmpty()
    }
}
