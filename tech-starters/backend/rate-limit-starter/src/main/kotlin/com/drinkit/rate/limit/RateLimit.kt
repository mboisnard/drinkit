package com.drinkit.rate.limit

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.time.Duration

/** At most [limit] calls per [period], on average: a key that waited regains one call every `period / limit`. */
@TechStarterTool
data class RateLimit(val limit: Long, val period: Duration) {
    init {
        require(limit > 0) { "A rate limit needs a positive count, not $limit" }
        require(period.isPositive) { "A rate limit needs a positive period, not $period" }
    }
}

/** The limits a key must all respect, counted apart from any other rule's. */
@TechStarterTool
data class RateLimitRule(val name: String, val limits: List<RateLimit>) {
    init {
        require(name.isNotBlank()) { "A rate limit rule needs a name" }
        require(limits.isNotEmpty()) { "The rate limit rule $name needs at least one limit" }
        require(limits.distinctBy { it.period }.size == limits.size) { "Two limits of the rule $name share a period" }
    }
}
