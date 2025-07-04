package com.drinkit.messaging.springevents

import com.drinkit.messaging.PlatformEvent
import com.drinkit.messaging.PlatformEventPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
internal class SpringEventsPublisherPlatform(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : PlatformEventPublisher {

    private val logger = KotlinLogging.logger { }

    override fun <EventType : PlatformEvent<*>> publish(event: EventType): EventType {
        logger.debug { "Publishing event $event" }

        applicationEventPublisher.publishEvent(event)
        return event
    }
}
