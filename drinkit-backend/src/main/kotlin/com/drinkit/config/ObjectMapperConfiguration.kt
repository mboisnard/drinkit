package com.drinkit.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter


@Configuration
class ObjectMapperConfiguration {

    @Bean
    @Primary
    fun configureCustomObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper()

        mapper.registerModules(
            AbstractIdJacksonModule(),
            JavaTimeModule(),
            Jdk8Module(),
            KotlinModule.Builder()
                .configure(KotlinFeature.NullIsSameAsDefault, true)
                .build(),
        )

        return mapper
    }

    // Can be removed when deleting WebConfig
    @Bean
    @Primary
    fun mappingJackson2HttpMessageConverter(objectMapper: ObjectMapper): MappingJackson2HttpMessageConverter {
        return MappingJackson2HttpMessageConverter(objectMapper)
    }
}