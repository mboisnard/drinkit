package com.drinkit.security.infra.chain

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.web.filter.OncePerRequestFilter

private val SAFE_METHODS = setOf("GET", "HEAD", "OPTIONS", "TRACE")
private val SAME_SITE_VALUES = setOf("same-origin", "none")

/**
 * Refuses a state-changing request that the browser marks as coming from another site, in its `Sec-Fetch-Site`
 * header, even when it carries a valid CSRF token. A second line of defense against cross-site request forgery, in
 * case a token leaks. A request without the header, from an older browser or a non-browser client, goes on to the
 * CSRF check.
 *
 * OWASP: ASVS 5.0 V3.5.1, CSRF Prevention Cheat Sheet (Fetch Metadata headers).
 */
internal class FetchMetadataFilter(private val refusal: AccessDeniedHandler) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val site = request.getHeader("Sec-Fetch-Site")

        if (request.method in SAFE_METHODS || site == null || site in SAME_SITE_VALUES) {
            chain.doFilter(request, response)
        } else {
            refusal.handle(request, response, AccessDeniedException("Cross-site request"))
        }
    }
}
