package com.drinkit.money.forex.exchangerateapi

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange
import java.math.BigDecimal

internal interface ExchangeRateApiClient {

    @GetExchange("/{apiKey}/latest/{baseCurrency}")
    fun fetchLatestRates(
        @PathVariable apiKey: String,
        @PathVariable baseCurrency: String,
    ): ExchangeRateApiResponse
}

internal data class ExchangeRateApiResponse(
    @field:JsonProperty("result")
    val result: String,

    @field:JsonProperty("base_code")
    val baseCode: String,

    @field:JsonProperty("conversion_rates")
    val conversionRates: Map<String, BigDecimal>,

    @field:JsonProperty("error-type")
    val errorType: String? = null,
)
