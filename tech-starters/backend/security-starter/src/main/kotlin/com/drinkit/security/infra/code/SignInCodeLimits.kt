package com.drinkit.security.infra.code

import com.drinkit.rate.limit.RateLimit
import com.drinkit.rate.limit.RateLimitDecision
import com.drinkit.rate.limit.RateLimitDecision.Allowed
import com.drinkit.rate.limit.RateLimitRule
import com.drinkit.rate.limit.RateLimiter
import com.drinkit.security.IpAddress
import java.time.Duration

private const val REQUESTS_PER_EMAIL_AND_HOUR = 5L
private const val CALLS_PER_IP_AND_DAY = 50L

private val perEmail = listOf(RateLimit(REQUESTS_PER_EMAIL_AND_HOUR, Duration.ofHours(1)))
private val perIp = listOf(RateLimit(CALLS_PER_IP_AND_DAY, Duration.ofDays(1)))

/**
 * The IP address counts before the email, so that a refused IP address costs its email nothing. A try counts before
 * its code is checked, so that guesses sent at the same time all count. Without a [RateLimiter], as with
 * `starters.rate-limit.enabled=false`, nothing is limited.
 *
 * OWASP: ASVS 5.0 V6.3.1, V6.6.3, Authentication Cheat Sheet (login throttling per account and per IP address).
 */
internal class SignInCodeLimits(private val rateLimiter: RateLimiter?) {

    private val requestsPerEmail = RateLimitRule("sign-in-code-requests-per-email", perEmail)
    private val requestsPerIp = RateLimitRule("sign-in-code-requests-per-ip", perIp)
    private val triesPerIp = RateLimitRule("sign-in-code-tries-per-ip", perIp)

    fun requestFrom(ipAddress: IpAddress) = consume(requestsPerIp, ipAddress.host())

    fun requestFor(login: String) = consume(requestsPerEmail, login)

    fun tryFrom(ipAddress: IpAddress) = consume(triesPerIp, ipAddress.host())

    private fun consume(rule: RateLimitRule, key: String): RateLimitDecision =
        rateLimiter?.consume(rule, key) ?: Allowed
}
