package com.drinkit.money.forex.ecb

import org.springframework.web.service.annotation.GetExchange
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty
import java.math.BigDecimal

internal interface EuropeanCentralBankClient {

    @GetExchange("/stats/eurofxref/eurofxref-daily.xml")
    fun fetchDailyExchangeRates(): EcbDailyExchangeRatesResponse
}

internal data class EcbDailyExchangeRatesResponse(
    @field:JacksonXmlProperty(localName = "Cube")
    val outerCube: EcbOuterCube,
) {
    val rates: List<EcbRate> = outerCube.dateCube.rates
}

internal data class EcbOuterCube(
    @field:JacksonXmlProperty(localName = "Cube")
    val dateCube: EcbDateCube,
)

internal data class EcbDateCube(
    @field:JacksonXmlProperty(isAttribute = true, localName = "time")
    val time: String,

    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "Cube")
    val rates: List<EcbRate>,
)

internal data class EcbRate(
    @field:JacksonXmlProperty(isAttribute = true, localName = "currency")
    val currency: String,

    @field:JacksonXmlProperty(isAttribute = true, localName = "rate")
    val rate: BigDecimal,
)
