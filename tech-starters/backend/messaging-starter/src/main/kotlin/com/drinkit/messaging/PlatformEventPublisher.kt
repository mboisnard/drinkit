package com.drinkit.messaging

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
interface PlatformEventPublisher {

    /**
     * The event leaves once the current transaction commits, never when it rolls back, and at once outside a
     * transaction.
     */
    fun <EventType : PlatformEvent<EventType>> publish(event: EventType): EventType
}
