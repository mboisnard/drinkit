package com.drinkit.money.forex.exchangerateapi

import com.drinkit.feign.FeignJsonConfiguration
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.math.BigDecimal

@FeignClient(
    name = "exchangerate-api",
    url = "https://v6.exchangerate-api.com/v6",
    configuration = [FeignJsonConfiguration::class]
)
internal interface ExchangeRateApiFeignClient {

    @GetMapping("/{apiKey}/latest/{baseCurrency}")
    fun fetchLatestRates(
        @PathVariable apiKey: String,
        @PathVariable baseCurrency: String
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
    val errorType: String? = null
)
