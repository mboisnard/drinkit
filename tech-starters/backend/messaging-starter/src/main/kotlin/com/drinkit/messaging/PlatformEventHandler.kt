package com.drinkit.messaging

import org.springframework.scheduling.annotation.Async

@Async
annotation class PlatformEventHandler(
    val name: String = ""
)
