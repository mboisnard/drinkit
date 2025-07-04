package com.drinkit.messaging

interface PlatformEventPublisher {

    fun <EventType : PlatformEvent<*>> publish(event: EventType): EventType
}
