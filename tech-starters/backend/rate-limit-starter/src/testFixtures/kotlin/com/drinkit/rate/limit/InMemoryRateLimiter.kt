package com.drinkit.rate.limit

import com.drinkit.rate.limit.infra.gcra.GcraRateLimiter
import com.drinkit.rate.limit.infra.gcra.Outcome
import com.drinkit.rate.limit.infra.gcra.TheoreticalArrivals
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** The starter's [RateLimiter], its counts kept in memory. */
fun inMemoryRateLimiter(clock: Clock = Clock.systemUTC()): RateLimiter =
    GcraRateLimiter(InMemoryTheoreticalArrivals(), clock)

internal class InMemoryTheoreticalArrivals : TheoreticalArrivals {

    private val arrivals = mutableMapOf<Pair<String, String>, Map<Duration, Instant>>()

    @Synchronized
    override fun update(
        rule: String,
        keyHash: String,
        now: Instant,
        decide: (Map<Duration, Instant>) -> Outcome,
    ): Outcome {
        val outcome = decide(arrivals[rule to keyHash].orEmpty())

        if (outcome is Outcome.Allowed) {
            arrivals[rule to keyHash] = outcome.theoreticalArrivals
        }

        return outcome
    }
}
