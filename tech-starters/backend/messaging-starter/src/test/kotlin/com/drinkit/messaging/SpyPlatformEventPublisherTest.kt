package com.drinkit.messaging

import kotlin.reflect.KClass

internal class SpyPlatformEventPublisherTest : PlatformEventPublisherTestContract() {

    private val spy = SpyPlatformEventPublisher()

    override val publisher: PlatformEventPublisher = spy

    override fun countPublished(type: KClass<out PlatformEvent<*>>): Int = spy.countEventsOfType(type)
}
