package com.drinkit.security.infra.code

import com.drinkit.rate.limit.RateLimitDecision
import com.drinkit.security.CurrentVisitor
import com.drinkit.security.infra.SecurityErrorResponses
import com.drinkit.security.infra.rememberme.requestRememberMe
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tools.jackson.databind.json.JsonMapper

/** Extends the form login filter to take its place in the chain, which `HttpSecurity.addFilter` finds by class. */
internal class EmailCodeSignInFilter(
    private val jsonMapper: JsonMapper,
    private val maxEmailLength: Int,
    private val limits: SignInCodeLimits,
    private val visitor: CurrentVisitor,
    private val errorResponses: SecurityErrorResponses,
) : UsernamePasswordAuthenticationFilter() {

    // Before the sign-in, whose every refusal is a 401, and before the code is checked, so that a limited try costs
    // its email none of its tries
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        if (requiresAuthentication(httpRequest, httpResponse)) {
            val fromIp = limits.tryFrom(visitor.ipAddress())
            if (fromIp is RateLimitDecision.Refused) {
                errorResponses.tooManyRequests(httpRequest, httpResponse, fromIp)
                return
            }
        }

        super.doFilter(request, response, chain)
    }

    override fun attemptAuthentication(request: HttpServletRequest, response: HttpServletResponse): Authentication {
        val body = request.readSignInBody(jsonMapper)
        request.requestRememberMe(body?.path("rememberMe")?.booleanValue(false) == true)

        // A malformed sign-in goes to the provider too, which publishes the failure that the security log records
        val login = body?.login(maxEmailLength).orEmpty()
        val code = body?.code().orEmpty()
        return authenticationManager.authenticate(EmailCodeAuthenticationToken(login, code, request.locale))
    }
}

/**
 * A filter added by hand would keep its defaults, which never save the sign-in in the session.
 *
 * OWASP: ASVS 5.0 V7.2.4, Session Management Cheat Sheet (session id renewal at sign-in).
 */
internal class EmailCodeSignInConfigurer(filter: EmailCodeSignInFilter, failureHandler: AuthenticationFailureHandler) :
    AbstractAuthenticationFilterConfigurer<HttpSecurity, EmailCodeSignInConfigurer, EmailCodeSignInFilter>(
        filter,
        null,
    ) {

    init {
        successHandler { _, response, _ -> response.status = HttpStatus.NO_CONTENT.value() }
        failureHandler(failureHandler)
    }

    override fun init(http: HttpSecurity) {
        loginProcessingUrl(CODE_SIGN_IN_PATH)
        super.init(http)
    }

    override fun createLoginProcessingUrlMatcher(loginProcessingUrl: String) =
        requestMatcherBuilder.matcher(HttpMethod.POST, loginProcessingUrl)
}
