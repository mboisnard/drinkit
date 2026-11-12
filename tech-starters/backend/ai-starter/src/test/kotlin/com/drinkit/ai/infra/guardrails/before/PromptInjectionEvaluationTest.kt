package com.drinkit.ai.infra.guardrails.before

import com.drinkit.ai.guardrails.UntrustedText
import com.drinkit.ai.infra.guardrails.EvaluationSet
import com.drinkit.ai.infra.guardrails.GuardrailsProperties
import com.drinkit.ai.infra.guardrails.requestWith
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import org.junit.jupiter.api.Test

/** Runs the evaluation set of `guardrails/prompt-injections.csv` through the groups that check the user's text. */
internal class PromptInjectionEvaluationTest {

    private val guardrails = listOf(InjectionGuardrail(), ObfuscationGuardrail())
    private val cases = EvaluationSet.read()
    private val defaults = GuardrailsProperties()

    @Test
    fun `blocks the known prompt injections at the default thresholds`() {
        // Given
        val attacks = cases.getValue("attack")

        // When
        val missed = attacks.filter { isBlocked(it).not() }

        // Then
        attacks.size shouldBeGreaterThanOrEqual MIN_ATTACKS
        missed.shouldBeEmpty()
    }

    @Test
    fun `lets wine and cocktail questions through`() {
        // Given
        val questions = cases.getValue("benign")

        // When
        val flagged = questions.filter(::isFlagged)

        // Then
        questions.size shouldBeGreaterThanOrEqual MIN_QUESTIONS
        flagged.shouldBeEmpty()
    }

    // Typed straight in the prompt, or wrapped as the code sends what it did not write
    private fun isBlocked(text: String) = asSent(text).all { user -> findings(user).any(defaults::blocks) }

    private fun isFlagged(text: String) = asSent(text).any { findings(it).isNotEmpty() }

    private fun asSent(text: String) = listOf(text, "A customer writes: ${UntrustedText.of(text)}")

    private fun findings(user: String) = guardrails.flatMap { it.check(requestWith(user = user)) }

    private companion object {
        const val MIN_ATTACKS = 20
        const val MIN_QUESTIONS = 10
    }
}
