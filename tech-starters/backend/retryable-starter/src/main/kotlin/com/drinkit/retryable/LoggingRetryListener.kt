package com.drinkit.retryable

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationListener
import org.springframework.core.retry.RetryException
import org.springframework.resilience.retry.MethodRetryEvent
import org.springframework.util.ClassUtils

internal class LoggingRetryListener : ApplicationListener<MethodRetryEvent> {

    private val logger = KotlinLogging.logger {}

    // A failure's message can hold personal data, such as the event a refused write carried: only its class is logged
    override fun onApplicationEvent(event: MethodRetryEvent) {
        val method = ClassUtils.getQualifiedMethodName(event.method)
        val failure = event.failure

        when {
            !event.isRetryAborted -> logger.info { "Attempt of $method failed: ${failure.javaClass.name}" }
            failure is RetryException && failure.retryCount > 0 -> logger.warn { "Retries of $method exhausted" }
        }
    }
}
