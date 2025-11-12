package com.drinkit.retryable

import com.drinkit.documentation.tech.starter.TechStarterTool
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.stereotype.Component

/**
 * An implementation of [RetryListener] that logs each failed retry attempt.
 *
 * This component hooks into the retry mechanism to provide a trace
 * for any occurring errors. On each failure of a retryable operation,
 * it logs a message containing the attempt count and the captured exception.
 *
 * @see RetryListener
 */
@Component(value = "loggingRetryListener")
@TechStarterTool
internal class LoggingRetryListener: RetryListener {

    private val logger = KotlinLogging.logger {}

    override fun <T : Any, E : Throwable> onError(
        context: RetryContext,
        callback: RetryCallback<T, E>,
        throwable: Throwable,
    ) {
        super.onError(context, callback, throwable)
        logger.info(throwable) { "Error on retry: ${context.retryCount}" }
    }
}