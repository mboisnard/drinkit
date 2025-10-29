package com.drinkit.money.forex.spi

import com.drinkit.money.Currency.EUR
import com.drinkit.money.Currency.GBP
import com.drinkit.money.Currency.USD
import com.drinkit.money.forex.core.ExchangeRate
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

abstract class ExchangeRatesTestContract {

    private val repository: ExchangeRates by lazy { fetchRepository() }

    abstract fun fetchRepository(): ExchangeRates

    @Test
    fun `save an exchange rate and find it`() {
        // Given
        val exchangeRate = ExchangeRate.from(EUR, USD, BigDecimal("1.10"))

        // When
        repository.saveOrUpdate(setOf(exchangeRate))

        // Then
        val found = repository.find(EUR, USD)
        found shouldBe exchangeRate
    }

    @Test
    fun `save multiple exchange rates`() {
        // Given
        val eurToUsd = ExchangeRate.from(EUR, USD, BigDecimal("1.10"))
        val eurToGbp = ExchangeRate.from(EUR, GBP, BigDecimal("0.85"))

        // When
        repository.saveOrUpdate(setOf(eurToUsd, eurToGbp))

        // Then
        val foundEurToUsd = repository.find(EUR, USD)
        val foundEurToGbp = repository.find(EUR, GBP)

        foundEurToUsd shouldBe eurToUsd
        foundEurToGbp shouldBe eurToGbp
    }

    @Test
    fun `update existing exchange rate when saving with same source and target`() {
        // Given
        val initialRate = ExchangeRate.from(EUR, USD, BigDecimal("1.10"))
        repository.saveOrUpdate(setOf(initialRate))

        // When
        val updatedRate = ExchangeRate.from(EUR, USD, BigDecimal("1.15"))
        repository.saveOrUpdate(setOf(updatedRate))

        // Then
        val found = repository.find(EUR, USD)
        found shouldBe updatedRate
    }

    @Test
    fun `should distinguish between different currency pairs`() {
        // Given
        val eurToUsd = ExchangeRate.from(EUR, USD, BigDecimal("1.10"))
        val usdToEur = ExchangeRate.from(USD, EUR, BigDecimal("0.91"))
        repository.saveOrUpdate(setOf(eurToUsd, usdToEur))

        // When
        val foundEurToUsd = repository.find(EUR, USD)
        val foundUsdToEur = repository.find(USD, EUR)

        // Then
        foundEurToUsd shouldBe eurToUsd
        foundUsdToEur shouldBe usdToEur
    }
}
