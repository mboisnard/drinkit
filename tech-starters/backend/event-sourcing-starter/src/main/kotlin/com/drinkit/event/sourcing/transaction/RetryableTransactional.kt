package com.drinkit.event.sourcing.transaction

import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.transaction.annotation.Propagation.NESTED
import org.springframework.transaction.annotation.Transactional
import kotlin.annotation.AnnotationTarget.CLASS


class DuplicateSequenceException(override val message: String, override val cause: Throwable?) :
    RuntimeException(message, cause)

/**
 * Composite annotation that ensures reliable event saving in a concurrent context.
 *
 * It combines a nested transaction with a {@code @Retryable} mechanism
 * to handle {@code DuplicateSequenceException} failures.
 *
 * The NESTED propagation is crucial: it creates a savepoint that isolates the failure,
 * allowing the retry to execute without rolling back the main transaction.
 *
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.retry.annotation.Retryable
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(CLASS)
@Transactional(propagation = NESTED, readOnly = false)
@Retryable(
    value = [DuplicateSequenceException::class],
    backoff = Backoff(value = 0),
    listeners = ["loggingRetryListener"],
    maxAttempts = 3,
)
annotation class RetryableTransactional
