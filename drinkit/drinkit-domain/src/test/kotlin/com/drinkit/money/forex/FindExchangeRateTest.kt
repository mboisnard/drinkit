package com.drinkit.money.forex

import com.drinkit.money.Currency.EUR
import com.drinkit.money.Currency.GBP
import com.drinkit.money.Currency.USD
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class FindExchangeRateTest {

    private val exchangeRateFixtures = ExchangeRateFixtures()
    private val findExchangeRate = exchangeRateFixtures.findExchangeRate

    @Test
    fun `find direct rate when it exists`() {
        // Given
        val directUsdToGbp = exchangeRateFixtures.givenAnExchangeRate(USD, GBP, BigDecimal("0.80"))
        exchangeRateFixtures.givenAnExchangeRate(EUR, USD, BigDecimal("1.10"))
        exchangeRateFixtures.givenAnExchangeRate(EUR, GBP, BigDecimal("0.85"))

        // When
        val rate = findExchangeRate.invoke(USD, GBP)

        // Then
        rate shouldBe directUsdToGbp
    }

    @Test
    fun `calculate cross rate from EUR when direct rate does not exist`() {
        // Given
        exchangeRateFixtures.givenAnExchangeRate(EUR, USD, BigDecimal("1.10"))
        exchangeRateFixtures.givenAnExchangeRate(EUR, GBP, BigDecimal("0.85"))

        // When
        val rate = findExchangeRate.invoke(USD, GBP)

        // Then
        rate.shouldNotBeNull() should {
            it.source shouldBe USD
            it.target shouldBe GBP
            // Rate should be approximately 0.85 / 1.10 = 0.77272727
            it.value shouldBe BigDecimal("0.77272727")
        }
    }

    @Test
    fun `calculate inverse rate from EUR when target is EUR`() {
        // Given
        exchangeRateFixtures.givenAnExchangeRate(EUR, USD, BigDecimal("1.10"))

        // When
        val rate = findExchangeRate.invoke(USD, EUR)

        // Then
        rate.shouldNotBeNull() should {
            it.source shouldBe USD
            it.target shouldBe EUR
            // Rate should be approximately 1 / 1.10 = 0.90909091
            it.value shouldBe BigDecimal("0.90909091")
        }
    }

    @Test
    fun `null when cross rate calculation is not possible`() {
        // Given
        exchangeRateFixtures.givenAnExchangeRate(EUR, GBP, BigDecimal("0.85"))

        // When
        val rate = findExchangeRate.invoke(USD, GBP)

        // Then
        rate shouldBe null
    }

    @Test
    fun `throw exception when source and target currencies are the same`() {
        // Given
        val currency = EUR

        // When & Then
        shouldThrow<IllegalArgumentException> {
            findExchangeRate.invoke(currency, currency)
        }
    }
}
