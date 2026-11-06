package com.drinkit.security.infra.code

import com.drinkit.security.infra.rememberme.requestRememberMe
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
internal class EmailCodeSignInFilter(private val jsonMapper: JsonMapper, private val maxEmailLength: Int) :
    UsernamePasswordAuthenticationFilter() {

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
