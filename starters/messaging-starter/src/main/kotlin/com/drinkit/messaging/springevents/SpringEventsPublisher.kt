package com.drinkit.messaging.springevents

import com.drinkit.messaging.Event
import com.drinkit.messaging.EventPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
internal class SpringEventsPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
): EventPublisher {

    private val logger = KotlinLogging.logger { }

    override fun <EventType : Event<*>> publish(event: EventType): EventType {
        logger.debug { "Publishing event $event" }

        applicationEventPublisher.publishEvent(event)
        return event
    }
}