package com.drinkit.kotlin

import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DateUtilsTest {

    private val min = LocalDate.of(1900, 1, 1)
    private val max = LocalDate.of(2026, 11, 4)

    @Test
    fun `a date inside the bounds is strictly between them`() {
        // When & Then
        LocalDate.of(1982, 6, 15).isStrictlyBetween(min, max) shouldBe true
    }

    @Test
    fun `a date on either bound is not strictly between them`() {
        // When & Then
        listOf(min, max).forAll { it.isStrictlyBetween(min, max) shouldBe false }
    }
}
