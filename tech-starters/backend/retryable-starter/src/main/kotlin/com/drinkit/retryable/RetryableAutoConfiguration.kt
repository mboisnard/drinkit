package com.drinkit.retryable

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.resilience.retry.MethodRetryEvent

@AutoConfiguration
@ConditionalOnProperty(prefix = "starters.retryable", name = ["enabled"], matchIfMissing = true)
@EnableResilientMethods
internal class RetryableAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun loggingRetryListener(): ApplicationListener<MethodRetryEvent> = LoggingRetryListener()
}
