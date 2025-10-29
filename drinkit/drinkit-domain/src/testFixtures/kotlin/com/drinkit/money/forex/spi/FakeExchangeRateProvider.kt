package com.drinkit.money.forex.spi

import com.drinkit.money.Currency
import com.drinkit.money.forex.core.ExchangeRate

class FakeExchangeRateProvider(
    private val isAvailable: Boolean = true,
    private val priority: Int = 1,
    private val ratesToReturn: List<ExchangeRate> = emptyList(),
    private val shouldThrowException: Boolean = false,
) : ExchangeRateProvider {

    var fetchRatesWasCalled: Boolean = false
        private set

    var lastRequestedCurrencies: Set<Currency>? = null
        private set

    override fun fetchRates(baseCurrencies: Set<Currency>): List<ExchangeRate> {
        fetchRatesWasCalled = true
        lastRequestedCurrencies = baseCurrencies

        if (shouldThrowException) {
            throw RuntimeException("Provider failed")
        }

        return ratesToReturn
    }

    override fun isAvailable(): Boolean = isAvailable

    override fun priority(): Int = priority
}