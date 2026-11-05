package com.drinkit.security.infra.events

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.security.authentication.AuthenticationTrustResolverImpl
import org.springframework.security.authentication.RememberMeAuthenticationToken
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.authorization.event.AuthorizationDeniedEvent

/**
 * Writes the security events in the vocabulary of the OWASP Logging Vocabulary cheat sheet, such as
 * `authn_login_success`, `authn_login_fail`, `authz_fail` or `session_revoked`, so that alerts and investigations
 * can search for a stable event name. Each line names the user by id only, never by email, and holds no password,
 * token, cookie or session id: personal data and secrets stay out of the logs.
 *
 * OWASP: ASVS 5.0 V16.3.1, V16.3.2, V16.2.5, Logging Vocabulary and Logging Cheat Sheets (event names, no secrets).
 */
internal class SecurityEventLogger {

    private val logger = KotlinLogging.logger {}
    private val trustResolver = AuthenticationTrustResolverImpl()

    fun loginSucceeded(userId: String, method: String) {
        logger.info { "authn_login_success:$userId,method=$method" }
    }

    fun rememberMeGraceUsed(userId: String) {
        logger.info { "authn_token_grace_used:$userId" }
    }

    fun rememberMeTokenReused(userId: String) {
        logger.warn { "authn_token_reuse:$userId" }
    }

    fun sessionsRevoked(userId: String) {
        logger.info { "session_revoked:$userId" }
    }

    fun sessionDeserializationFailed() {
        logger.info { "session_deserialization_fail" }
    }

    @EventListener
    fun onLoginSuccess(event: AuthenticationSuccessEvent) {
        val method = if (event.authentication is RememberMeAuthenticationToken) "remember_me" else "password"
        loginSucceeded(event.authentication.name, method)
    }

    @EventListener(AbstractAuthenticationFailureEvent::class)
    fun onLoginFailure() {
        logger.info { "authn_login_fail" }
    }

    @EventListener
    fun onAccessDenied(event: AuthorizationDeniedEvent<*>) {
        val authentication = event.authentication.get()

        if (trustResolver.isAuthenticated(authentication)) logger.info { "authz_fail:${authentication.name}" }
    }
}
