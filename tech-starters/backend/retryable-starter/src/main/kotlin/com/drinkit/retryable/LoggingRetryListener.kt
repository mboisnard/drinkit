package com.drinkit.retryable

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.stereotype.Component

@Component(value = "loggingRetryListener")
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