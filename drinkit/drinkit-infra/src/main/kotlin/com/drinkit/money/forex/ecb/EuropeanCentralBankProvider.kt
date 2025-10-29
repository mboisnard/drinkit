package com.drinkit.money.forex.ecb

import com.drinkit.money.Currency
import com.drinkit.money.forex.core.ExchangeRate
import com.drinkit.money.forex.spi.ExchangeRateProvider
import feign.FeignException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
internal class EuropeanCentralBankProvider(
    private val ecbClient: EuropeanCentralBankFeignClient,
) : ExchangeRateProvider {

    private val logger = KotlinLogging.logger {}

    override fun fetchRates(baseCurrencies: Set<Currency>): List<ExchangeRate> {
        if (Currency.EUR !in baseCurrencies) {
            logger.warn { "ECB only provides EUR-based rates, but EUR was not in requested currencies: $baseCurrencies" }
            return emptyList()
        }

        logger.info { "Fetching exchange rates from ECB for EUR" }

        val rates = try {
            val response = ecbClient.fetchDailyExchangeRates()

            response.rates.mapNotNull { rate ->
                val targetCurrency = Currency.fromCode(rate.currency) ?: return@mapNotNull null

                ExchangeRate.from(
                    source = Currency.EUR,
                    target = targetCurrency,
                    value = rate.rate,
                )
            }
        } catch (ex: FeignException) {
            logger.error(ex) { "Failed to fetch exchange rates from ECB, returning no rates" }
            emptyList()
        }

        logger.info { "Fetched ${rates.size} exchange rates from ECB" }

        return rates
    }

    override fun isAvailable(): Boolean = true

    override fun priority(): Int = 2 // Lower priority than ExchangeRate API (fallback)
}
