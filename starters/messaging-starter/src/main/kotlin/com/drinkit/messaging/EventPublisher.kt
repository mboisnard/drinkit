package com.drinkit.messaging

interface EventPublisher {

    fun <EventType : Event<*>> publish(event: EventType): EventType
}