package com.drinkit.ai.infra.guardrails.patterns

import com.drinkit.ai.guardrails.ThreatLevel
import com.drinkit.ai.guardrails.ThreatLevel.CRITICAL
import com.drinkit.ai.guardrails.ThreatLevel.HIGH
import com.drinkit.kotlin.PHONE_NUMBER_REGEX
import com.drinkit.kotlin.VALID_EMAIL_REGEX

/** Masked in the prompt before it leaves, and looked for in the answer, in this order: a card holds a phone number. */
internal enum class PersonalData(
    val threatLevel: ThreatLevel,
    private val pattern: Regex,
    private val isValid: (String) -> Boolean = { true },
) {
    IBAN(CRITICAL, Regex("""\b[A-Z]{2}\d{2}\s?[A-Z0-9]{4}\s?(?:[A-Z0-9]{4}\s?){2,7}[A-Z0-9]{1,4}\b""")),
    CARD(CRITICAL, Regex("""\b(?:4\d{3}|5[1-5]\d{2}|3[47]\d{2}|6\d{3})[\s-]?(?:\d[\s-]?){8,14}\d\b"""), ::passesLuhn),
    NIR(CRITICAL, Regex("""\b[12]\s?\d{2}\s?(?:0[1-9]|1[0-2])\s?\d{2}\s?\d{3}\s?\d{3}\s?\d{2}\b""")),
    EMAIL(HIGH, VALID_EMAIL_REGEX),
    PHONE(HIGH, PHONE_NUMBER_REGEX),
    ;

    val rule = name.lowercase()

    fun isIn(text: String): Boolean = pattern.findAll(text).any { isValid(it.value) }

    fun masked(text: String): String = pattern.replace(text) { if (isValid(it.value)) "[$rule]" else it.value }

    companion object {
        fun mask(text: String): String = entries.fold(text) { masked, data -> data.masked(masked) }
    }
}

private const val DECIMAL = 10

// The checksum of every card number: a run of digits that fails it is a lot or an order number
private fun passesLuhn(number: String): Boolean {
    val digits = number.filter { it.isDigit() }.map { it.digitToInt() }.reversed()
    val sum = digits.mapIndexed { index, digit -> if (index % 2 == 0) digit else digitSum(digit * 2) }.sum()

    return sum % DECIMAL == 0
}

private fun digitSum(number: Int): Int = number / DECIMAL + number % DECIMAL
