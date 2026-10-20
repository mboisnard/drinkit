package com.drinkit.money.forex.spi

import com.drinkit.money.core.Currency
import com.drinkit.money.core.ExchangeRate

interface ExchangeRates {

    fun find(source: Currency, target: Currency): ExchangeRate?

    fun saveOrUpdate(exchangeRates: Set<ExchangeRate>): Set<ExchangeRate>
}
