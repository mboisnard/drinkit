package com.drinkit.feign

import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import feign.codec.Decoder
import org.springframework.beans.factory.ObjectFactory
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.cloud.openfeign.support.SpringDecoder
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter

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
