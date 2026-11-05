package com.drinkit.security.infra.login

import com.drinkit.security.ConnectedUser
import com.drinkit.security.CurrentUser
import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.events.SecurityEventLogger
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.CredentialsContainer
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

private const val FACTOR_PREFIX = "FACTOR_"

/**
 * Gives the application its connected user through [CurrentUser], so that no business code touches the security
 * context of Spring Security. It also signs a user in, or reloads their roles, the way a login would: under a new
 * session id, so that an id an attacker planted before the change is worthless, and with the login saved in the
 * session.
 *
 * OWASP: ASVS 5.0 V7.2.4, V8.3.2, Session Management Cheat Sheet (id renewal on privilege change).
 */
internal class SpringCurrentUser(
    private val holder: SecurityContextHolderStrategy,
    private val accounts: UserAccounts,
    private val sessionStrategy: SessionAuthenticationStrategy,
    private val contextRepository: SecurityContextRepository,
    private val events: SecurityEventLogger,
) : CurrentUser {

    override fun get(): ConnectedUser = orNull() ?: throw AuthenticationCredentialsNotFoundException("Anonymous")

    override fun orNull(): ConnectedUser? = holder.context.authentication
        ?.takeIf { it.isAuthenticated && it !is AnonymousAuthenticationToken }
        ?.toConnectedUser()

    override fun signIn(userId: String): ConnectedUser {
        val user = requireNotNull(accounts.findById(userId)) { "No account $userId to sign in" }.toUserDetails()
        val connected = connect(UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities))

        events.loginSucceeded(connected.id, "sign_in")
        return connected
    }

    override fun refresh(): ConnectedUser? {
        val current = holder.context.authentication?.takeIf { orNull() != null } ?: return null

        return when (val account = accounts.findById(current.name)) {
            null -> null.also { signOut(current) }
            else -> connect(current.reloadedWith(account.toUserDetails()))
        }
    }

    // Rebuilt from the current authentication rather than a new one, so that it stays remembered or not and keeps the
    // FACTOR_ authorities its login proved, such as the password: only the roles are replaced.
    private fun Authentication.reloadedWith(user: UserDetails) = toBuilder()
        .principal(user)
        .authorities { authorities ->
            authorities.removeIf { !it.isFactor() }
            authorities.addAll(user.authorities)
        }
        .build()

    private fun connect(authentication: Authentication): ConnectedUser {
        // A login erases the password hash in Spring's ProviderManager, which signIn and refresh do not go through:
        // without this, the hash would be stored in the session row.
        (authentication as? CredentialsContainer)?.eraseCredentials()

        val exchange = currentExchange()
        sessionStrategy.onAuthentication(authentication, exchange.request, exchange.exchangeResponse)

        val context = holder.createEmptyContext().also { it.authentication = authentication }
        holder.context = context
        contextRepository.saveContext(context, exchange.request, exchange.exchangeResponse)

        return authentication.toConnectedUser()
    }

    private fun signOut(authentication: Authentication) {
        val exchange = currentExchange()
        SecurityContextLogoutHandler()
            .also {
                it.setSecurityContextHolderStrategy(holder)
                it.setSecurityContextRepository(contextRepository)
            }
            .logout(exchange.request, exchange.exchangeResponse, authentication)
    }

    private fun currentExchange() = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes

    private val ServletRequestAttributes.exchangeResponse get() = requireNotNull(response) { "No response" }

    private fun Authentication.toConnectedUser() = ConnectedUser(
        id = name,
        roles = authorities.filterNot { it.isFactor() }.mapNotNull { it.authority }.toSet(),
    )

    private fun GrantedAuthority.isFactor() = authority?.startsWith(FACTOR_PREFIX) == true
}
