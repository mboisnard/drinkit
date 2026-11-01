package com.drinkit.messaging.infra

import com.drinkit.messaging.PlatformEventPublisher
import com.drinkit.messaging.infra.springevents.PlatformEventHandlerProcessor
import com.drinkit.messaging.infra.springevents.SpringEventsPublisher
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnProperty(prefix = "starters.messaging", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(MessagingProperties::class)
internal class MessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun platformEventPublisher(applicationEventPublisher: ApplicationEventPublisher): PlatformEventPublisher =
        SpringEventsPublisher(applicationEventPublisher)

    @Bean
    @ConditionalOnMissingBean
    fun platformEventRetry(properties: MessagingProperties): Retry = with(properties.retry) {
        if (exponentialBackoff) ExponentialBackoffRetry(initialDelay = delay) else FixedDelayRetry(delay)
    }

    @Bean
    @ConditionalOnMissingBean
    fun platformEventHandlerProcessor(
        context: ConfigurableApplicationContext,
        retry: Retry,
    ): PlatformEventHandlerProcessor = PlatformEventHandlerProcessor(context, retry)
}
