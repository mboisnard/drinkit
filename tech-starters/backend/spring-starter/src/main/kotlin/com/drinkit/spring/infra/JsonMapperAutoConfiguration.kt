package com.drinkit.spring.infra

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.kotlinModule

@AutoConfiguration
@ConditionalOnProperty(prefix = "starters.spring", name = ["enabled"], matchIfMissing = true)
internal class JsonMapperAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["applicationJsonMapperBuilderCustomizer"])
    fun applicationJsonMapperBuilderCustomizer() = JsonMapperBuilderCustomizer { builder ->
        builder.addModule(kotlinModule { configure(KotlinFeature.NullIsSameAsDefault, true) })
    }
}
