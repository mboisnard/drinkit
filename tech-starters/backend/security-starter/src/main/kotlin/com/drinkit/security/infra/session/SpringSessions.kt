package com.drinkit.security.infra.session

import com.drinkit.security.Sessions
import com.drinkit.security.infra.events.SecurityEventLogger
import com.drinkit.security.infra.rememberme.RememberMeTokens
import org.springframework.session.FindByIndexNameSessionRepository

/**
 * Signs a user out on every device, through the session repository of Spring Session, which finds the sessions of a
 * user on every instance. It also forgets their remember-me logins, which would otherwise sign them in again at the
 * next visit. The starter calls it when a remember-me token comes back after its rotation, as a stolen one would.
 *
 * OWASP: ASVS 5.0 V7.4.1, Session Management Cheat Sheet (revoking all sessions).
 */
internal class SpringSessions(
    private val repository: FindByIndexNameSessionRepository<*>,
    private val rememberMeTokens: RememberMeTokens,
    private val events: SecurityEventLogger,
) : Sessions {

    override fun revokeAll(userId: String) {
        repository.findByPrincipalName(userId).keys.forEach(repository::deleteById)
        rememberMeTokens.deleteAllOf(userId)

        events.sessionsRevoked(userId)
    }
}
