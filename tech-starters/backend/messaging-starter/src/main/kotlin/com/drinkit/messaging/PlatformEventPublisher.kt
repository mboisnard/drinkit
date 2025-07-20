package com.drinkit.messaging

interface PlatformEventPublisher {

    fun <EventType : PlatformEvent<EventType>> publish(event: EventType): EventType
}
