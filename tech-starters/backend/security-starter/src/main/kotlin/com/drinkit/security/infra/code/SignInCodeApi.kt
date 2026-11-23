package com.drinkit.security.infra.code

import com.drinkit.security.CurrentVisitor
import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.events.SecurityEventLogger
import com.drinkit.security.infra.hashOf
import com.drinkit.security.infra.login.SignIns
import com.drinkit.security.infra.login.toUserDetails
import com.drinkit.security.infra.rememberme.requestRememberMe
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.FactorGrantedAuthority
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.web.ErrorResponseException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.json.JsonMapper
import java.time.Clock

/**
 * Every refused sign-in is the same 401: a wrong code, an unknown login and a refused account alike.
 *
 * OWASP: ASVS 5.0 V6.3.8, V6.6, Authentication Cheat Sheet (generic failures, out-of-band codes).
 */
@Suppress("LongParameterList")
internal class SignInCodeApi(
    private val issuer: SignInCodeIssuer,
    private val codes: SignInCodes,
    private val accounts: UserAccounts,
    private val signIns: SignIns,
    private val rememberMe: RememberMeServices?,
    private val limits: SignInCodeLimits,
    private val visitor: CurrentVisitor,
    private val events: SecurityEventLogger,
    private val clock: Clock,
    private val jsonMapper: JsonMapper,
    private val maxEmailLength: Int,
) {

    // The IP address counts first, and a limited email keeps its live code, which a new one would replace
    fun requestCode(request: ServerRequest): ServerResponse {
        val servletRequest = request.servletRequest()

        limits.requestFrom(visitor.ipAddress()).orThrow()
        val login = servletRequest.readSignInBody(jsonMapper)?.login(maxEmailLength)
            ?: throw ErrorResponseException(HttpStatus.BAD_REQUEST)
        limits.requestFor(login).orThrow()

        issuer.issue(login)

        return ServerResponse.noContent().build()
    }

    // Limited before the code is checked, so that a limited try costs its email none of its tries
    fun signIn(request: ServerRequest): ServerResponse {
        val servletRequest = request.servletRequest()
        val response = requireNotNull(
            (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).response,
        )

        limits.tryFrom(visitor.ipAddress()).orThrow()
        val body = servletRequest.readSignInBody(jsonMapper)
        servletRequest.requestRememberMe(body?.path("rememberMe")?.booleanValue(false) == true)

        val login = body?.login(maxEmailLength).orEmpty()
        val code = body?.code().orEmpty()
        val now = clock.instant()
        val accepted = login.isNotEmpty() && code.isNotEmpty() && codes.tryCode(login, hashOf(code), now)
        val account = if (accepted) accounts.findByLogin(login) ?: accounts.register(login) else null

        if (account == null) {
            events.loginFailed()
            rememberMe?.loginFail(servletRequest, response)
            throw ErrorResponseException(HttpStatus.UNAUTHORIZED)
        }

        val user = account.toUserDetails()
        val factor = FactorGrantedAuthority.withAuthority(FactorGrantedAuthority.OTT_AUTHORITY).issuedAt(now).build()
        val authentication = signIns.signIn(
            UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities + factor),
            "email_code",
        )
        rememberMe?.loginSuccess(servletRequest, response, authentication)

        return ServerResponse.noContent().build()
    }
}
