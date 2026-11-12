package com.drinkit.observability.infra

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

// Before Spring Boot's, whose own problem details handler backs off once this one exists
@AutoConfiguration(before = [WebMvcAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ResponseEntityExceptionHandler::class)
@ConditionalOnProperty("starters.observability.enabled", matchIfMissing = true)
internal class ProblemDetailsAutoConfiguration {

    // No @Order: the advices of the application come first, ahead of this catch-all
    @Bean
    @ConditionalOnMissingBean(ResponseEntityExceptionHandler::class)
    fun problemExceptionHandler(): ProblemExceptionHandler = object : ProblemExceptionHandler() {}
}
