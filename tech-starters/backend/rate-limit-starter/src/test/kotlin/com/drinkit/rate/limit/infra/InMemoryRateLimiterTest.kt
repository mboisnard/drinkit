package com.drinkit.rate.limit.infra

import com.drinkit.rate.limit.InMemoryTheoreticalArrivals
import com.drinkit.rate.limit.RateLimiter
import com.drinkit.rate.limit.infra.gcra.GcraRateLimiter

internal class InMemoryRateLimiterTest : RateLimiterTestContract() {

    override fun rateLimiter(): RateLimiter = GcraRateLimiter(InMemoryTheoreticalArrivals(), clock)
}
