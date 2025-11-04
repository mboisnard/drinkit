package com.drinkit.money.forex.ecb

import com.drinkit.webclient.feign.FeignXmlConfiguration
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import java.math.BigDecimal

@FeignClient(
    name = "ecb-forex",
    url = "https://www.ecb.europa.eu",
    configuration = [FeignXmlConfiguration::class]
)
internal interface EuropeanCentralBankFeignClient {

    @GetMapping("/stats/eurofxref/eurofxref-daily.xml")
    fun fetchDailyExchangeRates(): EcbDailyExchangeRatesResponse
}

@JacksonXmlRootElement(localName = "Envelope")
internal data class EcbDailyExchangeRatesResponse(
    @field:JacksonXmlProperty(localName = "Cube")
    val outerCube: EcbOuterCube
) {
    val rates: List<EcbRate> = outerCube.dateCube.rates
}

internal data class EcbOuterCube(
    @field:JacksonXmlProperty(localName = "Cube")
    val dateCube: EcbDateCube
)

internal data class EcbDateCube(
    @field:JacksonXmlProperty(isAttribute = true, localName = "time")
    val time: String,

    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "Cube")
    val rates: List<EcbRate>
)

internal data class EcbRate(
    @field:JacksonXmlProperty(isAttribute = true, localName = "currency")
    val currency: String,

    @field:JacksonXmlProperty(isAttribute = true, localName = "rate")
    val rate: BigDecimal
)