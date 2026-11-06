package com.drinkit.security.infra.code

import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.hashOf
import com.drinkit.security.infra.login.toUserDetails
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.FactorGrantedAuthority
import java.time.Clock
import java.util.Locale

internal class EmailCodeAuthenticationToken(val login: String, val code: String, val locale: Locale) :
    AbstractAuthenticationToken(emptyList()) {

    override fun getCredentials() = code

    override fun getPrincipal() = login

    // The default one prints the principal, an email, wherever a token is logged
    override fun toString() = "EmailCodeAuthenticationToken"
}

/**
 * Every refusal is the same [BadCredentialsException]: a wrong code, an unknown login and a refused account alike.
 *
 * OWASP: ASVS 5.0 V6.3.8, V6.6, Authentication Cheat Sheet (generic failures, out-of-band codes).
 */
internal class EmailCodeAuthenticationProvider(
    private val codes: SignInCodes,
    private val accounts: UserAccounts,
    private val clock: Clock,
) : AuthenticationProvider {

    override fun supports(authentication: Class<*>) =
        EmailCodeAuthenticationToken::class.java.isAssignableFrom(authentication)

    override fun authenticate(authentication: Authentication): Authentication {
        val attempt = authentication as EmailCodeAuthenticationToken
        val now = clock.instant()

        val wellFormed = attempt.login.isNotEmpty() && attempt.code.isNotEmpty()
        val accepted = wellFormed && codes.tryCode(attempt.login, hashOf(attempt.code), now)
        if (!accepted) throw BadCredentialsException("Wrong code")

        val account = accounts.findByLogin(attempt.login)
            ?: accounts.register(attempt.login, attempt.locale)
            ?: throw BadCredentialsException("Account refused")

        val user = account.toUserDetails()
        val factor = FactorGrantedAuthority.withAuthority(FactorGrantedAuthority.OTT_AUTHORITY).issuedAt(now).build()
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities + factor)
    }
}
