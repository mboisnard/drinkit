package com.drinkit.messaging

import com.drinkit.messaging.infra.springevents.SomethingHappened
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal abstract class PlatformEventPublisherTestContract {

    protected abstract val publisher: PlatformEventPublisher

    protected abstract fun countPublished(type: KClass<out PlatformEvent<*>>): Int

    @Test
    fun `returns the event it published`() {
        // When
        val published = publisher.publish(SomethingHappened(1))

        // Then
        published shouldBe SomethingHappened(1)
    }

    @Test
    fun `publishes each of two equal events`() {
        // When
        publisher.publish(SomethingHappened(1))
        publisher.publish(SomethingHappened(1))

        // Then
        countPublished(SomethingHappened::class) shouldBe 2
    }
}
