package com.drinkit.messaging

import org.springframework.scheduling.annotation.Async

@Async
annotation class EventHandler(
    val name: String = ""
)
