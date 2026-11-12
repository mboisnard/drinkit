package com.drinkit.ai.infra.guardrails.after

import com.drinkit.ai.infra.guardrails.requestWith
import com.drinkit.ai.infra.guardrails.responseWith
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class PiiGuardrailTest {

    private val guardrail = PiiGuardrail()

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        textBlock = """
        The producer answers at jeanne.martin@domaine-des-roches.fr        | email
        Call the cellar on 06 12 34 56 78 before noon                       | phone
        The winery is reachable at +33 5 56 12 34 56                        | phone
        Pay to FR76 3000 6000 0112 3456 7890 189                            | iban
        The card 4111 1111 1111 1111 paid the order                         | card
        Her social security number is 2 85 05 78 006 084 36                 | nir""",
    )
    fun `flags personal data in an answer`(answer: String, rule: String) {
        // When
        val findings = guardrail.check(requestWith(), responseWith(answer))

        // Then
        findings.map { it.rule } shouldContain rule
    }

    @Test
    fun `does not take a vintage, a price or a lot number for personal data`() {
        // Given
        val answer = "A 2019 Pommard costs 45.50 €, keeps until 2035 and is served at 16 °C, in a 75 cl bottle. " +
            "Its lot number is 4111 1111 1111 1112."

        // When
        val findings = guardrail.check(requestWith(), responseWith(answer))

        // Then
        findings.shouldBeEmpty()
    }
}
