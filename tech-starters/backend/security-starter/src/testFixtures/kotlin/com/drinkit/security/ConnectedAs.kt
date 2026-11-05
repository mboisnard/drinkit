package com.drinkit.security

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.support.WebTestUtils
import org.springframework.test.web.servlet.request.RequestPostProcessor

/** Sends a MockMvc request as [user], with the XSRF cookie and header a browser of the application would send. */
fun connectedAs(user: ConnectedUser): RequestPostProcessor {
    val principal = User.withUsername(user.id)
        .password("")
        .authorities(user.roles.map(::SimpleGrantedAuthority))
        .build()
    val token = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities)
    return RequestPostProcessor { request -> authentication(token).postProcessRequest(withXsrfToken(request)) }
}

// Spring's csrf() swaps the token repository of the shared filter chain for a session one, for every later test
@Suppress("SpreadOperator")
private fun withXsrfToken(request: MockHttpServletRequest): MockHttpServletRequest {
    val repository = requireNotNull(WebTestUtils.getCsrfTokenRepository(request)) { "No CSRF filter" }
    val token = repository.generateToken(request)
    val response = MockHttpServletResponse()
    repository.saveToken(token, request, response)
    request.setCookies(*request.cookies.orEmpty(), *response.cookies)
    request.addHeader(token.headerName, token.token)
    return request
}
