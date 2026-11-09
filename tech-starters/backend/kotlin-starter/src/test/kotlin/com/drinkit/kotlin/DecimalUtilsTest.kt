package com.drinkit.kotlin

import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class DecimalUtilsTest {

    @Test
    fun `a decimal on either bound is between them`() {
        // When & Then
        listOf("-90".toBigDecimal(), "90.000".toBigDecimal()).forAll { it.isBetween(-90.0, 90.0) shouldBe true }
    }

    @Test
    fun `a decimal past a bound is not between them`() {
        // When & Then
        listOf("-90.0001".toBigDecimal(), "90.0001".toBigDecimal()).forAll { it.isBetween(-90.0, 90.0) shouldBe false }
    }

    @Test
    fun `a division rounds a half to the even neighbour at the requested scale`() {
        // When & Then
        "1".toBigDecimal().safelyDivide("8".toBigDecimal(), 2) shouldBe "0.12".toBigDecimal()
        "3".toBigDecimal().safelyDivide("8".toBigDecimal(), 2) shouldBe "0.38".toBigDecimal()
    }

    @Test
    fun `a division with an endless quotient stops at the requested scale`() {
        // When & Then
        "1".toBigDecimal().safelyDivide("3".toBigDecimal(), 4) shouldBe "0.3333".toBigDecimal()
    }
}
