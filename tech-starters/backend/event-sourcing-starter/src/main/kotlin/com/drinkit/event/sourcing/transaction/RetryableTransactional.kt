package com.drinkit.event.sourcing.transaction

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.springframework.resilience.annotation.Retryable
import org.springframework.transaction.annotation.Propagation.NESTED
import org.springframework.transaction.annotation.Transactional
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * Thrown by an event store when another write already took the sequence id of the event it appends.
 */
@TechStarterTool
class DuplicateSequenceException(override val message: String, override val cause: Throwable?) :
    RuntimeException(message, cause)

/**
 * Runs a use case again when the event it appends loses a race for its sequence id.
 *
 * On a [DuplicateSequenceException], [Retryable] makes three attempts at most, and each one reads the new history.
 *
 * The propagation is NESTED. Inside an open transaction, each attempt runs in a savepoint of it: a refused attempt
 * rolls back to its savepoint, and the outer transaction stays usable. With no open transaction, each attempt runs
 * in a new transaction.
 *
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.resilience.annotation.Retryable
 */
@TechStarterTool
@Retention(AnnotationRetention.RUNTIME)
@Target(CLASS)
@Transactional(propagation = NESTED, readOnly = false)
@Retryable(includes = [DuplicateSequenceException::class], maxRetries = 2, delay = 0)
annotation class RetryableTransactional
