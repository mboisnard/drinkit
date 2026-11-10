package com.drinkit.security.infra.code

import com.drinkit.rate.limit.RateLimitDecision
import com.drinkit.rate.limit.RateLimitDecision.Allowed
import com.drinkit.rate.limit.inMemoryRateLimiter
import com.drinkit.security.IpAddress
import com.drinkit.test.ControlledClock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.time.Duration

private const val REQUESTS_PER_EMAIL_AND_HOUR = 5
private const val REQUESTS_PER_IP = 50

internal class SignInCodeLimitsTest {

    private val clock = ControlledClock().fix()
    private val limits = SignInCodeLimits(inMemoryRateLimiter(clock))

    @Test
    fun `refuses an email a sixth code request within an hour, however spaced`() {
        // Given
        repeat(REQUESTS_PER_EMAIL_AND_HOUR) {
            limits.requestFor("ada@drinkit.test") shouldBe Allowed
            clock.add(Duration.ofMinutes(1))
        }

        // When
        val sixth = limits.requestFor("ada@drinkit.test")

        // Then
        sixth.shouldBeInstanceOf<RateLimitDecision.Refused>()
    }

    @Test
    fun `limits each email apart`() {
        // Given
        repeat(REQUESTS_PER_EMAIL_AND_HOUR) { limits.requestFor("ada@drinkit.test") }

        // When
        val another = limits.requestFor("grace@drinkit.test")

        // Then
        another shouldBe Allowed
    }

    @Test
    fun `counts the IP addresses of one IPv6 network as one`() {
        // Given
        repeat(REQUESTS_PER_IP) { limits.requestFrom(IpAddress("2001:db8:5:7::${it.toString(16)}")) }

        // When
        val fromTheSameNetwork = limits.requestFrom(IpAddress("2001:db8:5:7:ffff::1"))

        // Then
        fromTheSameNetwork.shouldBeInstanceOf<RateLimitDecision.Refused>()
    }

    @Test
    fun `limits nothing without a rate limiter, as when rate-limit-starter is disabled`() {
        // Given
        val unlimited = SignInCodeLimits(null)
        val ipAddress = IpAddress("203.0.113.7")

        // When
        val decisions = List(REQUESTS_PER_IP + 1) {
            listOf(
                unlimited.requestFrom(ipAddress),
                unlimited.requestFor("ada@drinkit.test"),
                unlimited.tryFrom(ipAddress),
            )
        }

        // Then
        decisions.flatten().toSet() shouldBe setOf(Allowed)
    }
}
