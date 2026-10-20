package com.drinkit.money.forex.spi

import com.drinkit.money.core.Currency
import com.drinkit.money.core.ExchangeRate

interface ExchangeRateProvider {

    fun fetchRates(baseCurrencies: Set<Currency>): List<ExchangeRate>

    fun isAvailable(): Boolean

    fun priority(): Int
}
