package com.drinkit.config

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JacksonModule
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.kotlinModule

@Configuration
class JsonMapperConfiguration {

    @Bean
    fun drinkitJsonMapperBuilderCustomizer(jacksonModules: List<JacksonModule>) =
        JsonMapperBuilderCustomizer { builder ->
            builder
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .addModule(AbstractIdJacksonModule())
                .addModule(kotlinModule { configure(KotlinFeature.NullIsSameAsDefault, true) })
                // Custom modules contributed by other configurations (mixins)
                .addModules(jacksonModules)
        }
}
