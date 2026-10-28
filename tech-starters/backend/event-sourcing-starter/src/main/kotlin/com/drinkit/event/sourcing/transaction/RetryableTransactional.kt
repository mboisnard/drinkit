package com.drinkit.event.sourcing.transaction

import org.springframework.resilience.annotation.Retryable
import org.springframework.transaction.annotation.Propagation.NESTED
import org.springframework.transaction.annotation.Transactional
import kotlin.annotation.AnnotationTarget.CLASS

class DuplicateSequenceException(override val message: String, override val cause: Throwable?) :
    RuntimeException(message, cause)

/**
 * Composite annotation that ensures reliable event saving in a concurrent context.
 *
 * It combines a nested transaction with a [Retryable] mechanism
 * to handle [DuplicateSequenceException] failures: three attempts at most,
 * each one in a transaction of its own, so that it reads the new history.
 *
 * The NESTED propagation is crucial: it creates a savepoint that isolates the failure,
 * allowing the retry to execute without rolling back the main transaction.
 *
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.resilience.annotation.Retryable
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(CLASS)
@Transactional(propagation = NESTED, readOnly = false)
@Retryable(includes = [DuplicateSequenceException::class], maxRetries = 2, delay = 0)
annotation class RetryableTransactional
