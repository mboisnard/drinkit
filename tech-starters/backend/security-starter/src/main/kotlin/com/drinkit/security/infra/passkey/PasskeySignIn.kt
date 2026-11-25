package com.drinkit.security.infra.passkey

import com.drinkit.security.infra.events.SecurityEventLogger
import com.drinkit.security.infra.login.SignIns
import com.drinkit.security.infra.login.UserAccountsUserDetailsService
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.ObjectPostProcessor
import org.springframework.security.config.annotation.web.AuthorizeHttpRequestsDsl
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.access.intercept.AuthorizationFilter
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler
import org.springframework.security.web.authentication.HttpMessageConverterAuthenticationSuccessHandler
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter

// Spring's filters answer the options and the sign-in before these rules: theirs only say who may call them
internal fun AuthorizeHttpRequestsDsl.passkeyRules() {
    authorize(HttpMethod.POST, "/webauthn/register/options", authenticated)
    authorize(HttpMethod.POST, "/webauthn/register", authenticated)
    authorize(HttpMethod.DELETE, "/webauthn/register/*", authenticated)
    authorize(HttpMethod.POST, "/webauthn/authenticate/options", permitAll)
    authorize(HttpMethod.POST, "/login/webauthn", permitAll)
}

/**
 * Spring Security's passkeys in the security chain, at their standard URLs. A passkey sign-in goes the way of the
 * others: a new session id and XSRF token, then [SignIns.signedIn].
 *
 * Spring Security, Passkeys. W3C WebAuthn Level 3, 7.2 (assertion). OWASP: ASVS 5.0 V7.2.4, 6.3.3.
 */
internal class PasskeySignIn(
    private val users: UserAccountsUserDetailsService,
    private val signIns: SignIns,
    private val sessionStrategy: SessionAuthenticationStrategy,
    private val contextRepository: SecurityContextRepository,
    private val events: SecurityEventLogger,
) {

    fun configure(http: HttpSecurity) {
        // The name of a user entity is the id of its account. Shared by this chain only, so that no other sign-in
        // finds a UserDetailsService bean that loads by id
        http.setSharedObject(UserDetailsService::class.java, UserDetailsService(users::loadUserById))
        // Before the authorization, since Spring's registration filter comes right after it
        http.addFilterBefore(RefusedPasskeyFilter(), AuthorizationFilter::class.java)

        http.webAuthn { passkeys ->
            passkeys.disableDefaultRegistrationPage(true)
            passkeys.withObjectPostProcessor(
                object : ObjectPostProcessor<WebAuthnAuthenticationFilter> {
                    override fun <O : WebAuthnAuthenticationFilter> postProcess(filter: O): O =
                        filter.also(::signInAsOthers)
                },
            )
        }
    }

    private fun signInAsOthers(filter: WebAuthnAuthenticationFilter) {
        val succeeded = HttpMessageConverterAuthenticationSuccessHandler()
        val failed = AuthenticationEntryPointFailureHandler(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))

        filter.setSessionAuthenticationStrategy(sessionStrategy)
        filter.setSecurityContextRepository(contextRepository)
        filter.setAuthenticationSuccessHandler { request, response, authentication ->
            signIns.signedIn(authentication.name, "passkey")
            succeeded.onAuthenticationSuccess(request, response, authentication)
        }
        filter.setAuthenticationFailureHandler { request, response, exception ->
            events.loginFailed()
            failed.onAuthenticationFailure(request, response, exception)
        }
    }
}
