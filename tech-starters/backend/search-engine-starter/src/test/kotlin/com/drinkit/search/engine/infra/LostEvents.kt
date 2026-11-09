package com.drinkit.search.engine.infra

import com.drinkit.messaging.SpyPlatformEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// An instance that stops before its events reach any handler. It keeps them, for a test to deliver one
@Configuration(proxyBeanMethods = false)
internal class LostEvents {

    @Bean
    fun platformEventPublisher() = SpyPlatformEventPublisher()
}
