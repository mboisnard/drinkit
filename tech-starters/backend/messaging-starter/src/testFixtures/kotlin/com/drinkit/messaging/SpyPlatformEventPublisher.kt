package com.drinkit.messaging

import kotlin.reflect.KClass

class SpyPlatformEventPublisher : PlatformEventPublisher {

    private val publishedPlatformEvents = mutableListOf<PlatformEvent<*>>()

    override fun <EventType : PlatformEvent<*>> publish(event: EventType): EventType {
        publishedPlatformEvents += event
        return event
    }

    fun isPublished(platformEventType: KClass<out PlatformEvent<*>>): Boolean =
        publishedPlatformEvents.any { it.javaClass.kotlin == platformEventType }

    fun countEventsOfType(platformEventType: KClass<out PlatformEvent<*>>): Int =
        publishedPlatformEvents.count { it.javaClass.kotlin == platformEventType }

    fun findLastSentEvent(): PlatformEvent<*>? = publishedPlatformEvents.lastOrNull()
}