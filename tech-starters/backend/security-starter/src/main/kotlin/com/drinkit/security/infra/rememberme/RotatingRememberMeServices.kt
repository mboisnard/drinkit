package com.drinkit.security.infra.rememberme

import com.drinkit.security.Sessions
import com.drinkit.security.infra.SecurityStarterProperties
import com.drinkit.security.infra.events.SecurityEventLogger
import com.drinkit.security.infra.hashOf
import com.drinkit.security.infra.login.UserAccountsUserDetailsService
import com.drinkit.security.infra.matchesHash
import com.drinkit.security.infra.randomToken
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices
import org.springframework.security.web.authentication.rememberme.InvalidCookieException
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException
import java.time.Clock
import java.time.Duration
import java.time.Instant

// How long the previous token stays valid after a rotation: long enough for the requests a page sends in parallel
// with the same cookie, too short for an attacker to replay a stolen one.
private val GRACE: Duration = Duration.ofSeconds(10)
private const val USER_AGENT_LENGTH = 256

private const val NANOS_PER_MICRO = 1_000L

/**
 * Signs a returning user in from their remember-me cookie, which holds a series and a token. The token changes at
 * each visit and is stored hashed, so that a token that comes back after its rotation reveals a theft, of the cookie
 * or of the browser: the user is then signed out everywhere. A series ends at a fixed time after the sign-in, however
 * often the user comes back, so that a stolen cookie does not open the account forever.
 *
 * OWASP: ASVS 5.0 V7.2.3, V7.3.2, V7.4.1 (rotated hashed token, reuse detection, absolute lifetime).
 */
@Suppress("LongParameterList")
internal class RotatingRememberMeServices(
    private val users: UserAccountsUserDetailsService,
    private val rememberMeTokens: RememberMeTokens,
    private val sessions: Sessions,
    private val events: SecurityEventLogger,
    private val clock: Clock,
    private val validity: Duration,
    private val cookie: SecurityStarterProperties.Cookie,
) : AbstractRememberMeServices(randomToken(), users) {

    init {
        setCookieName(cookie.rememberMe)
    }

    // Rows keep microseconds: a time with nanoseconds would shift every comparison and lifetime by a fraction
    private val microsecondClock = Clock.tick(clock, Duration.ofNanos(NANOS_PER_MICRO))

    override fun rememberMeRequested(request: HttpServletRequest, parameter: String) = request.rememberMeRequested

    override fun onLoginSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        successfulAuthentication: Authentication,
    ) {
        val now = microsecondClock.instant()
        rememberMeTokens.deleteCreatedBefore(now - validity)
        // A copy of the cookie this browser held would otherwise stay valid until its series ends
        presentedSeries(request)?.let(rememberMeTokens::delete)

        val series = randomToken()
        val token = randomToken()
        rememberMeTokens.create(
            RememberMeSeries(series, successfulAuthentication.name, hashOf(token), null, now, now),
            request.getHeader(HttpHeaders.USER_AGENT)?.take(USER_AGENT_LENGTH),
        )

        setCookie(arrayOf(series, token), validity.toSeconds().toInt(), request, response)
    }

    override fun processAutoLoginCookie(
        cookieTokens: Array<String>,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): UserDetails {
        if (cookieTokens.size != 2) throw InvalidCookieException("A remember-me cookie holds a series and a token")

        val (seriesId, token) = cookieTokens
        val now = microsecondClock.instant()
        val series = liveSeries(seriesId, now)

        val presented = hashOf(token)
        if (presented.matchesHash(series.tokenHash)) {
            rotate(series, presented, now, request, response)
        } else {
            replay(series, presented, now)
        }

        return users.loadUserById(series.userId)
    }

    override fun logout(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?) {
        presentedSeries(request)?.let(rememberMeTokens::delete)
    }

    override fun setCookie(
        tokens: Array<String>,
        maxAge: Int,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        response.addCookie(cookie.forSite(cookieName, encodeCookie(tokens), maxAge))
    }

    override fun cancelCookie(request: HttpServletRequest, response: HttpServletResponse) {
        response.addCookie(cookie.forSite(cookieName, null, 0))
    }

    private fun presentedSeries(request: HttpServletRequest): String? = extractRememberMeCookie(request)
        ?.let { runCatching { decodeCookie(it) }.getOrNull() }
        ?.firstOrNull()

    private fun liveSeries(seriesId: String, now: Instant): RememberMeSeries {
        val series = rememberMeTokens.find(seriesId)
            ?: throw RememberMeAuthenticationException("Unknown remember-me series")

        if (!now.isBefore(series.endsAt())) {
            rememberMeTokens.delete(seriesId)
            throw RememberMeAuthenticationException("Expired remember-me series")
        }

        return series
    }

    private fun rotate(
        series: RememberMeSeries,
        presented: String,
        now: Instant,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val token = randomToken()

        if (rememberMeTokens.rotate(series.series, presented, hashOf(token), now)) {
            val timeLeft = Duration.between(now, series.endsAt())
            setCookie(arrayOf(series.series, token), timeLeft.toSeconds().toInt(), request, response)
        } else {
            val current = rememberMeTokens.find(series.series)
                ?: throw RememberMeAuthenticationException("Remember-me series ended during its rotation")
            replay(current, presented, now)
        }
    }

    private fun replay(series: RememberMeSeries, presented: String, now: Instant) {
        val previous = series.previousTokenHash

        if (previous != null && presented.matchesHash(previous) && now.isBefore(series.rotatedAt + GRACE)) {
            events.rememberMeGraceUsed(series.userId)
        } else {
            sessions.revokeAll(series.userId)
            events.rememberMeTokenReused(series.userId)
            throw RememberMeAuthenticationException("Remember-me token used again")
        }
    }

    private fun RememberMeSeries.endsAt() = createdAt + validity
}
