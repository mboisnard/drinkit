package com.drinkit.security.infra.session

import com.drinkit.security.Sessions
import com.drinkit.security.infra.events.SecurityEventLogger
import com.drinkit.security.infra.rememberme.RememberMeTokens
import org.springframework.session.FindByIndexNameSessionRepository

/**
 * Also forgets the remember-me logins, which would otherwise sign the user in again at the next visit.
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
