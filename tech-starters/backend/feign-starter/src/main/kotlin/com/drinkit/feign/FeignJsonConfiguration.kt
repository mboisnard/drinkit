package com.drinkit.feign

import com.fasterxml.jackson.databind.ObjectMapper
import feign.codec.Decoder
import org.springframework.beans.factory.ObjectFactory
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.cloud.openfeign.support.SpringDecoder
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter

/**
 * Feign client configuration for JSON support.
 *
 * Usage:
 * ```
 * @FeignClient(
 *     name = "my-api",
 *     url = "https://api.example.com",
 *     configuration = [FeignJsonConfiguration::class]
 * )
 * interface MyJsonApiClient {
 *     @GetMapping("/endpoint")
 *     fun fetchData(): MyJsonResponse
 * }
 * ```
 *
 * This configuration creates a Feign Decoder that uses Jackson for JSON deserialization.
 * It uses the ObjectMapper from the parent Spring context (with Kotlin module already configured).
 *
 * The beans are scoped to the Feign client context (isolated per client).
 */
class FeignJsonConfiguration {

    @Bean
    fun feignDecoder(objectMapper: ObjectMapper): Decoder {
        val jsonConverter = MappingJackson2HttpMessageConverter(objectMapper)
        val messageConverters = ObjectFactory { HttpMessageConverters(jsonConverter) }
        return SpringDecoder(messageConverters)
    }
}
