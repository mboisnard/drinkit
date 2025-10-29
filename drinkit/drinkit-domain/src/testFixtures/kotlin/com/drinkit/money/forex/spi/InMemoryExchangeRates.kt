package com.drinkit.money.forex.spi

import com.drinkit.money.Currency
import com.drinkit.money.forex.core.ExchangeRate

class InMemoryExchangeRates : ExchangeRates {

    private val store = mutableMapOf<Pair<Currency, Currency>, ExchangeRate>()

    override fun find(source: Currency, target: Currency): ExchangeRate? {
        return store[source to target]
    }

    override fun saveOrUpdate(exchangeRates: Set<ExchangeRate>): Set<ExchangeRate> {
        exchangeRates.forEach { exchangeRate ->
            store[exchangeRate.source to exchangeRate.target] = exchangeRate.copy(
                    value = exchangeRate.value
            )
        }
        return exchangeRates
    }

    fun clear() {
        store.clear()
    }
}