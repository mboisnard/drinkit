package com.drinkit.rate.limit.infra

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.rate.limit.RateLimit
import com.drinkit.rate.limit.RateLimitDecision.Allowed
import com.drinkit.rate.limit.RateLimitDecision.Refused
import com.drinkit.rate.limit.RateLimitRule
import com.drinkit.rate.limit.RateLimiter
import com.drinkit.rate.limit.generated.jooq.DrinkitApplication
import com.drinkit.rate.limit.inMemoryRateLimiter
import com.drinkit.rate.limit.infra.gcra.GcraRateLimiter
import com.drinkit.test.ControlledClock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import org.springframework.aop.Advisor
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class RateLimitAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                DataSourceAutoConfiguration::class.java,
                DataSourceTransactionManagerAutoConfiguration::class.java,
                TransactionAutoConfiguration::class.java,
                JooqAutoConfiguration::class.java,
                RateLimitAutoConfiguration::class.java,
            ),
        )
        .withBean(Clock::class.java, { ControlledClock().fix(Instant.parse("2026-11-09T10:00:00Z")) })

    private val twoPerHour = RateLimitRule("shared", listOf(RateLimit(2, Duration.ofHours(1))))

    @Test
    fun `the starter gives a RateLimiter when a DSLContext exists`() {
        runner.run { context ->
            // When & Then
            context.getBean<RateLimiter>().shouldBeInstanceOf<GcraRateLimiter>()
        }
    }

    @Test
    fun `an application RateLimiter replaces the starter's`() {
        // Given
        val own = inMemoryRateLimiter()

        runner.withBean(RateLimiter::class.java, { own }).run { context ->
            // When & Then
            context.getBean<RateLimiter>() shouldBeSameInstanceAs own
        }
    }

    @Test
    fun `starters rate-limit enabled false removes every bean`() {
        // Given
        val disabled = runner.withPropertyValues("starters.rate-limit.enabled=false")

        disabled.run { context ->
            // When & Then
            context.getBeansOfType(RateLimiter::class.java).keys shouldBe emptySet()
            context.getBeansOfType(Advisor::class.java).keys.contains("rateLimitedAdvisor") shouldBe false
        }
    }

    @Test
    fun `two application contexts sharing a database share the count of a key`() {
        runner.run { first ->
            runner.run { second ->
                // Given
                first.getBean<RateLimiter>().consume(twoPerHour, "alice@drinkit.com")
                second.getBean<RateLimiter>().consume(twoPerHour, "alice@drinkit.com")

                // When
                val third = first.getBean<RateLimiter>().consume(twoPerHour, "alice@drinkit.com")

                // Then
                third shouldBe Refused("shared", Duration.ofMinutes(30))
            }
        }
    }

    @Test
    fun `concurrent first calls of a key are counted once each`() {
        runner.run { context ->
            // Given
            val rule = RateLimitRule("concurrent", listOf(RateLimit(5, Duration.ofHours(1))))
            val limiter = context.getBean<RateLimiter>()
            val start = CountDownLatch(1)
            val calls = List(CALLS) { Callable { start.await().let { limiter.consume(rule, "alice@drinkit.com") } } }

            // When
            val decisions = Executors.newFixedThreadPool(CALLS).use { pool ->
                val futures = calls.map(pool::submit)
                start.countDown()
                futures.map { it.get() }
            }

            // Then
            decisions.count { it == Allowed } shouldBe 5
        }
    }

    private companion object {
        const val CALLS = 8
    }
}
