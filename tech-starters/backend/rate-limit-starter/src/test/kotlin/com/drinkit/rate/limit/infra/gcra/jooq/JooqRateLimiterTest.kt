package com.drinkit.rate.limit.infra.gcra.jooq

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.rate.limit.RateLimit
import com.drinkit.rate.limit.RateLimitRule
import com.drinkit.rate.limit.RateLimiter
import com.drinkit.rate.limit.generated.jooq.DrinkitApplication
import com.drinkit.rate.limit.generated.jooq.tables.references.RATE_LIMIT
import com.drinkit.rate.limit.infra.RateLimiterTestContract
import com.drinkit.rate.limit.infra.gcra.GcraRateLimiter
import io.kotest.matchers.collections.shouldContainExactly
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqRateLimiterTest : RateLimiterTestContract() {

    private lateinit var dsl: DSLContext

    @BeforeEach
    fun setup(dsl: DSLContext) {
        this.dsl = dsl
    }

    override fun rateLimiter(): RateLimiter = GcraRateLimiter(JooqTheoreticalArrivals(dsl), clock)

    private val twoPerHour = RateLimitRule("signin", listOf(RateLimit(2, Duration.ofHours(1))))

    @Test
    fun `a row whose arrival has passed is cleaned up`() {
        // Given
        val limiter = rateLimiter()
        limiter.consume(twoPerHour, "alice@drinkit.com")
        clock.add(Duration.ofMinutes(31))

        // When
        limiter.consume(twoPerHour, "bob@drinkit.com")

        // Then
        storedKeyHashes() shouldContainExactly listOf(BOB_HASH)
    }

    @Test
    fun `the stored key is a hash, never the raw key`() {
        // When
        rateLimiter().consume(twoPerHour, "alice@drinkit.com")

        // Then
        storedKeyHashes() shouldContainExactly listOf(ALICE_HASH)
    }

    private fun storedKeyHashes() = dsl.select(RATE_LIMIT.KEY_HASH).from(RATE_LIMIT).fetch(RATE_LIMIT.KEY_HASH)

    private companion object {
        // SHA-256 of alice@drinkit.com and bob@drinkit.com
        const val ALICE_HASH = "db8b8ccceb97e68d0f90664517199f8fe48765acea93f7d810b0a369dc892232"
        const val BOB_HASH = "08634f072c40a66219a3641eff532a7bffc157db97d546084f958235fd424630"
    }
}
