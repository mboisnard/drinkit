package com.drinkit.observability.infra

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.core.Ordered
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

// Before Spring Boot's, whose own problem details handler backs off once this one exists
@AutoConfiguration(before = [WebMvcAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ResponseEntityExceptionHandler::class)
@ConditionalOnBooleanProperty("starters.observability.enabled", matchIfMissing = true)
internal class ProblemDetailsAutoConfiguration {

    // No @Order: the advices of the application come first, ahead of this catch-all
    @Bean
    @ConditionalOnMissingBean(ResponseEntityExceptionHandler::class)
    fun problemExceptionHandler(): ProblemExceptionHandler = object : ProblemExceptionHandler() {}

    // Right after the filter of Spring Boot that opens the trace of the request, ahead of every other one
    @Bean
    @ConditionalOnMissingBean(name = ["unresolvedExceptionFilter"])
    fun unresolvedExceptionFilter(
        @Lazy @Qualifier(DispatcherServlet.HANDLER_EXCEPTION_RESOLVER_BEAN_NAME) resolver: HandlerExceptionResolver,
    ) = FilterRegistrationBean(UnresolvedExceptionFilter(resolver)).apply {
        order = Ordered.HIGHEST_PRECEDENCE + 2
    }
}
