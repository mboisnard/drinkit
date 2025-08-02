package com.drinkit.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

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
            kotlinModule { configure(KotlinFeature.NullIsSameAsDefault, true) },
        )

        return mapper
    }
}
