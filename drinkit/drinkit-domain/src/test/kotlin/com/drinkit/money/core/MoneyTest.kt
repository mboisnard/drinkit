package com.drinkit.money.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MoneyTest {

    @Test
    fun `validation error for negative amount`() {
        // Given
        val negativeAmount = "-10.00".toBigDecimal()
        val money = Money.from(negativeAmount, Currency.EUR)

        // When
        val errors = money.validate()

        // Then
        errors shouldHaveSize 1
        errors.first() shouldBe "Amount must be positive or zero"
    }

    @Test
    fun `convert money using exchange rate`() {
        // Given
        val moneyInEur = Money.from("100.00".toBigDecimal(), Currency.EUR)
        val eurToUsdRate = ExchangeRate.from(Currency.EUR, Currency.USD, "1.10".toBigDecimal())

        // When
        val moneyInUsd = moneyInEur.convertWith(eurToUsdRate)

        // Then
        moneyInUsd.amount shouldBe "110.00".toBigDecimal()
        moneyInUsd.currency shouldBe Currency.USD
    }

    @Test
    fun `use banker's rounding when converting with exchange rate`() {
        // Given
        val moneyInEur = Money.from("100.00".toBigDecimal(), Currency.EUR)
        val eurToUsdRate = ExchangeRate.from(Currency.EUR, Currency.USD, "1.12345".toBigDecimal())

        // When
        val moneyInUsd = moneyInEur.convertWith(eurToUsdRate)

        // Then
        // 100.00 * 1.12345 = 112.345, which rounds to 112.34 with HALF_EVEN
        moneyInUsd.amount shouldBe "112.34".toBigDecimal()
    }

    @Test
    fun `throw exception when converting with mismatched exchange rate source`() {
        // Given
        val moneyInUsd = Money.from("100.00".toBigDecimal(), Currency.USD)
        val eurToGbpRate = ExchangeRate.from(Currency.EUR, Currency.GBP, "1.10".toBigDecimal())

        // When & Then
        shouldThrow<IllegalArgumentException> {
            moneyInUsd.convertWith(eurToGbpRate)
        }
    }
}
