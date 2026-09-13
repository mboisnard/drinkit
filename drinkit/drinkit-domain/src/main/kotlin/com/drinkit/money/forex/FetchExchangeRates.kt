package com.drinkit.money.forex

import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.money.Currency
import com.drinkit.money.forex.spi.ExchangeRateProvider
import com.drinkit.money.forex.spi.ExchangeRates
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Usecase
class FetchExchangeRates(private val exchangeRates: ExchangeRates, private val providers: List<ExchangeRateProvider>) {
    @Transactional
    fun invoke() {
        val sortedProviders = providers
            .filter { it.isAvailable() }
            .sortedBy { it.priority() }

        for (provider in sortedProviders) {
            try {
                val rates = provider.fetchRates(Currency.entries.toSet())
                exchangeRates.saveOrUpdate(rates.toSet())
                return
            } catch (_: Exception) {
                // Continue to next provider
            }
        }
    }
}
