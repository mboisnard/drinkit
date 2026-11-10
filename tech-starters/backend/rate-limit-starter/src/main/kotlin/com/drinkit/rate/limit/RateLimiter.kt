package com.drinkit.rate.limit

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.web.ErrorResponseException
import java.time.Duration

/** Counts a call against the limits of a rule, and tells whether it may go on. */
@TechStarterTool
interface RateLimiter {

    /** Counts against [key], such as an email, the id of a user or the host of a third-party site. */
    fun consume(rule: RateLimitRule, key: String, cost: Long = 1): RateLimitDecision
}

@TechStarterTool
sealed interface RateLimitDecision {

    /** Throws [RateLimitExceededException] when refused, which answers 429 with `Retry-After` over HTTP. */
    fun orThrow()

    data object Allowed : RateLimitDecision {
        override fun orThrow() = Unit
    }

    data class Refused(val rule: String, val retryAfter: Duration) : RateLimitDecision {

        /** The value of `Retry-After`, rounded up so that a client retrying on time is let through. */
        val retryAfterSeconds: Long get() = retryAfter.plusNanos(NANOS_PER_SECOND - 1).seconds.coerceAtLeast(1)

        override fun orThrow(): Nothing = throw RateLimitExceededException(rule, retryAfter)

        private companion object {
            const val NANOS_PER_SECOND = 1_000_000_000L
        }
    }
}

/** Answers 429 with `Retry-After` in whole seconds. */
@TechStarterTool
class RateLimitExceededException(val rule: String, val retryAfter: Duration) :
    ErrorResponseException(TOO_MANY_REQUESTS) {

    init {
        val seconds = RateLimitDecision.Refused(rule, retryAfter).retryAfterSeconds

        headers.set(RETRY_AFTER, seconds.toString())
        setDetail("Too many calls, retry in $seconds s")
    }
}
