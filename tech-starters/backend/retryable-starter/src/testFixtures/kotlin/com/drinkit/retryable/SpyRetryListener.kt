package com.drinkit.retryable

import org.springframework.context.ApplicationListener
import org.springframework.resilience.retry.MethodRetryEvent

class SpyRetryListener : ApplicationListener<MethodRetryEvent> {

    private val retryEvents = mutableListOf<MethodRetryEvent>()

    override fun onApplicationEvent(event: MethodRetryEvent) {
        retryEvents += event
    }

    fun countFailedAttempts(): Int = retryEvents.count { !it.isRetryAborted }
}
