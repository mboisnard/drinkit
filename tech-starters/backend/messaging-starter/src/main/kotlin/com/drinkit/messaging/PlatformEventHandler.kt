package com.drinkit.messaging

import com.drinkit.documentation.tech.starter.TechStarterTool

/**
 * An [idempotent] handler runs once per key of its event, however many times it is delivered: the key of the
 * delivery, or the one [idempotencyKey] reads from the event, a SpEL expression such as `#event.orderId`.
 */
@TechStarterTool
@Suppress("LongParameterList")
annotation class PlatformEventHandler(
    val name: String,
    val exchange: String = "",
    val oneQueuePerInstance: Boolean = false,
    val maxAttempts: Int = 3,
    val delay: Long = 0,
    val idempotent: Boolean = false,
    val idempotencyKey: String = "",
)

// An annotation holds no code: its rules live here, and the processor of each implementation calls them
internal fun PlatformEventHandler.validate() {
    require(maxAttempts >= 1) { "Handler $name: maxAttempts must be at least 1" }
    require(delay >= 0) { "Handler $name: delay must not be negative" }
    // A broker would feed the queue of an instance from a waiting queue that outlives the instance
    require(delay == 0L || !oneQueuePerInstance) { "Handler $name: a delay does not combine with oneQueuePerInstance" }
    // Each instance must handle the event, where an idempotent handler handles it on one instance only
    require(
        !idempotent || !oneQueuePerInstance,
    ) { "Handler $name: idempotent does not combine with oneQueuePerInstance" }
    require(idempotent || idempotencyKey.isEmpty()) { "Handler $name: an idempotencyKey needs idempotent = true" }
}
