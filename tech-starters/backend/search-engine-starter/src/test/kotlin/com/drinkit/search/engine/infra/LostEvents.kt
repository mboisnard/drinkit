package com.drinkit.search.engine.infra

import com.drinkit.messaging.PlatformEvent
import com.drinkit.messaging.PlatformEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.CopyOnWriteArrayList

// An instance that stops before its events reach any handler. It keeps them, for a test to deliver one
@Configuration(proxyBeanMethods = false)
internal class LostEvents {

    @Bean
    fun platformEventPublisher() = KeptEvents()
}

internal class KeptEvents : PlatformEventPublisher {

    val published = CopyOnWriteArrayList<PlatformEvent<*>>()

    override fun <EventType : PlatformEvent<EventType>> publish(event: EventType): EventType =
        event.also { published += it }
}
