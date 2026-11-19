package com.drinkit.money.forex.spi

import com.drinkit.money.core.Currency.EUR
import com.drinkit.money.core.Currency.GBP
import com.drinkit.money.core.Currency.USD
import com.drinkit.money.core.ExchangeRate
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

abstract class ExchangeRatesTestContract {

    private val repository: ExchangeRates by lazy { fetchRepository() }

    abstract fun fetchRepository(): ExchangeRates

    @Test
    fun `save an exchange rate and find it`() {
        // Given
        val exchangeRate = ExchangeRate.from(EUR, USD, "1.10".toBigDecimal())

        // When
        repository.saveOrUpdate(setOf(exchangeRate))

        // Then
        val found = repository.find(EUR, USD)
        found shouldBe exchangeRate
    }

    @Test
    fun `save multiple exchange rates`() {
        // Given
        val eurToUsd = ExchangeRate.from(EUR, USD, "1.10".toBigDecimal())
        val eurToGbp = ExchangeRate.from(EUR, GBP, "0.85".toBigDecimal())

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
        val initialRate = ExchangeRate.from(EUR, USD, "1.10".toBigDecimal())
        repository.saveOrUpdate(setOf(initialRate))

        // When
        val updatedRate = ExchangeRate.from(EUR, USD, "1.15".toBigDecimal())
        repository.saveOrUpdate(setOf(updatedRate))

        // Then
        val found = repository.find(EUR, USD)
        found shouldBe updatedRate
    }

    @Test
    fun `should distinguish between different currency pairs`() {
        // Given
        val eurToUsd = ExchangeRate.from(EUR, USD, "1.10".toBigDecimal())
        val usdToEur = ExchangeRate.from(USD, EUR, "0.91".toBigDecimal())
        repository.saveOrUpdate(setOf(eurToUsd, usdToEur))

        // When
        val foundEurToUsd = repository.find(EUR, USD)
        val foundUsdToEur = repository.find(USD, EUR)

        // Then
        foundEurToUsd shouldBe eurToUsd
        foundUsdToEur shouldBe usdToEur
    }
}
