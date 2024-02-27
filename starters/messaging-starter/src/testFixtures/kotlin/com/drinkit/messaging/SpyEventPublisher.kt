package com.drinkit.messaging

import kotlin.reflect.KClass

class SpyEventPublisher : EventPublisher {

    private val publishedEvents = mutableListOf<Event<*>>()

    override fun <EventType : Event<*>> publish(event: EventType): EventType {
        publishedEvents += event
        return event
    }

    fun isPublished(eventType: KClass<out Event<*>>): Boolean =
        publishedEvents.any { it.javaClass.kotlin == eventType }

    fun countEventsOfType(eventType: KClass<out Event<*>>): Int =
        publishedEvents.count { it.javaClass.kotlin == eventType }

    fun findLastSentEvent(): Event<*>? = publishedEvents.lastOrNull()
}