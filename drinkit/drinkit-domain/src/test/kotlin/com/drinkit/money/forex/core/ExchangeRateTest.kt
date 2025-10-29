package com.drinkit.money.forex.core

import com.drinkit.money.Currency
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ExchangeRateTest {

    @Test
    fun `throw exception when rate is not positive`() {
        // Given & When & Then
        shouldThrow<IllegalArgumentException> {
            ExchangeRate.from(Currency.EUR, Currency.USD, BigDecimal.ZERO)
        }

        shouldThrow<IllegalArgumentException> {
            ExchangeRate.from(Currency.EUR, Currency.USD, BigDecimal("-1.10"))
        }
    }

    @Test
    fun `throw exception when source and target currencies are the same`() {
        // Given
        val currency = Currency.EUR

        // When & Then
        val exception = shouldThrow<IllegalArgumentException> {
            ExchangeRate.from(currency, currency, BigDecimal("1.10"))
        }
        exception.message shouldBe "Source and target currencies must be different"
    }
}
