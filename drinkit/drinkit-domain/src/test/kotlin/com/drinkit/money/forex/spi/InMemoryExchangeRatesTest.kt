package com.drinkit.money.forex.spi

internal class InMemoryExchangeRatesTest : ExchangeRatesTestContract() {

    override fun fetchRepository(): ExchangeRates = InMemoryExchangeRates()
}
