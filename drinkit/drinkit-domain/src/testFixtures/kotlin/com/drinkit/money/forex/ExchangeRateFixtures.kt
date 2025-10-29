package com.drinkit.money.forex

import com.drinkit.money.Currency
import com.drinkit.money.forex.core.ExchangeRate
import com.drinkit.money.forex.spi.ExchangeRateProvider
import com.drinkit.money.forex.spi.FakeExchangeRateProvider
import com.drinkit.money.forex.spi.InMemoryExchangeRates
import java.math.BigDecimal

class ExchangeRateFixtures {

    val exchangeRates = InMemoryExchangeRates()
    val fakeExchangeRatesProvider = FakeExchangeRateProvider()
    val exchangeRateProviders = mutableListOf<ExchangeRateProvider>(fakeExchangeRatesProvider)

    val findExchangeRate = FindExchangeRate(exchangeRates)

    val fetchExchangeRates = FetchExchangeRates(exchangeRates, exchangeRateProviders)

    fun givenAnExchangeRate(
        source: Currency = Currency.EUR,
        target: Currency = Currency.USD,
        value: BigDecimal = BigDecimal("1.10"),
    ): ExchangeRate = exchangeRates.saveOrUpdate(setOf(ExchangeRate.from(source, target, value))).first()
}
