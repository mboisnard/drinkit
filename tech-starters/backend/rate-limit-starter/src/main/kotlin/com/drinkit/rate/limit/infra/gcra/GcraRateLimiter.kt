package com.drinkit.rate.limit.infra.gcra

import com.drinkit.kotlin.sha256
import com.drinkit.rate.limit.RateLimit
import com.drinkit.rate.limit.RateLimitDecision
import com.drinkit.rate.limit.RateLimitRule
import com.drinkit.rate.limit.RateLimiter
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** OWASP: ASVS 5.0 V2.4.1, V16.3.3 (anti-automation, logged refusals). */
internal class GcraRateLimiter(private val theoreticalArrivals: TheoreticalArrivals, private val clock: Clock) :
    RateLimiter {

    private val logger = KotlinLogging.logger { }

    override fun consume(rule: RateLimitRule, key: String, cost: Long): RateLimitDecision {
        val smallestLimit = rule.limits.minOf { it.limit }
        require(cost in 1..smallestLimit) { "A call to ${rule.name} costs from 1 to $smallestLimit, not $cost" }

        val now = clock.instant()
        val outcome = theoreticalArrivals.update(rule.name, key.sha256(), now) { Gcra.decide(rule, it, now, cost) }

        return when (outcome) {
            is Outcome.Allowed -> RateLimitDecision.Allowed

            is Outcome.Refused -> RateLimitDecision.Refused(rule.name, outcome.retryAfter).also {
                logger.warn { "Rate limit ${rule.name} refused a call, retry in ${outcome.retryAfter.toSeconds()}s" }
            }
        }
    }
}

/**
 * The generic cell rate algorithm, https://brandur.org/rate-limiting: each limit of a key keeps the instant its
 * next call is theoretically due, and a call passes while that instant stays within one period of now.
 */
internal object Gcra {

    fun decide(rule: RateLimitRule, theoreticalArrivals: Map<Duration, Instant>, now: Instant, cost: Long): Outcome {
        val nextArrivals = rule.limits.associate { limit ->
            limit.period to nextArrival(limit, theoreticalArrivals[limit.period], now, cost)
        }

        val retryAfter = nextArrivals.map { (period, next) -> Duration.between(now, next.minus(period)) }.max()

        return if (retryAfter.isPositive) Outcome.Refused(retryAfter) else Outcome.Allowed(nextArrivals)
    }

    private fun nextArrival(limit: RateLimit, theoreticalArrival: Instant?, now: Instant, cost: Long): Instant {
        val emissionInterval = limit.period.dividedBy(limit.limit)
        val from = maxOf(theoreticalArrival ?: now, now)

        return from.plus(emissionInterval.multipliedBy(cost))
    }
}

/** The theoretical arrival of each limit of a key, by period, which only one call at a time reads and changes. */
internal interface TheoreticalArrivals {

    /** Saves the arrivals [decide] returns when it allows the call, and forgets those [now] has passed. */
    fun update(rule: String, keyHash: String, now: Instant, decide: (Map<Duration, Instant>) -> Outcome): Outcome
}

internal sealed interface Outcome {
    data class Allowed(val theoreticalArrivals: Map<Duration, Instant>) : Outcome
    data class Refused(val retryAfter: Duration) : Outcome
}
