package com.drinkit.security.infra.passkey

import com.webauthn4j.util.exception.WebAuthnException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Answers 400 to a passkey refused at creation, as Spring's registration filter answers an unreadable one, where that
 * filter lets the refusal out as a 500. Nothing is saved by then.
 *
 * W3C WebAuthn Level 3, 7.1 (registering a new credential).
 */
internal class RefusedPasskeyFilter : OncePerRequestFilter() {

    private val registration = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/webauthn/register")

    override fun shouldNotFilter(request: HttpServletRequest) = !registration.matches(request)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        try {
            chain.doFilter(request, response)
        } catch (_: WebAuthnException) {
            response.status = HttpStatus.BAD_REQUEST.value()
        } catch (_: IllegalArgumentException) {
            // Spring's answer to a passkey whose id is already saved, or to a request it cannot read
            response.status = HttpStatus.BAD_REQUEST.value()
        }
    }
}
