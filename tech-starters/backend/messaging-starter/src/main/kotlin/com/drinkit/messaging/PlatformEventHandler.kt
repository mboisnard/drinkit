package com.drinkit.messaging

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.springframework.scheduling.annotation.Async

@Async
@TechStarterTool
annotation class PlatformEventHandler(
    val name: String,
    val exchange: String = "",
    val oneQueuePerInstance: Boolean = false,
)
