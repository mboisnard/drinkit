package com.drinkit.money

import com.drinkit.money.forex.core.ExchangeRate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class MoneyTest {

    @Test
    fun `validation error for negative amount`() {
        // Given
        val negativeAmount = BigDecimal("-10.00")
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
        val moneyInEur = Money.from(BigDecimal("100.00"), Currency.EUR)
        val eurToUsdRate = ExchangeRate.from(Currency.EUR, Currency.USD, BigDecimal("1.10"))

        // When
        val moneyInUsd = moneyInEur.convertWith(eurToUsdRate)

        // Then
        moneyInUsd.amount shouldBe BigDecimal("110.00")
        moneyInUsd.currency shouldBe Currency.USD
    }

    @Test
    fun `use banker's rounding when converting with exchange rate`() {
        // Given
        val moneyInEur = Money.from(BigDecimal("100.00"), Currency.EUR)
        val eurToUsdRate = ExchangeRate.from(Currency.EUR, Currency.USD, BigDecimal("1.12345"))

        // When
        val moneyInUsd = moneyInEur.convertWith(eurToUsdRate)

        // Then
        // 100.00 * 1.12345 = 112.345, which rounds to 112.34 with HALF_EVEN
        moneyInUsd.amount shouldBe BigDecimal("112.34")
    }

    @Test
    fun `throw exception when converting with mismatched exchange rate source`() {
        // Given
        val moneyInUsd = Money.from(BigDecimal("100.00"), Currency.USD)
        val eurToGbpRate = ExchangeRate.from(Currency.EUR, Currency.GBP, BigDecimal("1.10"))

        // When & Then
        shouldThrow<IllegalArgumentException> {
            moneyInUsd.convertWith(eurToGbpRate)
        }
    }
}
