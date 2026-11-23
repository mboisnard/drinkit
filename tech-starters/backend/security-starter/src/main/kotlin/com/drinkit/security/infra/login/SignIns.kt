package com.drinkit.security.infra.login

import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.events.SecurityEventLogger
import org.springframework.security.core.Authentication
import org.springframework.security.core.CredentialsContainer
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * Every way in, by code, remembered or [com.drinkit.security.CurrentUser.signIn], ends here: the application hears of
 * it by a direct call, in the request of the sign-in.
 *
 * OWASP: ASVS 5.0 V7.2.4, Session Management Cheat Sheet (session id renewal at sign-in).
 */
internal class SignIns(
    private val holder: SecurityContextHolderStrategy,
    private val accounts: UserAccounts,
    private val sessionStrategy: SessionAuthenticationStrategy,
    private val contextRepository: SecurityContextRepository,
    private val events: SecurityEventLogger,
) {

    fun signIn(authentication: Authentication, method: String): Authentication =
        connect(authentication).also { signedIn(it.name, method) }

    /** For a sign-in whose session Spring connects itself, as a remembered one. */
    fun signedIn(userId: String, method: String) {
        accounts.signedIn(userId)
        events.loginSucceeded(userId, method)
    }

    /** Under a new session id and a new XSRF token, so that an id planted before the change is worthless. */
    fun connect(authentication: Authentication): Authentication {
        // Spring's ProviderManager erases the credentials of a sign-in, which no sign-in here goes through: without
        // this, they would be stored in the session row
        (authentication as? CredentialsContainer)?.eraseCredentials()

        val exchange = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
        val response = requireNotNull(exchange.response) { "No response" }
        sessionStrategy.onAuthentication(authentication, exchange.request, response)

        val context = holder.createEmptyContext().also { it.authentication = authentication }
        holder.context = context
        contextRepository.saveContext(context, exchange.request, response)

        return authentication
    }
}
