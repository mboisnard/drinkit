package com.drinkit.security.infra.login

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer
import org.springframework.security.web.authentication.AuthenticationFailureHandler

internal const val LOGIN_PATH = "/api/auth/login"

/**
 * Plugs [JsonLoginFilter] into the chain the way Spring Security plugs its form login, so that the filter gets the
 * chain's authentication manager, session strategy, remember-me services and security context repository. A filter
 * added by hand would keep its defaults, which never save the login in the session. It answers a successful login
 * with a 204, and a failed one through the failure handler it is given.
 *
 * OWASP: ASVS 5.0 V7.2.4, Session Management Cheat Sheet (session id renewal at login).
 */
internal class JsonLoginConfigurer(filter: JsonLoginFilter, failureHandler: AuthenticationFailureHandler) :
    AbstractAuthenticationFilterConfigurer<HttpSecurity, JsonLoginConfigurer, JsonLoginFilter>(filter, null) {

    init {
        successHandler { _, response, _ -> response.status = HttpStatus.NO_CONTENT.value() }
        failureHandler(failureHandler)
    }

    override fun init(http: HttpSecurity) {
        loginProcessingUrl(LOGIN_PATH)
        super.init(http)
    }

    override fun createLoginProcessingUrlMatcher(loginProcessingUrl: String) =
        requestMatcherBuilder.matcher(HttpMethod.POST, loginProcessingUrl)
}
