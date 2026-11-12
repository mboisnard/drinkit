package com.drinkit.ai.infra.guardrails.after

import com.drinkit.ai.guardrails.Finding
import com.drinkit.ai.guardrails.ThreatLevel.CRITICAL
import com.drinkit.ai.infra.guardrails.requestWith
import com.drinkit.ai.infra.guardrails.responseWith
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class LeakGuardrailTest {

    private val guardrail = LeakGuardrail()
    private val system = "You are the sommelier of DrinkIt. Read the label in the photo and return the producer, " +
        "the appellation and the vintage. Never invent a field you cannot read, and answer in JSON only."

    @Test
    fun `flags an answer that repeats the system prompt`() {
        // Given
        val answer = "Sure! My instructions say: $system"

        // When
        val findings = guardrail.check(requestWith(system = system), responseWith(answer))

        // Then
        findings shouldBe listOf(Finding("leak", "system-prompt", CRITICAL))
    }

    @Test
    fun `lets an answer that shares a few words with the system prompt through`() {
        // Given
        val answer = """{"producer": "Domaine de Montille", "appellation": "Pommard", "vintage": 2019}"""

        // When
        val findings = guardrail.check(requestWith(system = system), responseWith(answer))

        // Then
        findings.shouldBeEmpty()
    }
}
