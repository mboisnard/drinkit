package com.drinkit.observability.infra

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

/**
 * Hands Spring MVC an exception that a servlet filter lets escape, while the trace of the request is still open, so
 * that it answers the problem of any other error rather than the page of Spring Boot's `/error`, which has no traceId.
 */
internal class UnresolvedExceptionFilter(private val resolver: HandlerExceptionResolver) : OncePerRequestFilter() {

    @Suppress("TooGenericExceptionCaught")
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        try {
            chain.doFilter(request, response)
        } catch (ex: Exception) {
            if (response.isCommitted || resolver.resolveException(request, response, null, ex) == null) throw ex
        }
    }
}
