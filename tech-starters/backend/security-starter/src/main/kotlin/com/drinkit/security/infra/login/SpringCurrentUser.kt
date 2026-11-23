package com.drinkit.security.infra.login

import com.drinkit.security.ConnectedUser
import com.drinkit.security.CurrentUser
import com.drinkit.security.UserAccounts
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

private const val FACTOR_PREFIX = "FACTOR_"

/**
 * Signs in and reloads roles through [SignIns], under a new session id.
 *
 * OWASP: ASVS 5.0 V7.2.4, V8.3.2, Session Management Cheat Sheet (id renewal on privilege change).
 */
internal class SpringCurrentUser(
    private val holder: SecurityContextHolderStrategy,
    private val accounts: UserAccounts,
    private val signIns: SignIns,
    private val contextRepository: SecurityContextRepository,
) : CurrentUser {

    override fun get(): ConnectedUser = orNull() ?: throw AuthenticationCredentialsNotFoundException("Anonymous")

    override fun orNull(): ConnectedUser? = holder.context.authentication
        ?.takeIf { it.isAuthenticated && it !is AnonymousAuthenticationToken }
        ?.toConnectedUser()

    override fun signIn(userId: String): ConnectedUser {
        val user = requireNotNull(accounts.findById(userId)) { "No account $userId to sign in" }.toUserDetails()

        return signIns.signIn(
            UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities),
            "sign_in",
        )
            .toConnectedUser()
    }

    override fun refresh(): ConnectedUser? {
        val current = holder.context.authentication?.takeIf { orNull() != null } ?: return null

        return when (val account = accounts.findById(current.name)) {
            null -> null.also { signOut(current) }
            else -> signIns.connect(current.reloadedWith(account.toUserDetails())).toConnectedUser()
        }
    }

    // Rebuilt from the current authentication, so that it stays remembered or not and keeps the FACTOR_ authorities
    // its sign-in proved: only the roles change
    private fun Authentication.reloadedWith(user: UserDetails) = toBuilder()
        .principal(user)
        .authorities { authorities ->
            authorities.removeIf { !it.isFactor() }
            authorities.addAll(user.authorities)
        }
        .build()

    private fun signOut(authentication: Authentication) {
        val exchange = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
        SecurityContextLogoutHandler()
            .also {
                it.setSecurityContextHolderStrategy(holder)
                it.setSecurityContextRepository(contextRepository)
            }
            .logout(exchange.request, requireNotNull(exchange.response) { "No response" }, authentication)
    }

    private fun Authentication.toConnectedUser() = ConnectedUser(
        id = name,
        roles = authorities.filterNot { it.isFactor() }.mapNotNull { it.authority }.toSet(),
    )

    private fun GrantedAuthority.isFactor() = authority?.startsWith(FACTOR_PREFIX) == true
}
