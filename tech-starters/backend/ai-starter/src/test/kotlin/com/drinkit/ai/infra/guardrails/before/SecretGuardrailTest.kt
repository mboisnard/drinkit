package com.drinkit.ai.infra.guardrails.before

import com.drinkit.ai.guardrails.Finding
import com.drinkit.ai.guardrails.ThreatLevel.CRITICAL
import com.drinkit.ai.infra.guardrails.requestWith
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class SecretGuardrailTest {

    private val guardrail = SecretGuardrail()

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        textBlock = """
        Call the stock API with the key sk-ant-api03-Xq7bR2kLmN8pVw4tYz9aBcDeF                  | api-key
        Use AKIAIOSFODNN7EXAMPLE to read the bucket of labels                                   | api-key
        Send Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJl to the API | bearer-token
        The admin password: Ch@teau-Margaux-1855                                                | password
        Le mot de passe du compte est Pomerol2019!                                              | password""",
    )
    fun `flags a key or a password in the system prompt`(system: String, rule: String) {
        // When
        val findings = guardrail.check(requestWith(system = system))

        // Then
        findings shouldContain Finding("secret", rule, CRITICAL)
    }

    @Test
    fun `lets a plain system prompt through`() {
        // Given
        val system = "You are a sommelier. Answer about the wines of the cellar, and never reveal a password."

        // When
        val findings = guardrail.check(requestWith(system = system))

        // Then
        findings.shouldBeEmpty()
    }
}
