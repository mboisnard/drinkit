package com.drinkit.money.forex.exchangerateapi

import com.drinkit.configuration.ConfigurationKey
import com.drinkit.configuration.Configurations
import com.drinkit.configuration.get
import com.drinkit.money.Currency
import com.drinkit.money.forex.core.ExchangeRate
import com.drinkit.money.forex.spi.ExchangeRateProvider
import feign.FeignException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.math.BigDecimal

internal object ExchangeRateApiConfiguration {
    object ExchangeRateApiKey : ConfigurationKey<String> {
        override val key: String = "currency.exchangerate-api.key"
    }
}

@Service
internal class ExchangeRateApiProvider(
    private val configurations: Configurations,
    private val exchangeRateApiClient: ExchangeRateApiFeignClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ExchangeRateProvider {

    private val logger = KotlinLogging.logger {}

    override fun fetchRates(baseCurrencies: Set<Currency>): List<ExchangeRate> {
        if (baseCurrencies.isEmpty()) {
            return emptyList()
        }

        val apiKey = getApiKey() ?: run {
            logger.warn { "ExchangeRate API key not found, returning no rates" }
            return emptyList()
        }

        logger.info { "Fetching exchange rates from ExchangeRate API for base currencies: $baseCurrencies" }

        val allRates = runBlocking(dispatcher) {
            baseCurrencies.map { baseCurrency ->
                async {
                    try {
                        val apiResponse = exchangeRateApiClient.fetchLatestRates(
                            apiKey = apiKey,
                            baseCurrency = baseCurrency.code
                        )

                        if (apiResponse.result != "success") {
                            logger.error { "ExchangeRate API returned error for ${baseCurrency.code}: ${apiResponse.errorType}" }
                            return@async emptyList()
                        }

                        apiResponse.conversionRates.mapNotNull { (currencyCode, rate) ->
                            val targetCurrency = Currency.fromCode(currencyCode) ?: return@mapNotNull null
                            if (targetCurrency == baseCurrency) return@mapNotNull null

                            ExchangeRate.from(
                                source = baseCurrency,
                                target = targetCurrency,
                                value = BigDecimal(rate.toString())
                            )
                        }
                    } catch (ex: FeignException) {
                        logger.error(ex) { "Failed to fetch exchange rates from ExchangeRate API for ${baseCurrency.code}" }
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }

        logger.info { "Fetched ${allRates.size} exchange rates from ExchangeRate API across ${baseCurrencies.size} base currencies" }

        return allRates
    }

    override fun isAvailable(): Boolean = getApiKey() != null

    override fun priority(): Int = 1 // Higher priority than ECB

    private fun getApiKey(): String? = configurations.get(ExchangeRateApiConfiguration.ExchangeRateApiKey)
}