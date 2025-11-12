package com.drinkit.messaging

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
interface PlatformEventPublisher {

    fun <EventType : PlatformEvent<EventType>> publish(event: EventType): EventType
}
