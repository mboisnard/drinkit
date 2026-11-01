package com.drinkit.messaging

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
annotation class PlatformEventHandler(
    val name: String,
    val exchange: String = "",
    val oneQueuePerInstance: Boolean = false,
    val maxAttempts: Int = 3,
    val delay: Long = 0,
)

// An annotation holds no code: its rules live here, and the processor of each implementation calls them
internal fun PlatformEventHandler.validate() {
    require(maxAttempts >= 1) { "Handler $name: maxAttempts must be at least 1" }
    require(delay >= 0) { "Handler $name: delay must not be negative" }
    // A broker would feed the queue of an instance from a waiting queue that outlives the instance
    require(delay == 0L || !oneQueuePerInstance) { "Handler $name: a delay does not combine with oneQueuePerInstance" }
}
