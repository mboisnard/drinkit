package com.drinkit.webclient.feign

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import feign.Retryer
import feign.codec.Decoder
import org.springframework.beans.factory.ObjectFactory
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.cloud.openfeign.support.SpringDecoder
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import java.time.Duration

/**
 * Feign client configuration for XML support.
 *
 * Usage:
 * ```
 * @FeignClient(
 *     name = "my-api",
 *     url = "https://api.example.com",
 *     configuration = [FeignXmlConfiguration::class]
 * )
 * interface MyXmlApiClient {
 *     @GetMapping("/endpoint")
 *     fun fetchData(): MyXmlResponse
 * }
 * ```
 *
 * This configuration creates a Feign Decoder that uses Jackson for XML deserialization.
 *
 * The beans are scoped to the Feign client context (isolated per client).
 */
class FeignXmlConfiguration {

    @Bean
    fun feignRetryer(): Retryer {
        return Retryer.Default(
                Duration.ofMillis(100).toMillis(),
                Duration.ofSeconds(3).toMillis(),
                3
        )
    }

    @Bean
    fun feignDecoder(): Decoder {
        val xmlMapper = XmlMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build()
            .registerKotlinModule { configure(KotlinFeature.NullIsSameAsDefault, true) }

        val xmlConverter = MappingJackson2XmlHttpMessageConverter(xmlMapper)
        val messageConverters = ObjectFactory { HttpMessageConverters(xmlConverter) }

        return SpringDecoder(messageConverters)
    }
}