package com.drinkit.money.forex

import com.drinkit.money.forex.ecb.EuropeanCentralBankClient
import com.drinkit.money.forex.exchangerateapi.ExchangeRateApiClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import org.springframework.web.service.registry.ImportHttpServices
import tools.jackson.databind.DeserializationFeature
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.kotlinModule

private const val ECB_GROUP = "ecb"

/**
 * Boot registers the proxies for these interfaces and reads their base URL and timeouts from
 * `spring.http.serviceclient.*`, so no proxy factory is built by hand.
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "exchangerate-api", types = [ExchangeRateApiClient::class])
@ImportHttpServices(group = ECB_GROUP, types = [EuropeanCentralBankClient::class])
internal class ForexClientsConfiguration {

    /**
     * The ECB only serves XML: unknown properties are ignored and absent values fall back to Kotlin
     * defaults. Scoped to that group so the other clients keep the application converters.
     */
    @Bean
    fun ecbXmlHttpServiceGroupConfigurer() = RestClientHttpServiceGroupConfigurer { groups ->
        groups.filterByName(ECB_GROUP).forEachClient { _, clientBuilder ->
            val xmlMapper = XmlMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .addModule(kotlinModule { configure(KotlinFeature.NullIsSameAsDefault, true) })
                .build()

            clientBuilder.configureMessageConverters { it.withXmlConverter(JacksonXmlHttpMessageConverter(xmlMapper)) }
        }
    }
}
