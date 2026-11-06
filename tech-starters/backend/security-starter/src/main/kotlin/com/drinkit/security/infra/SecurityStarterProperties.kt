package com.drinkit.security.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

private const val IDLE_MINUTES = 30L
private const val ABSOLUTE_HOURS = 8L
private const val REMEMBER_ME_DAYS = 30L
private const val CODE_MINUTES = 10L
private const val CODE_ATTEMPTS = 3
private const val CODE_ATTEMPTS_HOURS = 1L
private const val EMAIL_MAX_LENGTH = 320

/**
 * The settings of the starter, under `starters.security`, with defaults safe for production: sessions that end after
 * 30 idle minutes or 8 hours, remember-me logins that end 30 days after the sign-in, sign-in codes valid for 10
 * minutes with three wrong tries per login and per hour, and `__Host-` cookies sent over HTTPS only. Development over
 * plain HTTP sets an empty prefix and `secure=false`.
 *
 * OWASP: ASVS 5.0 V3.3, V7.3.1, V7.3.2, Session Management Cheat Sheet (cookie attributes, timeouts).
 */
@ConfigurationProperties(prefix = "starters.security")
internal data class SecurityStarterProperties(
    val enabled: Boolean = true,
    val session: Session = Session(),
    val rememberMe: RememberMe = RememberMe(),
    val code: Code = Code(),
    val cookie: Cookie = Cookie(),
) {
    data class Session(
        val idleTimeout: Duration = Duration.ofMinutes(IDLE_MINUTES),
        val absoluteTimeout: Duration = Duration.ofHours(ABSOLUTE_HOURS),
    )

    data class RememberMe(val validity: Duration = Duration.ofDays(REMEMBER_ME_DAYS))

    data class Code(
        val validity: Duration = Duration.ofMinutes(CODE_MINUTES),
        val maxAttempts: Int = CODE_ATTEMPTS,
        val attemptsWindow: Duration = Duration.ofHours(CODE_ATTEMPTS_HOURS),
        val maxEmailLength: Int = EMAIL_MAX_LENGTH,
    ) {
        init {
            require(maxAttempts > 0) { "starters.security.code.max-attempts must allow at least one try" }
        }
    }

    // A browser keeps a __Host- cookie only if it is Secure, has the path / and no domain, so that a subdomain cannot
    // set or overwrite it. Over plain HTTP, in development, the prefix must be empty.
    data class Cookie(val prefix: String = "__Host-", val secure: Boolean = true) {
        init {
            require(secure || prefix.isEmpty()) {
                "starters.security.cookie.prefix must be empty when starters.security.cookie.secure is false: " +
                    "a browser drops a prefixed cookie that is not Secure"
            }
        }

        val session get() = "${prefix}SESSION"
        val xsrfToken get() = "${prefix}XSRF-TOKEN"
        val rememberMe get() = "${prefix}remember-me"

        // Path=/ even when the application runs under a context path: a __Host- cookie requires it
        fun forSite(name: String, value: String?, maxAge: Int) = jakarta.servlet.http.Cookie(name, value).also {
            it.path = "/"
            it.maxAge = maxAge
            it.secure = secure
            it.isHttpOnly = true
            it.setAttribute("SameSite", SAME_SITE)
        }

        companion object {
            const val SAME_SITE = "Lax"
        }
    }
}
