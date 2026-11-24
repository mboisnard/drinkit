package com.drinkit.retryable.infra

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.resilience.retry.MethodRetryEvent

@AutoConfiguration
@ConditionalOnBooleanProperty("starters.retryable.enabled", matchIfMissing = true)
@EnableResilientMethods
internal class RetryableAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["loggingRetryListener"])
    fun loggingRetryListener(): ApplicationListener<MethodRetryEvent> = LoggingRetryListener()
}
